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

package com.valaphee.synergy.pro

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.handler.logging.LoggingHandler
import java.math.BigInteger

/**
 * @author Kevin Ludwig
 */
class FrontendHandler(
    private val proxy: ProProxy
) : ChannelInboundHandlerAdapter() {
    private var outboundChannel: Channel? = null

    private var state: State? = null

    private var srpI = 0L
    private lateinit var srpv: BigInteger
    private lateinit var srpA: BigInteger
    private lateinit var srpb: BigInteger
    private lateinit var srpB: BigInteger

    override fun channelActive(context: ChannelHandlerContext) {
        outboundChannel = Bootstrap()
            .group(context.channel().eventLoop())
            .channel(context.channel()::class.java)
            .handler(object : ChannelInitializer<Channel>() {
                override fun initChannel(channel: Channel) {
                    channel.pipeline().addLast(LoggingHandler(), BackendHandler(context.channel()))
                }
            })
            .option(ChannelOption.AUTO_READ, false)
            .localAddress(proxy.`interface`, 0)
            .remoteAddress(proxy.host, proxy.port)
            .connect().addListener(object : ChannelFutureListener {
                override fun operationComplete(future: ChannelFuture) {
                    if (future.isSuccess) context.channel().read()
                    else context.channel().close()
                }
            }).channel()
        state = State.Handshake
    }

    override fun channelInactive(context: ChannelHandlerContext) {
        outboundChannel?.let { if (it.isActive) it.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE) }
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        val `in` = (message as ByteBuf).retainedDuplicate()
        if (outboundChannel!!.isActive) outboundChannel!!.writeAndFlush(message).addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (future.isSuccess) context.channel().read()
                else future.channel().close()
            }
        })

        try {
            when (state) {
                State.Handshake -> if (StringBuilder(16).apply {
                        var value = `in`.readByte().toInt()
                        while (value != 0) {
                            append(value.toChar())
                            value = `in`.readByte().toInt()
                        }
                    }.toString() == clientMagic) {
                    /*val out = context.alloc().buffer(serverMagic.toByteArray().size + 1)
                    out.writeBytes(serverMagic.toByteArray())
                    out.writeByte(0)
                    context.writeAndFlush(out)*/
                    state = State.LoginSrp1
                }
                State.LoginSrp1 -> {
                    srpI = `in`.readLongLE()
                    srpA = BigInteger(ByteArray(`in`.readableBytes()).apply { `in`.readBytes(this) })

                    val s = ProProxy.srpRoutines.generateRandomSalt(32, ProProxy.random)
                    val x = ProProxy.srpRoutines.computeX(ProProxy.sha256Local.get(), s, proxy.k0)
                    srpv = ProProxy.srpRoutines.computeVerifier(ProProxy.srpN, ProProxy.srpg, x)
                    srpb = ProProxy.srpRoutines.generatePrivateValue(ProProxy.srpN, ProProxy.random)
                    srpB = ProProxy.srpRoutines.computePublicServerValue(ProProxy.srpN, ProProxy.srpg, ProProxy.srpk, srpv, srpb)

                    /*val out = context.alloc().buffer()
                    out.writeBytes(s)
                    out.writeByte(ProProxy.srpg.byteValueExact().toInt())
                    out.writeBytes(srpB.toByteArray())
                    context.writeAndFlush(out)*/
                    state = State.LoginSrp2
                }
                State.LoginSrp2 -> {
                    `in`.readLongLE()
                    val M1_c = BigInteger(ByteArray(`in`.readableBytes()).apply { `in`.readBytes(this) })
                    if (ProProxy.srpRoutines.isValidPublicValue(ProProxy.srpN, M1_c)) {
                        val u_c = ProProxy.srpRoutines.computeU(ProProxy.sha256Local.get(), ProProxy.srpN, srpA, srpB)
                        val S_c = ProProxy.srpRoutines.computeSessionKey(ProProxy.srpN, srpv, u_c, srpA, srpb)
                        if (ProProxy.srpRoutines.computeClientEvidence(ProProxy.sha256Local.get(), srpA, srpB, S_c).equals(M1_c)) {
                        }
                    }
                    val x_s = ProProxy.srpRoutines.computeX(ProProxy.sha256Local.get(), proxy.k1, proxy.k2)
                    val u_s = ProProxy.srpRoutines.computeU(ProProxy.sha256Local.get(), ProProxy.srpN, srpA, srpB)
                    val S_s = ProProxy.srpRoutines.computeSessionKey(ProProxy.srpN, ProProxy.srpg, ProProxy.srpk, x_s, u_s, srpb, srpA)
                    val M1_s = ProProxy.srpRoutines.computeClientEvidence(ProProxy.sha256Local.get(), srpA, srpB, S_s)

                    /*val out = context.alloc().buffer()
                    out.writeBytes(M1_s.toByteArray())
                    out.writeBytes(ByteArray(292))
                    context.writeAndFlush(out)*/
                    state = State.Encryption
                }
                State.Encryption -> Unit
            }
        } finally {
            `in`.release()
        }
    }

    override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        /*if (context.channel().isActive) context.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)*/
    }

    private enum class State {
        Handshake, LoginSrp1, LoginSrp2, Encryption
    }

    companion object {
        private const val clientMagic = "HELLO PRO CLIENT"
        private const val serverMagic = "HELLO PRO SERVER"
    }
}
