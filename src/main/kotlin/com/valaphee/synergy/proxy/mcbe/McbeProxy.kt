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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.valaphee.jackson.dataformat.nbt.NbtFactory
import com.valaphee.jackson.dataformat.nbt.util.DeepEqualsLinkedHashMap
import com.valaphee.jackson.dataformat.nbt.util.EmbeddedObjectDeserializationProblemHandler
import com.valaphee.netcode.mcbe.latestProtocolVersion
import com.valaphee.netcode.mcbe.latestVersion
import com.valaphee.netcode.mcbe.network.Compressor
import com.valaphee.netcode.mcbe.network.Decompressor
import com.valaphee.netcode.mcbe.network.PacketBuffer
import com.valaphee.netcode.mcbe.network.PacketCodec
import com.valaphee.netcode.mcbe.network.Pong
import com.valaphee.netcode.mcbe.world.GameMode
import com.valaphee.netcode.mcbe.world.block.Block
import com.valaphee.netcode.mcbe.world.block.BlockState
import com.valaphee.synergy.proxy.RouterProxy
import com.valaphee.synergy.proxy.bossGroup
import com.valaphee.synergy.proxy.underlyingNetworking
import com.valaphee.synergy.proxy.workerGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFactory
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.DatagramPacket
import io.netty.util.ReferenceCountUtil
import network.ycc.raknet.RakNet
import network.ycc.raknet.packet.UnconnectedPing
import network.ycc.raknet.packet.UnconnectedPong
import network.ycc.raknet.pipeline.UserDataCodec
import network.ycc.raknet.server.channel.RakNetServerChannel
import network.ycc.raknet.server.pipeline.UdpPacketHandler
import java.net.InetSocketAddress
import java.util.zip.GZIPInputStream

/**
 * @author Kevin Ludwig
 */
@JsonTypeName("mcbe")
class McbeProxy(
    id: String,
    host: String,
    port: Int = 19132,
    `interface`: String
) : RouterProxy<Unit>(id, host, port, `interface`) {
    override val type get() = "mcbe"

    @JsonIgnore private var channel: Channel? = null

    override suspend fun start() {
        require(channel == null)

        super.start()

        channel = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channelFactory(ChannelFactory { RakNetServerChannel(underlyingNetworking.datagramChannel) })
            .handler(object : ChannelInitializer<Channel>() {
                override fun initChannel(channel: Channel) {
                    channel.pipeline().addLast(object : UdpPacketHandler<UnconnectedPing>(UnconnectedPing::class.java) {
                        override fun handle(context: ChannelHandlerContext, address: InetSocketAddress, unconnectedPing: UnconnectedPing) {
                            val rakNetConfig = context.channel().config() as RakNet.Config
                            val unconnectedPong = UnconnectedPong(unconnectedPing.clientTime, rakNetConfig.serverId, rakNetConfig.magic, Pong(rakNetConfig.serverId, "Synergy", latestVersion, latestProtocolVersion, "MCPE", false, GameMode.Survival, 0, 1, port, port, "Synergy").toString())
                            val buffer = context.alloc().directBuffer(unconnectedPong.sizeHint())
                            try {
                                rakNetConfig.codec.encode(unconnectedPong, buffer)
                                repeat(3) { context.writeAndFlush(DatagramPacket(buffer.retainedSlice(), address)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) }
                            } finally {
                                ReferenceCountUtil.safeRelease(unconnectedPong)
                                buffer.release()
                            }
                        }
                    })
                }
            })
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(channel: Channel) {
                    channel.pipeline().addLast(UserDataCodec.NAME, userDataCodec)
                    channel.pipeline().addLast(Compressor.NAME, Compressor(7))
                    channel.pipeline().addLast(Decompressor.NAME, Decompressor())
                    channel.pipeline().addLast(PacketCodec.NAME,  PacketCodec({ PacketBuffer(it, jsonObjectMapper, nbtLeObjectMapper, nbtLeVarIntObjectMapper, nbtLeVarIntNoWrapObjectMapper) }, false))
                    channel.pipeline().addLast(FrontendHandler(this@McbeProxy))
                }
            })
            .localAddress(host, port)
            .bind().channel()
    }

    override suspend fun stop() {
        channel?.let {
            it.close()
            channel = null

            super.stop()
        }
    }

    companion object {
        internal val userDataCodec = UserDataCodec(0xFE)
        internal val jsonObjectMapper = ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY).enable(JsonParser.Feature.ALLOW_COMMENTS)
        internal val nbtObjectMapper = ObjectMapper(NbtFactory())
        internal val nbtLeObjectMapper = ObjectMapper(NbtFactory().enable(NbtFactory.Feature.LittleEndian))
        internal val nbtLeVarIntObjectMapper = ObjectMapper(NbtFactory().enable(NbtFactory.Feature.LittleEndian).enable(NbtFactory.Feature.VarInt))
        internal val nbtLeVarIntNoWrapObjectMapper = ObjectMapper(NbtFactory().enable(NbtFactory.Feature.LittleEndian).enable(NbtFactory.Feature.VarInt).enable(NbtFactory.Feature.NoWrap))
        internal val blocks: Map<String, Block>

        init {
            listOf(jsonObjectMapper, nbtObjectMapper, nbtLeObjectMapper, nbtLeVarIntObjectMapper, nbtLeVarIntNoWrapObjectMapper).forEach { it.registerKotlinModule().registerModule(SimpleModule().addAbstractTypeMapping(Map::class.java, DeepEqualsLinkedHashMap::class.java)).addHandler(EmbeddedObjectDeserializationProblemHandler).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) }

            val blockStates = mutableMapOf<String, MutableList<Map<String, Any>>>()
            nbtObjectMapper.readValue<BlockPalette>(GZIPInputStream(McbeProxy::class.java.getResource("/block_palette.nbt")!!.openStream())).blockStates.forEach { blockStates.getOrPut(it.blockKey, ::mutableListOf).add(it.states) }
            blocks = blockStates.mapValues { Block(it.key, it.value) }
        }

        private data class BlockPalette(
            @get:JsonProperty("blocks") val blockStates: List<BlockState>
        )
    }
}
