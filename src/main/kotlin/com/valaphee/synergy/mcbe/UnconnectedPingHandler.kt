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

import com.valaphee.netcode.mcbe.latestProtocolVersion
import com.valaphee.netcode.mcbe.latestVersion
import com.valaphee.netcode.mcbe.network.Pong
import com.valaphee.netcode.mcbe.world.GameMode
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.DatagramPacket
import io.netty.util.ReferenceCountUtil
import network.ycc.raknet.RakNet
import network.ycc.raknet.packet.UnconnectedPing
import network.ycc.raknet.packet.UnconnectedPong
import network.ycc.raknet.server.pipeline.UdpPacketHandler
import java.net.InetSocketAddress

/**
 * @author Kevin Ludwig
 */
object UnconnectedPingHandler : UdpPacketHandler<UnconnectedPing>(UnconnectedPing::class.java) {
    override fun handle(context: ChannelHandlerContext, address: InetSocketAddress, unconnectedPing: UnconnectedPing) {
        val rakNetConfig = context.channel().config() as RakNet.Config
        val unconnectedPong = UnconnectedPong(unconnectedPing.clientTime, rakNetConfig.serverId, rakNetConfig.magic, Pong(rakNetConfig.serverId, "Synergy", latestVersion, latestProtocolVersion, "MCPE", false, GameMode.Survival, 0, 1, 19132, 19133, "Synergy").toString())
        val buffer = context.alloc().directBuffer(unconnectedPong.sizeHint())
        try {
            rakNetConfig.codec.encode(unconnectedPong, buffer)
            repeat(3) { context.writeAndFlush(DatagramPacket(buffer.retainedSlice(), address)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) }
        } finally {
            ReferenceCountUtil.safeRelease(unconnectedPong)
            buffer.release()
        }
    }
}
