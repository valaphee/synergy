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

package com.valaphee.synergy.bgs

import bgs.protocol.MethodOptionsProto
import bgs.protocol.NO_RESPONSE
import bgs.protocol.NoData
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Service
import com.google.protobuf.kotlin.get
import com.valaphee.synergy.events
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kotlinx.coroutines.channels.sendBlocking

/**
 * @author Kevin Ludwig
 */
class LoggingHandler(
    private val services: Map<Int, Service>
) : ChannelDuplexHandler() {
    internal val responses = mutableMapOf<Int, Pair<Service, MethodDescriptor>>()

    override fun channelRead(context: ChannelHandlerContext, message: Any?) {
        if (message is Packet) log(message)
        context.fireChannelRead(message)
    }

    override fun write(context: ChannelHandlerContext, message: Any?, promise: ChannelPromise?) {
        if (message is Packet) log(message)
        context.write(message, promise)
    }

    private fun log(packet: Packet) {
        events.sendBlocking(when (packet.header.serviceId) {
            PacketCodec.requestServiceId -> services[packet.header.serviceHash]?.let { service ->
                service.descriptorForType.methods.find { it.options[MethodOptionsProto.methodOptions].id == packet.header.methodId }?.let { methodDescriptor ->
                    val response = service.getResponsePrototype(methodDescriptor)
                    if (response !is NO_RESPONSE && response !is NoData) responses[packet.header.token] = service to methodDescriptor
                    BgsLogEvent(packet.header.token, packet.header.serviceHash, service.descriptorForType.name, packet.header.methodId, methodDescriptor.name, packet.payload)
                } ?: BgsLogEvent(packet.header.token, packet.header.serviceHash, service.descriptorForType.name, packet.header.methodId, null, packet.payload)
            } ?: BgsLogEvent(packet.header.token, packet.header.serviceHash, null, packet.header.methodId, null, packet.payload)
            PacketCodec.responseServiceId -> responses.remove(packet.header.token)?.let { (service, methodDescriptor) -> BgsLogEvent(packet.header.token, packet.header.serviceHash, service.descriptorForType.name, packet.header.methodId, methodDescriptor.name, packet.payload) } ?: BgsLogEvent(packet.header.token, 0, null, 0, null, packet.payload)
            else -> TODO()
        })
    }
}
