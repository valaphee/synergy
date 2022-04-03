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

package com.valaphee.synergy.bnet

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

/**
 * @author Kevin Ludwig
 */
class BnetProxyFrontendHandler(
    private val proxy: BnetProxy
) : ChannelInboundHandlerAdapter() {
    private var outboundChannel: Channel? = null

    override fun channelActive(ctx: ChannelHandlerContext) {
        outboundChannel = Bootstrap()
            .group(ctx.channel().eventLoop())
            .channel(ctx.channel()::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(channel: SocketChannel) {
                    channel.pipeline().addLast(
                        SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build().newHandler(channel.alloc()),
                        HttpClientCodec(),
                        HttpObjectAggregator(UShort.MAX_VALUE.toInt()),
                        BnetProxyBackendHandler(proxy, ctx.channel())
                    )
                }
            })
            .localAddress(proxy.interfaceHost, proxy.interfacePort)
            .remoteAddress(proxy.host, proxy.port)
            .connect().addListener(object : ChannelFutureListener {
                override fun operationComplete(future: ChannelFuture) {
                    if (!future.isSuccess) ctx.channel().close()
                }
            }).channel()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        outboundChannel?.let { if (it.isActive) it.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE) }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (outboundChannel!!.isActive) outboundChannel!!.writeAndFlush(msg).addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (!future.isSuccess) future.channel().close()
            }
        })
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        if (ctx.channel().isActive) ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }
}
