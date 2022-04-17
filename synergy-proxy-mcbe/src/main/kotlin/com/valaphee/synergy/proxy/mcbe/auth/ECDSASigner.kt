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

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.http.content.OutputStreamContent
import io.ktor.util.AttributeKey
import io.ktor.util.KtorDsl
import io.ktor.util.encodeBase64
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.core.readBytes
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.PooledByteBufAllocator
import java.security.PrivateKey
import java.security.Signature

/**
 * @author Kevin Ludwig
 */
class ECDSASigner(
    val privateKey: PrivateKey
) {
    class Config {
        lateinit var privateKey: PrivateKey

        fun build() = ECDSASigner(privateKey)
    }

    @KtorDsl
    companion object Plugin : HttpClientPlugin<Config, ECDSASigner> {
        override val key = AttributeKey<ECDSASigner>("ECDSASigner")

        override fun prepare(block: Config.() -> Unit) = Config().apply(block).build()

        override fun install(plugin: ECDSASigner, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Render) { payload ->
                if (payload is OutputStreamContent) {
                    val timestamp = (System.currentTimeMillis() + 11644473600L) * 10000L
                    val signer = Signature.getInstance("SHA256withECDSA").apply { initSign(plugin.privateKey) }
                    val signData = PooledByteBufAllocator.DEFAULT.directBuffer()
                    try {
                        signData.writeInt(1)
                        signData.writeZero(1)
                        signData.writeLong(timestamp)
                        signData.writeZero(1)
                        signData.writeBytes(context.method.value.toByteArray())
                        signData.writeZero(1)
                        signData.writeBytes(context.url.buildString().toByteArray())
                        signData.writeZero(1)
                        context.headers["Authorization"]?.let { signData.writeBytes(it.toByteArray()) }
                        signData.writeZero(1)
                        val bodyChannel = ByteChannel()
                        payload.writeTo(bodyChannel)
                        signData.writeBytes(bodyChannel.readRemaining().readBytes())
                        bodyChannel.close()
                        signData.writeZero(1)
                        signer.update(signData.nioBuffer())
                        signData.clear()
                        signData.writeInt(1)
                        signData.writeLong(timestamp)
                        signData.writeBytes(signer.sign())
                        context.header("Signature", ByteBufUtil.getBytes(signData).encodeBase64())
                    } finally {
                        signData.release()
                    }
                }
                proceedWith(payload)
            }
        }
    }
}
