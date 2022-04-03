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

package com.valaphee.synergy.mcbe

import com.valaphee.netcode.mcbe.network.Compressor
import com.valaphee.netcode.mcbe.network.Decompressor
import com.valaphee.netcode.mcbe.network.PacketBuffer
import com.valaphee.netcode.mcbe.network.PacketCodec
import com.valaphee.synergy.underlyingNetworking
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFactory
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import network.ycc.raknet.RakNet
import network.ycc.raknet.client.channel.RakNetClientChannel
import network.ycc.raknet.pipeline.UserDataCodec

/**
 * @author Kevin Ludwig
 */
class McbeProxyFrontendHandler(
    private val proxy: McbeProxy
) : ChannelInboundHandlerAdapter() {
    private var outboundChannel: Channel? = null

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.channel().config().isAutoRead = false
        outboundChannel = Bootstrap()
            .group(ctx.channel().eventLoop())
            .channelFactory(ChannelFactory { RakNetClientChannel(underlyingNetworking.datagramChannel) })
            .handler(object : ChannelInitializer<Channel>() {
                override fun initChannel(channel: Channel) {
                    channel.pipeline().addLast(UserDataCodec.NAME, McbeProxy.userDataCodec)
                    channel.pipeline().addLast(Compressor.NAME, Compressor(7))
                    channel.pipeline().addLast(Decompressor.NAME, Decompressor())
                    channel.pipeline().addLast(PacketCodec.NAME,  PacketCodec({ PacketBuffer(it, McbeProxy.jsonObjectMapper, McbeProxy.nbtLeObjectMapper, McbeProxy.nbtLeVarIntObjectMapper, McbeProxy.nbtLeVarIntNoWrapObjectMapper) }, true))
                    channel.pipeline().addLast(McbeProxyBackendHandler(proxy, ctx.channel()))
                }
            })
            .option(RakNet.MTU, 1_464)
            .option(RakNet.PROTOCOL_VERSION, 10)
            .localAddress(proxy.interfaceHost, proxy.interfacePort)
            .remoteAddress(proxy.host, proxy.port)
            .connect().addListener(object : ChannelFutureListener {
                override fun operationComplete(future: ChannelFuture) {
                    if (future.isSuccess) ctx.channel().config().isAutoRead = true
                    else ctx.channel().close()
                }
            }).channel()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        outboundChannel?.let { if (it.isActive) it.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE) }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (outboundChannel!!.isActive) outboundChannel!!.write(msg).addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (!future.isSuccess) future.channel().close()
            }
        })
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        /*if (ctx.channel().isActive) ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)*/
    }
}
