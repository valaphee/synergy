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

import bgs.protocol.RpcTypes
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame

/**
 * @author Kevin Ludwig
 */
class BnetCodec : MessageToMessageCodec<BinaryWebSocketFrame, BnetPacket>() {
    override fun encode(context: ChannelHandlerContext, message: BnetPacket, out: MutableList<Any>) {
        val header = message.header
        val headerSize = header.serializedSize
        val payload = message.payload
        val payloadSize = payload.size

        val buffer = context.alloc().buffer(2 + headerSize + payloadSize)
        buffer.writeShort(headerSize)
        buffer.writeBytes(header.toByteArray())
        buffer.writeBytes(payload)
        out.add(BinaryWebSocketFrame(buffer))
    }

    override fun decode(context: ChannelHandlerContext, `in`: BinaryWebSocketFrame, out: MutableList<Any>) {
        val buffer = `in`.content()

        val headerSize = buffer.readUnsignedShort()
        if (!buffer.isReadable(headerSize)) return
        val header = RpcTypes.Header.parseFrom(ByteArray(headerSize).apply { buffer.readBytes(this) })

        val payloadSize = if (header.hasSize()) header.size else buffer.readableBytes()
        if (!buffer.isReadable(payloadSize)) return
        val payload = ByteArray(payloadSize).apply { buffer.readBytes(this) }

        out.add(BnetPacket(header, payload))
    }
}
