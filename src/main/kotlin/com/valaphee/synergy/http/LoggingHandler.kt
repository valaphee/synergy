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

package com.valaphee.synergy.http

import com.valaphee.synergy.events
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpMessage
import kotlinx.coroutines.channels.sendBlocking

/**
 * @author Kevin Ludwig
 */
class LoggingHandler : ChannelDuplexHandler() {
    override fun channelRead(context: ChannelHandlerContext, message: Any?) {
        if (message is HttpMessage) log(message)
        context.fireChannelRead(message)
    }

    override fun write(context: ChannelHandlerContext, message: Any?, promise: ChannelPromise?) {
        if (message is HttpMessage) log(message)
        context.write(message, promise)
    }

    private fun log(message: HttpMessage) {
        events.sendBlocking(when (message) {
            is FullHttpRequest -> HttpRequestLogEvent(message.method().name(), message.uri())
            is FullHttpResponse -> HttpResponseLogEvent("${message.status().code()} ${message.status().reasonPhrase()}")
            else -> TODO()
        })
    }
}
