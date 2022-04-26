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

package com.valaphee.synergy.ngdp.casc

import com.valaphee.synergy.ngdp.util.Key
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

/**
 * @author Kevin Ludwig
 */
class Reference {
    val key: Key
    val file: Int
    val offset: Int
    val length: Int

    constructor(key: Key, file: Int, offset: Int, length: Int) {
        this.key = key
        this.file = file
        this.offset = offset
        this.length = length
    }

    constructor(buffer: ByteBuf, keySize: Int = KeySize, locationSize: Int = LocationSize, lengthSize: Int = LengthSize, segmentBits: Int = SegmentBits) {
        val offsetSize = (segmentBits + 7) / 8
        val fileSize = locationSize - offsetSize
        key = Key(ByteArray(keySize).apply { buffer.readBytes(this) })
        val file = buffer.readLE(fileSize)
        val offset = buffer.read(offsetSize)
        val extraBits = (offsetSize * 8) - segmentBits
        this.file = (file shl extraBits) or (offset ushr segmentBits)
        this.offset = offset and (UInt.MAX_VALUE.toLong() ushr extraBits).toInt()
        length = buffer.readLE(lengthSize)
    }

    fun toBuffer(keySize: Int = KeySize, locationSize: Int = LocationSize, lengthSize: Int = LengthSize, segmentBits: Int = SegmentBits): ByteBuf {
        val buffer = Unpooled.buffer(keySize + locationSize + lengthSize)
        val offsetSize = (segmentBits + 7) / 8
        val fileSize = locationSize - offsetSize
        buffer.writeBytes(key.bytes.copyOf(keySize))
        val extraBits = (offsetSize * 8) - segmentBits
        buffer.writeLE(file shr extraBits, fileSize)
        buffer.write(offset or ((file and (1 shl extraBits) - 1) shl segmentBits), offsetSize)
        buffer.writeLE(length, lengthSize)
        return buffer.asReadOnly()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Reference

        if (key != other.key) return false
        if (file != other.file) return false
        if (offset != other.offset) return false
        if (length != other.length) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + file
        result = 31 * result + offset
        result = 31 * result + length
        return result
    }

    companion object {
        const val KeySize = 9
        const val LocationSize = 5
        const val LengthSize = 4
        const val SegmentBits = 30

        private fun ByteBuf.read(size: Int): Int {
            var value = 0
            repeat(size) { value = value shl 8 or (readByte().toInt() and 0xFF) }
            return value
        }

        private fun ByteBuf.readLE(size: Int): Int {
            var value = 0
            repeat(size) { value = value or (readByte().toInt() and 0xFF shl (it * 8)) }
            return value
        }

        private fun ByteBuf.write(value: Int, size: Int) {
            repeat(size) { writeByte(value ushr ((size - it - 1) * 8) and 0xFF) }
        }

        private fun ByteBuf.writeLE(value: Int, size: Int) {
            repeat(size) { writeByte(value ushr (it * 8) and 0xFF) }
        }
    }
}
