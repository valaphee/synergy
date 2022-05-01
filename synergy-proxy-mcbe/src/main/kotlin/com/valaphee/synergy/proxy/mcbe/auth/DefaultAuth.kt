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
import com.valaphee.synergy.proxy.mcbe.service.DeviceAuth
import com.valaphee.synergy.proxy.mcbe.service.GoSignature
import com.valaphee.synergy.proxy.mcbe.service.GoSlice
import com.valaphee.synergy.proxy.mcbe.service.OAuth20Connect
import com.valaphee.synergy.proxy.mcbe.service.OAuth20Token
import com.valaphee.synergy.proxy.mcbe.service.Signature
import com.valaphee.synergy.proxy.mcbe.service.UserAuth
import com.valaphee.synergy.proxy.mcbe.service.toByteArray
import com.valaphee.synergy.proxy.mcbe.service.toGoSlice
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.util.encodeBase64
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jose4j.jws.JsonWebSignature
import java.awt.Desktop
import java.net.URI
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Base64
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
class DefaultAuth(
    private val keyPair: KeyPair
) : Auth {
    lateinit var version: String
    private var _authJws: String? = null
    override val authJws: String
        get() = _authJws ?: run {
            val authRootKey = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V"
            val authJwsChain = runBlocking {
                val deviceKeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
                val x = GoSlice()
                val y = GoSlice()
                GoSignature.Instance.GetPublicKeyXY(deviceKeyPair.private.encoded.toGoSlice(), x, y)
                val httpClient = HttpClient.config { install(Signature) { keyPair = deviceKeyPair } }
                val deviceAuth = httpClient.post("https://device.auth.xboxlive.com/device/authenticate") {
                    headers { header("X-Xbl-Contract-Version", "1") }
                    contentType(ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "RelyingParty" to "http://auth.xboxlive.com",
                            "TokenType" to "JWT",
                            "Properties" to mapOf(
                                "AuthMethod" to "ProofOfPossession",
                                "Id" to "{${UUID.randomUUID()}}",
                                "DeviceType" to "Android",
                                "Version" to "10",
                                "ProofKey" to mapOf(
                                    "crv" to "P-256",
                                    "alg" to "ES256",
                                    "use" to "sig",
                                    "kty" to "EC",
                                    "x" to Base64.getUrlEncoder().withoutPadding().encodeToString(x.toByteArray()),
                                    "y" to Base64.getUrlEncoder().withoutPadding().encodeToString(y.toByteArray())
                                )
                            )
                        )
                    )
                }.body<DeviceAuth>()
                val oauth20Connect = HttpClient.submitForm("https://login.live.com/oauth20_connect.srf", Parameters.build {
                    append("client_id", "0000000048183522")
                    append("scope", "service::user.auth.xboxlive.com::MBI_SSL")
                    append("response_type", "device_code")
                }).body<OAuth20Connect>()
                Desktop.getDesktop().browse(URI(oauth20Connect.verificationUri))
                println(oauth20Connect.userCode)
                delay(15000)
                val oauth20Token = HttpClient.submitForm("https://login.live.com/oauth20_token.srf", Parameters.build {
                    append("client_id", "0000000048183522")
                    append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    append("device_code", oauth20Connect.deviceCode)
                }).body<OAuth20Token>()
                val userAuth = httpClient.post("https://sisu.xboxlive.com/authorize") {
                    headers { header("X-Xbl-Contract-Version", "1") }
                    contentType(ContentType.Application.Json)
                    setBody(mapOf(
                        "AccessToken" to "t=${oauth20Token.accessToken}",
                        "AppId" to "0000000048183522",
                        "deviceToken" to deviceAuth.token,
                        "Sandbox" to "RETAIL",
                        "UseModernGamertag" to true,
                        "SiteName" to "user.auth.xboxlive.com",
                        "RelyingParty" to "https://multiplayer.minecraft.net/",
                        "ProofKey" to mapOf(
                            "crv" to "P-256",
                            "alg" to "ES256",
                            "use" to "sig",
                            "kty" to "EC",
                            "x" to Base64.getUrlEncoder().withoutPadding().encodeToString(x.toByteArray()),
                            "y" to Base64.getUrlEncoder().withoutPadding().encodeToString(y.toByteArray())
                        )
                    ))
                }.body<UserAuth>()
                HttpClient.post("https://multiplayer.minecraft.net/authentication") {
                    headers {
                        header("User-Agent", "MCPE/Android")
                        header("Client-Version", version)
                        header("Authorization", "XBL3.0 x=${userAuth.authorizationToken.claim.userInfo[0].userHash};${userAuth.authorizationToken.token}")
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
