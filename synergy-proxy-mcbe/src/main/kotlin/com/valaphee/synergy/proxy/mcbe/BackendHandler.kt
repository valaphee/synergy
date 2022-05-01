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

package com.valaphee.synergy.proxy.mcbe

import com.valaphee.netcode.mcbe.latestProtocolVersion
import com.valaphee.netcode.mcbe.network.EncryptionInitializer
import com.valaphee.netcode.mcbe.network.PacketBuffer
import com.valaphee.netcode.mcbe.network.PacketCodec
import com.valaphee.netcode.mcbe.network.packet.LoginPacket
import com.valaphee.netcode.mcbe.network.packet.ServerToClientHandshakePacket
import com.valaphee.netcode.mcbe.network.packet.WorldPacket
import com.valaphee.netcode.mcbe.util.Registries
import com.valaphee.netcode.mcbe.util.Registry
import com.valaphee.netcode.mcbe.util.generateKeyPair
import com.valaphee.netcode.mcbe.util.parseAuthJws
import com.valaphee.netcode.mcbe.util.parseServerToClientHandshakeJws
import com.valaphee.netcode.mcbe.util.parseUserJws
import com.valaphee.netcode.mcbe.world.entity.player.User
import com.valaphee.synergy.proxy.mcbe.auth.DefaultAuth
import io.ktor.util.encodeBase64
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import org.jose4j.jws.JsonWebSignature
import java.security.PublicKey

/**
 * @author Kevin Ludwig
 */
class BackendHandler(
    private val inboundChannel: Channel
) : ChannelDuplexHandler() {
    private var version = latestProtocolVersion
    private val keyPair = generateKeyPair()
    private lateinit var clientPublicKey: PublicKey
    private val auth = DefaultAuth(keyPair).apply { version = "1.18.30" }

    override fun channelInactive(context: ChannelHandlerContext) {
        if (inboundChannel.isActive) inboundChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    override fun write(context: ChannelHandlerContext, message: Any, promise: ChannelPromise) {
        super.write(context, when (message) {
            is LoginPacket -> {
                version = message.protocolVersion
                inboundChannel.pipeline()[PacketCodec::class.java].version = version
                context.pipeline()[PacketCodec::class.java].version = version

                val (_, verificationKey, _) = parseAuthJws(message.authJws)
                clientPublicKey = verificationKey
                val (_, user) = parseUserJws(message.userJws, verificationKey)
                LoginPacket(
                    message.protocolVersion, auth.authJws, JsonWebSignature().apply {
                        setHeader("alg", "ES384")
                        setHeader("x5u", keyPair.public.encoded.encodeBase64())
                        payload = McbeProxy.jsonObjectMapper.writeValueAsString(user.copy(operatingSystem = User.OperatingSystem.Android))
                        key = keyPair.private
                    }.compactSerialization
                )
            }
            else -> message
        }, promise)
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        if (message is ServerToClientHandshakePacket) {
            val (serverPublicKey, salt) = parseServerToClientHandshakeJws(message.jws)
            val encryptionInitializer = EncryptionInitializer(keyPair, clientPublicKey, true, salt)
            inboundChannel.write(encryptionInitializer.serverToClientHandshakePacket)
            inboundChannel.pipeline().addLast(encryptionInitializer)
            context.pipeline().addLast(EncryptionInitializer(keyPair, serverPublicKey, true, salt))
        } else inboundChannel.write(when (message) {
            is WorldPacket -> {
                val registries = Registries(Registry(), Registry())
                var runtimeId = 0
                (McbeProxy.blocks.values + message.blocks!!).sortedWith(if (version >= 486) compareBy { it.description.key.lowercase() } else compareBy { it.description.key.split(":", limit = 2)[1].lowercase() }).forEach { it.states.forEach { registries.blockStates[runtimeId++] = it } }
                message.items.forEach { registries.items[it.key] = it.value.key }
                inboundChannel.pipeline()[PacketCodec::class.java].wrapBuffer = { PacketBuffer(it, McbeProxy.jsonObjectMapper, McbeProxy.nbtLeObjectMapper, McbeProxy.nbtLeVarIntObjectMapper, McbeProxy.nbtLeVarIntNoWrapObjectMapper, registries) }
                context.pipeline()[PacketCodec::class.java].wrapBuffer = { PacketBuffer(it, McbeProxy.jsonObjectMapper, McbeProxy.nbtLeObjectMapper, McbeProxy.nbtLeVarIntObjectMapper, McbeProxy.nbtLeVarIntNoWrapObjectMapper, registries) }
                message
            }
            else -> message
        }).addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (!future.isSuccess) future.channel().close()
            }
        })
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        /*if (context.channel().isActive) context.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)*/
    }
}
