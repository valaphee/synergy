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

package com.valaphee.synergy.bnet

import bgs.protocol.MethodOptionsProto
import bgs.protocol.NO_RESPONSE
import bgs.protocol.NoData
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Service
import com.google.protobuf.kotlin.get
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Kevin Ludwig
 */
class BnetLoggingHandler : ChannelDuplexHandler() {
    private lateinit var codec: BnetCodec
    internal val responses = mutableMapOf<Int, Pair<Service, MethodDescriptor>>()

    override fun channelActive(ctx: ChannelHandlerContext) {
        codec = ctx.pipeline().get(BnetCodec::class.java)
        ctx.fireChannelActive()
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        if (message is BnetPacket) when (message.header.serviceId) {
            0 -> {
                codec.services[message.header.serviceHash]?.let { service ->
                    service.descriptorForType.methods.find { it.options[MethodOptionsProto.methodOptions].id == message.header.methodId }?.let { methodDescriptor ->
                        val response = service.getResponsePrototype(methodDescriptor)
                        if (response !is NO_RESPONSE && response !is NoData) responses[message.header.token] = service to methodDescriptor
                        serviceLoggers.getOrPut(service.descriptorForType.name) { LoggerFactory.getLogger(service.descriptorForType.name) }.debug("RPC #{}: {}\n{}", message.header.token, methodDescriptor.name, message.payload)
                    }
                }
            }
            254 -> {
                val (service, methodDescriptor) = checkNotNull(responses.remove(message.header.token))
                serviceLoggers.getOrPut(service.descriptorForType.name) { LoggerFactory.getLogger(service.descriptorForType.name) }.debug("RPC #{} returned {}\n{}", message.header.token, methodDescriptor.name, message.payload)
            }
        }
        context.fireChannelRead(message)
    }

    override fun write(context: ChannelHandlerContext, message: Any, promise: ChannelPromise?) {
        if (message is BnetPacket) when (message.header.serviceId) {
            0 -> {
                codec.services[message.header.serviceHash]?.let { service ->
                    service.descriptorForType.methods.find { it.options[MethodOptionsProto.methodOptions].id == message.header.methodId }?.let { methodDescriptor ->
                        val response = service.getResponsePrototype(methodDescriptor)
                        if (response !is NO_RESPONSE && response !is NoData) responses[message.header.token] = service to methodDescriptor
                        serviceLoggers.getOrPut(service.descriptorForType.name) { LoggerFactory.getLogger(service.descriptorForType.name) }.debug("RPC #{}: {}\n{}", message.header.token, methodDescriptor.name, message.payload)
                    }
                }
            }
            254 -> {
                val (service, methodDescriptor) = checkNotNull(responses.remove(message.header.token))
                serviceLoggers.getOrPut(service.descriptorForType.name) { LoggerFactory.getLogger(service.descriptorForType.name) }.debug("RPC #{} returned {}\n{}", message.header.token, methodDescriptor.name, message.payload)
            }
        }
        context.write(message, promise)
    }

    companion object {
        private val serviceLoggers = mutableMapOf<String, Logger>()
    }
}
