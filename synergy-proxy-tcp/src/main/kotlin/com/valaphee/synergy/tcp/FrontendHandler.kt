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

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelOption

/**
 * @author Kevin Ludwig
 */
class FrontendHandler(
    private val proxy: TcpProxy
) : ChannelInboundHandlerAdapter() {
    private var outboundChannel: Channel? = null

    override fun channelActive(context: ChannelHandlerContext) {
        outboundChannel = Bootstrap()
            .group(context.channel().eventLoop())
            .channel(context.channel()::class.java)
            .handler(BackendHandler(context.channel()))
            .option(ChannelOption.AUTO_READ, false)
            .localAddress(proxy.`interface`, 0)
            .remoteAddress(proxy.host, proxy.port)
            .connect().addListener(object : ChannelFutureListener {
                override fun operationComplete(future: ChannelFuture) {
                    if (future.isSuccess) context.channel().read()
                    else context.channel().close()
                }
            }).channel()
    }

    override fun channelInactive(context: ChannelHandlerContext) {
        outboundChannel?.let { if (it.isActive) it.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE) }
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        if (outboundChannel!!.isActive) outboundChannel!!.writeAndFlush(message).addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (future.isSuccess) context.channel().read()
                else future.channel().close()
            }
        })
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        if (context.channel().isActive) context.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }
}
