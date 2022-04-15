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

package com.valaphee.synergy.proxy.http

import com.valaphee.synergy.event.events
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpMessage
import kotlinx.coroutines.runBlocking

/**
 * @author Kevin Ludwig
 */
class EventPump(
    private val proxy: HttpProxy
) : ChannelDuplexHandler() {
    override fun channelRead(context: ChannelHandlerContext, message: Any?) {
        if (message is HttpMessage) emit(message)
        context.fireChannelRead(message)
    }

    override fun write(context: ChannelHandlerContext, message: Any?, promise: ChannelPromise?) {
        if (message is HttpMessage) emit(message)
        context.write(message, promise)
    }

    private fun emit(message: HttpMessage) {
        runBlocking {
            events.emit(
                when (message) {
                    is FullHttpRequest -> HttpRequestEvent(proxy.id, System.currentTimeMillis(), message.method().name(), message.uri(), message.headers().associate { it.key to it.value })
                    is FullHttpResponse -> HttpResponseEvent(proxy.id, System.currentTimeMillis(), message.status().code(), message.status().reasonPhrase(), message.headers().associate { it.key to it.value })
                    else -> TODO()
                }
            )
        }
    }
}
