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

import com.fasterxml.jackson.module.kotlin.readValue
import com.valaphee.netcode.mcbe.latestProtocolVersion
import com.valaphee.netcode.mcbe.network.EncryptionInitializer
import com.valaphee.netcode.mcbe.network.PacketBuffer
import com.valaphee.netcode.mcbe.network.PacketCodec
import com.valaphee.netcode.mcbe.network.packet.LoginPacket
import com.valaphee.netcode.mcbe.network.packet.ServerToClientHandshakePacket
import com.valaphee.netcode.mcbe.network.packet.WorldPacket
import com.valaphee.netcode.mcbe.util.Registries
import com.valaphee.netcode.mcbe.util.Registry
import com.valaphee.netcode.mcbe.world.entity.player.User
import com.valaphee.synergy.ObjectMapper
import com.valaphee.synergy.proxy.mcbe.auth.DefaultAuth
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * @author Kevin Ludwig
 */
class BackendHandler(
    private val inboundChannel: Channel
) : ChannelDuplexHandler() {
    private var version = latestProtocolVersion
    private val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp384r1")) }.generateKeyPair()
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

                val authJwsChain = (ObjectMapper.readValue<Map<*, *>>(message.authJws)["chain"] as List<*>).associateBy { KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(JwtConsumerBuilder().setJwsAlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, "ES384").setSkipSignatureVerification().build().process(it as String).joseObjects.single().headers.getStringHeaderValue("x5u")))) }
                var authJwsKey: PublicKey? = null
                authJwsChain[rootKey]?.let {
                    var authJws: Pair<PublicKey, String>? = rootKey to it as String
                    val authJwsKeys = mutableSetOf(authJws!!.first)
                    while (authJws != null) {
                        val _authJws = JwtConsumerBuilder().setJwsAlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, "ES384").setVerificationKey(authJws.first).build().processToClaims(authJws.second)
                        val authJwsPayload = ObjectMapper.readValue<Map<*, *>>(_authJws.rawJson)
                        val _authJwsKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode((authJwsPayload["identityPublicKey"] as String))))
                        if (_authJwsKey != rootKey) authJwsKey = _authJwsKey
                        authJws = if (authJwsKeys.add(_authJwsKey)) authJwsChain[_authJwsKey]?.let { _authJwsKey to it as String } else null
                    }
                }
                clientPublicKey = authJwsKey!!
                LoginPacket(
                    message.protocolVersion, auth.authJws, JsonWebSignature().apply {
                        setHeader("alg", "ES384")
                        setHeader("x5u", Base64.getEncoder().encodeToString(keyPair.public.encoded))
                        payload = McbeProxy.jsonObjectMapper.writeValueAsString(ObjectMapper.readValue<User>(JwtConsumerBuilder().setJwsAlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, "ES384").setVerificationKey(authJwsKey!!).build().processToClaims(message.userJws).rawJson).copy(operatingSystem = User.OperatingSystem.Android))
                        key = keyPair.private
                    }.compactSerialization
                )
            }
            else -> message
        }, promise)
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        if (message is ServerToClientHandshakePacket) {
            val jws = JwtConsumerBuilder().setJwsAlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, "ES384").setSkipSignatureVerification().build().process(message.jws)
            val clientSalt = ByteArray(16).apply(random::nextBytes)
            inboundChannel.write(ServerToClientHandshakePacket(JsonWebSignature().apply {
                setHeader("alg", "ES384")
                setHeader("x5u", Base64.getEncoder().encodeToString(keyPair.public.encoded))
                setHeader("typ", "JWT")
                payload = ObjectMapper.writeValueAsString(mapOf("salt" to Base64.getEncoder().encodeToString(clientSalt)))
                key = keyPair.private
            }.compactSerialization))
            inboundChannel.pipeline().addLast(EncryptionInitializer(keyPair, clientPublicKey, true, clientSalt))
            context.pipeline().addLast(EncryptionInitializer(keyPair, KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(jws.joseObjects.single().headers.getStringHeaderValue("x5u")))), true, Base64.getDecoder().decode(ObjectMapper.readValue<Map<*, *>>(jws.jwtClaims.rawJson)["salt"] as String)))
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

    companion object {
        private val rootKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode("MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V")))
        private val random = SecureRandom()
    }
}
