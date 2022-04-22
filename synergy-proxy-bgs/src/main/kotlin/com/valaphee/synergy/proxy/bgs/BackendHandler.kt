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

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import java.net.URI

/**
 * @author Kevin Ludwig
 */
class BackendHandler(
    private val proxy: BgsProxy,
    private val inboundChannel: Channel
) : ChannelInboundHandlerAdapter() {
    private val handshaker = WebSocketClientHandshakerFactory.newHandshaker(URI("wss://${proxy.host}/"), WebSocketVersion.V13, "v1.rpc.battle.net", false, DefaultHttpHeaders())
    private lateinit var handshakeFuture: ChannelPromise

    override fun handlerAdded(context: ChannelHandlerContext) {
        handshakeFuture = context.newPromise()
    }

    override fun channelActive(context: ChannelHandlerContext) {
        handshaker.handshake(context.channel())
    }

    override fun channelInactive(context: ChannelHandlerContext) {
        if (inboundChannel.isActive) inboundChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        if (handshaker.isHandshakeComplete) inboundChannel.writeAndFlush(message).addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (future.isSuccess) context.channel().read()
                else context.channel().close()
            }
        }) else try {
            handshaker.finishHandshake(context.channel(), message as FullHttpResponse)
            handshakeFuture.setSuccess()
            context.channel().read()
        } catch (ex: WebSocketHandshakeException) {
            handshakeFuture.setFailure(ex)
            context.channel().close()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        if (!handshakeFuture.isDone) handshakeFuture.setFailure(cause)
        if (context.channel().isActive) context.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }
}
