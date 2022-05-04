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

import com.valaphee.netcode.mcbe.network.Packet
import com.valaphee.synergy.messages
import com.valaphee.synergy.proxy.Connection
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kotlinx.coroutines.runBlocking

/**
 * @author Kevin Ludwig
 */
class EventEmitter(
    private val connection: Connection
) : ChannelDuplexHandler() {
    override fun channelRead(context: ChannelHandlerContext, message: Any?) {
        if (message is Packet) emit(message)
        context.fireChannelRead(message)
    }

    override fun write(context: ChannelHandlerContext, message: Any?, promise: ChannelPromise?) {
        if (message is Packet) emit(message)
        context.write(message, promise)
    }

    private fun emit(packet: Packet) {
        runBlocking { messages.emit(McbeMessage(connection.id, System.currentTimeMillis(), packet.toString())) }
    }
}
