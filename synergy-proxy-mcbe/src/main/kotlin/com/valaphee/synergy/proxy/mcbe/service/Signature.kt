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

package com.valaphee.synergy.proxy.mcbe.service

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.http.content.OutputStreamContent
import io.ktor.http.encodedPath
import io.ktor.util.AttributeKey
import io.ktor.util.KtorDsl
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.toByteArray
import java.security.KeyPair
import java.util.Base64

/**
 * @author Kevin Ludwig
 */
class Signature(
    val keyPair: KeyPair,
) {
    class Config {
        lateinit var keyPair: KeyPair

        fun build() = Signature(keyPair)
    }

    @KtorDsl
    companion object Plugin : HttpClientPlugin<Config, Signature> {
        override val key = AttributeKey<Signature>("Signature")

        override fun prepare(block: Config.() -> Unit) = Config().apply(block).build()

        override fun install(plugin: Signature, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Render) { payload ->
                if (payload is OutputStreamContent) {
                    val bodyChannel = ByteChannel()
                    payload.writeTo(bodyChannel)
                    val signature = GoSlice().apply { GoSignature.Instance.GenerateSignature(plugin.keyPair.private.encoded.toGoSlice(), context.method.value.toByteArray().toGoSlice(), context.url.encodedPath.toByteArray().toGoSlice(), (context.headers["Authorization"] ?: "").toByteArray().toGoSlice(), bodyChannel.readRemaining().readBytes().toGoSlice(), this) }
                    bodyChannel.close()
                    context.header("Signature", Base64.getEncoder().encodeToString(signature.toByteArray()))
                }
                proceed()
            }
        }
    }
}



