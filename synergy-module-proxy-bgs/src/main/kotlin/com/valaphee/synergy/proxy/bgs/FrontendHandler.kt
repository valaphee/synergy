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

package com.valaphee.synergy.proxy.bgs

import com.valaphee.synergy.proxy.Connection
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
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import java.net.URI

/**
 * @author Kevin Ludwig
 */
class FrontendHandler(
    private val proxy: BgsProxy,
    private val connection: Connection,
) : ChannelInboundHandlerAdapter() {
    private var outboundChannel: Channel? = null

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

    override fun userEventTriggered(context: ChannelHandlerContext, event: Any) {
        if (event is WebSocketServerProtocolHandler.HandshakeComplete) {

            outboundChannel = Bootstrap()
                .group(context.channel().eventLoop())
                .channel(context.channel()::class.java)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(channel: SocketChannel) {
                        channel.pipeline().addLast(
                            proxy.clientSslContext.newHandler(channel.alloc()),
                            HttpClientCodec(),
                            HttpObjectAggregator(UShort.MAX_VALUE.toInt()),
                            WebSocketClientProtocolHandler(URI(event.requestUri()), WebSocketVersion.V13, event.selectedSubprotocol(), false, event.requestHeaders(), UShort.MAX_VALUE.toInt()),
                            PacketCodec(BgsProxy.Services),
                            EventEmitter(connection, BgsProxy.Services),
                            BackendHandler(context.channel())
                        )
                    }
                })
                .localAddress(connection.viaHost, connection.viaPort)
                .remoteAddress(connection.remoteHost, connection.remotePort)
                .connect().addListener(object : ChannelFutureListener {
                    override fun operationComplete(future: ChannelFuture) {
                        if (future.isSuccess) context.channel().read()
                        else context.channel().close()
                    }
                }).channel()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        if (context.channel().isActive) context.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }
}
