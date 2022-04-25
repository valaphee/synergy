/*
 * Copyright (c) 2022, Valaphee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.valaphee.synergy.proxy.mcbe.auth

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.valaphee.netcode.mcbe.util.generateKeyPair
import com.valaphee.netcode.mcbe.world.entity.player.AuthExtra
import com.valaphee.synergy.ObjectMapper
import io.ktor.util.encodeBase64
import org.jose4j.base64url.Base64Url
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwx.CompactSerializer
import java.security.KeyPair
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

/**
 * @author Kevin Ludwig
 */
class PsychicSignatureAuth(
    private val keyPair: KeyPair
) : Auth {
    lateinit var authExtra: AuthExtra
    private var _authJws: String? = null
    override val authJws: String
        get() = _authJws ?: run {
            val authRootKey = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V"
            val immediateKeyPair = generateKeyPair()
            val _authRootJws = JsonWebSignature().apply {
                setHeader("alg", "ES384")
                setHeader("x5u", authRootKey)
                val iat = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
                payload = ObjectMapper.writeValueAsString(
                    mapOf(
                        "nbf" to iat - 60,
                        "randomNonce" to Random.nextLong(),
                        "iss" to "Mojang",
                        "exp" to iat + 172800,
                        "iat" to iat,
                        "certificateAuthority" to true,
                        "identityPublicKey" to immediateKeyPair.public.encoded.encodeBase64()
                    )
                )
            }
            val authRootJws = CompactSerializer.serialize(_authRootJws.headers.encodedHeader, _authRootJws.encodedPayload, Base64Url.encode(ByteArray(96)))
            val authImmediateJws = JsonWebSignature().apply {
                setHeader("alg", "ES384")
                setHeader("x5u", immediateKeyPair.public.encoded.encodeBase64())
                val iat = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
                payload = ObjectMapper.writeValueAsString(
                    mapOf(
                        "nbf" to iat - 60,
                        "extraData" to authExtra,
                        "randomNonce" to Random.nextLong(),
                        "iss" to "Mojang",
                        "exp" to iat + 172800,
                        "iat" to iat,
                        "identityPublicKey" to keyPair.public.encoded.encodeBase64()
                    )
                )
                key = immediateKeyPair.private
            }.compactSerialization
            val authUserJws = JsonWebSignature().apply {
                setHeader("alg", "ES384")
                setHeader("x5u", keyPair.public.encoded.encodeBase64())
                val iat = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
                payload = jacksonObjectMapper().writeValueAsString(
                    mapOf(
                        "certificateAuthority" to true,
                        "exp" to iat + 172800,
                        "identityPublicKey" to authRootKey,
                        "nbf" to iat - 60
                    )
                )
                key = keyPair.private
            }.compactSerialization
            ObjectMapper.writeValueAsString(mapOf("chain" to listOf(authUserJws, authRootJws, authImmediateJws))).also { _authJws = it }
        }
}
