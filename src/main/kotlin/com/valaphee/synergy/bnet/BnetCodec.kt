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

import bgs.protocol.Header
import bgs.protocol.MethodOptionsProto
import bgs.protocol.NO_RESPONSE
import bgs.protocol.NoData
import com.google.protobuf.Message
import com.google.protobuf.Service
import com.google.protobuf.kotlin.get
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame

/**
 * @author Kevin Ludwig
 */
class BnetCodec(
    internal val services: Map<Int, Service>
) : MessageToMessageCodec<BinaryWebSocketFrame, BnetPacket>() {
    internal val responses = mutableMapOf<Int, Message>()

    override fun encode(context: ChannelHandlerContext, message: BnetPacket, out: MutableList<Any>) {
        val header = message.header
        val headerSize = header.serializedSize
        val payload = message.payload
        if (payload is ByteArray) {
            val payloadSize = payload.size

            val buffer = context.alloc().buffer(2 + headerSize + payloadSize)
            buffer.writeShort(headerSize)
            buffer.writeBytes(header.toByteArray())
            buffer.writeBytes(payload)
            out.add(BinaryWebSocketFrame(buffer))
        } else if (payload is Message) {
            val payloadSize = payload.serializedSize

            val buffer = context.alloc().buffer(2 + headerSize + payloadSize)
            buffer.writeShort(headerSize)
            buffer.writeBytes(header.toByteArray())
            buffer.writeBytes(payload.toByteArray())
            out.add(BinaryWebSocketFrame(buffer))

            if (header.serviceId == 0) services[header.serviceHash]?.let { service ->
                service.descriptorForType.methods.find { it.options[MethodOptionsProto.methodOptions].id == header.methodId }?.let { methodDescriptor ->
                    val response = service.getResponsePrototype(methodDescriptor)
                    if (response !is NO_RESPONSE && response !is NoData) responses[header.token] = response
                }
            }
        }
    }

    override fun decode(context: ChannelHandlerContext, `in`: BinaryWebSocketFrame, out: MutableList<Any>) {
        val buffer = `in`.content()

        if (!buffer.isReadable(2)) return
        val headerSize = buffer.readUnsignedShort()
        if (!buffer.isReadable(headerSize)) return
        val header = Header.parseFrom(ByteArray(headerSize).apply { buffer.readBytes(this) })

        val payloadSize = if (header.hasSize()) header.size else buffer.readableBytes()
        if (!buffer.isReadable(payloadSize)) return
        val payload = ByteArray(payloadSize).apply { buffer.readBytes(this) }

        out.add(BnetPacket(header, when (header.serviceId) {
            0 -> services[header.serviceHash]?.let { service -> service.descriptorForType.methods.find { it.options[MethodOptionsProto.methodOptions].id == header.methodId }?.let { methodDescriptor -> service.getRequestPrototype(methodDescriptor).parserForType.parseFrom(payload) } }
            254 -> responses.remove(header.token)?.parserForType?.parseFrom(payload)
            else -> null
        } ?: payload))
    }
}
