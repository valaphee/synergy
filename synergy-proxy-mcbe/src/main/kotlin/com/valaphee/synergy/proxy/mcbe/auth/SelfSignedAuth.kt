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
import com.valaphee.netcode.mcbe.world.entity.player.AuthExtra
import com.valaphee.synergy.ObjectMapper
import io.ktor.util.encodeBase64
import org.jose4j.jws.JsonWebSignature
import java.security.KeyPair
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * @author Kevin Ludwig
 */
class SelfSignedAuth(
    private val keyPair: KeyPair
) : Auth {
    lateinit var authExtra: AuthExtra
    private var _authJws: String? = null
    override val authJws: String
        get() = _authJws ?: run {
            val authUserJws = JsonWebSignature().apply {
                setHeader("alg", "ES384")
                setHeader("x5u", keyPair.public.encoded.encodeBase64())
                val iat = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
                payload = jacksonObjectMapper().writeValueAsString(
                    mapOf(
                        "certificateAuthority" to true,
                        "exp" to iat + 172800,
                        "identityPublicKey" to keyPair.public.encoded.encodeBase64(),
                        "nbf" to iat - 60,
                        "extraData" to authExtra
                    )
                )
                key = keyPair.private
            }.compactSerialization
            ObjectMapper.writeValueAsString(mapOf("chain" to listOf(authUserJws))).also { _authJws = it }
        }
}
