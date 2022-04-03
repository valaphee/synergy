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

package com.valaphee.synergy.tcp

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

/**
 * @author Kevin Ludwig
 */
class TcpProxyBackendHandler(
    private val inboundChannel: Channel
) : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (inboundChannel.isActive) inboundChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        inboundChannel.writeAndFlush(msg).addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (future.isSuccess) ctx.channel().read()
                else future.channel().close()
            }
        })
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        if (ctx.channel().isActive) ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }
}
