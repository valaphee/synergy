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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.serialization.jackson.jackson
import io.ktor.util.encodeBase64
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kotlinx.coroutines.runBlocking
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import java.security.PublicKey
import java.util.Base64

/**
 * @author Kevin Ludwig
 */
class McbeProxyBackendHandler(
    private val mcbeProxy: McbeProxy,
    private val inboundChannel: Channel
) : ChannelDuplexHandler() {
    private val keyPair = generateKeyPair()
    private lateinit var clientPublicKey: PublicKey

    override fun channelActive(ctx: ChannelHandlerContext) {
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (inboundChannel.isActive) inboundChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        super.write(ctx, when (msg) {
            is LoginPacket -> {
                val (_, verificationKey, _) = parseAuthJws(msg.authJws)
                clientPublicKey = verificationKey
                val (_, user) = parseUserJws(msg.userJws, verificationKey)
                val authJwsChain = runBlocking {
                    httpClient.post("https://multiplayer.minecraft.net/authentication") {
                        headers {
                            header("Content-Type", "application/json")
                            header("User-Agent", "MCPE/Android")
                            header("Client-Version", user.version)
                            header("Authorization", mcbeProxy.authorization)
                        }
                        setBody(mapOf("identityPublicKey" to keyPair.public.encoded.encodeBase64()))
                    }.body<Map<*, *>>()["chain"] as List<*>
                }
                val authJwtContext = JwtConsumerBuilder().setJwsAlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, "ES384").apply { setSkipSignatureVerification() }.build().process(authJwsChain.first() as String)
                LoginPacket(
                    msg.protocolVersion, McbeProxy.jsonObjectMapper.writeValueAsString(
                        mapOf(
                            "chain" to listOf(JsonWebSignature().apply {
                                setHeader("alg", "ES384")
                                setHeader("x5u", Base64.getEncoder().encodeToString(keyPair.public.encoded))
                                val authJwsHeaders = authJwtContext.joseObjects.single().headers
                                payload = jacksonObjectMapper().writeValueAsString(mapOf("nbf" to authJwsHeaders.getStringHeaderValue("nbf"), "exp" to authJwsHeaders.getStringHeaderValue("exp"), "certificateAuthority" to true, "identityPublicKey" to authJwsHeaders.getStringHeaderValue("x5u")))
                                key = keyPair.private
                            }.compactSerialization) + authJwsChain
                        )
                    ), JsonWebSignature().apply {
                        setHeader("alg", "ES384")
                        setHeader("x5u", keyPair.public.encoded.encodeBase64())
                        payload = McbeProxy.jsonObjectMapper.writeValueAsString(user.copy(operatingSystem = User.OperatingSystem.Android))
                        key = keyPair.private
                    }.compactSerialization
                )
            }
            else -> msg
        }, promise)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is ServerToClientHandshakePacket) {
            val (serverPublicKey, salt) = parseServerToClientHandshakeJws(msg.jws)
            val encryptionInitializer = EncryptionInitializer(keyPair, clientPublicKey, true, salt)
            inboundChannel.write(encryptionInitializer.serverToClientHandshakePacket)
            inboundChannel.pipeline().addLast(encryptionInitializer)
            ctx.pipeline().addLast(EncryptionInitializer(keyPair, serverPublicKey, true, salt))
        } else inboundChannel.write(when (msg) {
            is WorldPacket -> {
                val registries = Registries(Registry(), Registry())
                var runtimeId = 0
                (McbeProxy.blocks.values + msg.blocks!!).sortedWith(if (486 >= 486) compareBy { it.description.key.lowercase() } else compareBy { it.description.key.split(":", limit = 2)[1].lowercase() }).forEach { it.states.forEach { registries.blockStates[runtimeId++] = it } }
                msg.items.forEach { registries.items[it.key] = it.value.key }
                inboundChannel.pipeline()[PacketCodec::class.java].wrapBuffer = { PacketBuffer(it, McbeProxy.jsonObjectMapper, McbeProxy.nbtLeObjectMapper, McbeProxy.nbtLeVarIntObjectMapper, McbeProxy.nbtLeVarIntNoWrapObjectMapper, registries) }
                ctx.pipeline()[PacketCodec::class.java].wrapBuffer = { PacketBuffer(it, McbeProxy.jsonObjectMapper, McbeProxy.nbtLeObjectMapper, McbeProxy.nbtLeVarIntObjectMapper, McbeProxy.nbtLeVarIntNoWrapObjectMapper, registries) }
                msg
            }
            else -> msg
        }).addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (!future.isSuccess) future.channel().close()
            }
        })
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        /*if (ctx.channel().isActive) ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)*/
    }

    companion object {
        private val httpClient = HttpClient(OkHttp) { install(ContentNegotiation) { jackson() } }
    }
}
