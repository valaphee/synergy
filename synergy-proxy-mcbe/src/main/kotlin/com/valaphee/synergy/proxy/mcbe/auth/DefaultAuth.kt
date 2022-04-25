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
import com.valaphee.synergy.HttpClient
import com.valaphee.synergy.ObjectMapper
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.encodeBase64
import kotlinx.coroutines.runBlocking
import org.jose4j.jws.JsonWebSignature
import java.security.KeyPair
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * @author Kevin Ludwig
 */
class DefaultAuth(
    private val keyPair: KeyPair
) : Auth {
    lateinit var version: String
    lateinit var authorization: String
    private var _authJws: String? = null
    override val authJws: String
        get() = _authJws ?: run {
            val authRootKey = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V"
            val authJwsChain = runBlocking {
                HttpClient.post("https://multiplayer.minecraft.net/authentication") {
                    headers {
                        header("User-Agent", "MCPE/Android")
                        header("Client-Version", version)
                        header("Authorization", authorization)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("identityPublicKey" to keyPair.public.encoded.encodeBase64()))
                }.body<Map<*, *>>()["chain"] as List<*>
            }
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
            ObjectMapper.writeValueAsString(mapOf("chain" to listOf(authUserJws) + authJwsChain)).also { _authJws = it }
        }
}
