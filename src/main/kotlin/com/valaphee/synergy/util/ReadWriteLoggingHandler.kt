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

package com.valaphee.synergy.util

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.logging.LoggingHandler
import java.net.SocketAddress

/**
 * @author Kevin Ludwig
 */
class ReadWriteLoggingHandler : LoggingHandler() {
    override fun channelRegistered(ctx: ChannelHandlerContext) {
        ctx.fireChannelRegistered()
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        ctx.fireChannelUnregistered()
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.fireChannelActive()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        ctx.fireChannelInactive()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        ctx.fireExceptionCaught(cause)
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any?) {
        ctx.fireUserEventTriggered(evt)
    }

    override fun bind(ctx: ChannelHandlerContext, localAddress: SocketAddress?, promise: ChannelPromise?) {
        ctx.bind(localAddress, promise)
    }

    override fun connect(ctx: ChannelHandlerContext, remoteAddress: SocketAddress?, localAddress: SocketAddress?, promise: ChannelPromise?) {
        ctx.connect(remoteAddress, localAddress, promise)
    }

    override fun disconnect(ctx: ChannelHandlerContext, promise: ChannelPromise?) {
        ctx.disconnect(promise)
    }

    override fun close(ctx: ChannelHandlerContext, promise: ChannelPromise?) {
        ctx.close(promise)
    }

    override fun deregister(ctx: ChannelHandlerContext, promise: ChannelPromise?) {
        ctx.deregister(promise)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.fireChannelReadComplete()
    }

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
        ctx.fireChannelWritabilityChanged()
    }

    override fun flush(ctx: ChannelHandlerContext) {
        ctx.flush()
    }
}
