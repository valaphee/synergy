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

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.math.BigInteger

/**
 * @author Kevin Ludwig
 */
class ProProxyFrontendHandler(
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
        /*outboundChannel = Bootstrap()
            .group(context.channel().eventLoop())
            .channel(context.channel()::class.java)
            .handler(ProProxyBackendHandler(context.channel()))
            .option(ChannelOption.AUTO_READ, false)
            .localAddress(proxy.`interface`, 0)
            .remoteAddress(proxy.host, proxy.port)
            .connect().addListener(object : ChannelFutureListener {
                override fun operationComplete(future: ChannelFuture) {
                    if (future.isSuccess) context.channel().read()
                    else context.channel().close()
                }
            }).channel()*/
        state = State.Handshake
    }

    override fun channelInactive(context: ChannelHandlerContext) {
        /*outboundChannel?.let { if (it.isActive) it.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE) }*/
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        /*if (outboundChannel!!.isActive) outboundChannel!!.writeAndFlush(message).addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (future.isSuccess) context.channel().read()
                else future.channel().close()
            }
        })*/
        val buffer = message as ByteBuf
        when (state) {
            State.Handshake -> if (StringBuilder().apply {
                var value = buffer.readByte().toInt()
                while (value != 0) {
                    append(value.toChar())
                    value = buffer.readByte().toInt()
                }
            }.toString() == clientMagic) {
                val responseBuffer = context.alloc().buffer(serverMagic.toByteArray().size + 1)
                responseBuffer.writeBytes(serverMagic.toByteArray())
                responseBuffer.writeByte(0)
                context.writeAndFlush(responseBuffer)
                state = State.LoginSrp1
            }
            State.LoginSrp1 -> {
                srpI = buffer.readLongLE()
                srpA = BigInteger(ByteArray(buffer.readableBytes()).apply { buffer.readBytes(this) })
                println("SRP1 ${java.lang.Long.toUnsignedString(srpI)} - ${srpA.toByteArray().size}")

                val s = ProProxy.srpRoutines.generateRandomSalt(32, ProProxy.random)
                val x = ProProxy.srpRoutines.computeX(ProProxy.sha256Local.get(), s, byteArrayOf(/*password*/))
                srpv = ProProxy.srpRoutines.computeVerifier(proxy.srpN, ProProxy.srpG, x)

                srpb = ProProxy.srpRoutines.generatePrivateValue(proxy.srpN, ProProxy.random)
                srpB = ProProxy.srpRoutines.computePublicServerValue(proxy.srpN, ProProxy.srpG, proxy.srpk, srpv, srpb)

                val responseBuffer = context.alloc().buffer()
                responseBuffer.writeBytes(s)
                responseBuffer.writeByte(ProProxy.srpG.byteValueExact().toInt())
                responseBuffer.writeBytes(srpB.toByteArray())
                context.writeAndFlush(responseBuffer)
                state = State.LoginSrp2
            }
            State.LoginSrp2 -> {
                val srpI = buffer.readLongLE()
                val M1 = BigInteger(ByteArray(buffer.readableBytes()).apply { buffer.readBytes(this) })
                if (ProProxy.srpRoutines.isValidPublicValue(proxy.srpN, M1)) {
                    val u = ProProxy.srpRoutines.computeU(ProProxy.sha256Local.get(), proxy.srpN, srpA, srpB)
                    val S = ProProxy.srpRoutines.computeSessionKey(proxy.srpN, srpv, u, srpA, srpb)
                    if (ProProxy.srpRoutines.computeClientEvidence(ProProxy.sha256Local.get(), srpA, srpB, S).equals(M1)) {
                        println("success")
                    } else {
                        println("where am I now?")
                    }
                } else {
                    println("invalid n")
                }
                println("SRP2 ${java.lang.Long.toUnsignedString(srpI)} - ${M1.toByteArray().size}")
            }
        }
    }

    override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        /*if (context.channel().isActive) context.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)*/
    }

    private enum class State {
        Handshake, LoginSrp1, LoginSrp2
    }

    companion object {
        private const val clientMagic = "HELLO PRO CLIENT"
        private const val serverMagic = "HELLO PRO SERVER"
    }
}
