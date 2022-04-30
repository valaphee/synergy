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

import com.valaphee.synergy.ngdp.casc.util.hashLookup3
import com.valaphee.synergy.ngdp.util.Key
import com.valaphee.synergy.ngdp.util.asHexStringToByteArray
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import java.io.File

/**
 * @author Kevin Ludwig
 */
class Index(
    private val shadowMemory: ShadowMemory
) {
    private val _entries = mutableMapOf<Key, Reference>()
    val entries: Map<Key, Reference> get() = _entries

    init {
        shadowMemory.versions.forEach { (bucket, version) ->
            val index = Unpooled.wrappedBuffer(File(shadowMemory.path, String.format("%02x%08x.idx", bucket, version)).readBytes())
            val headerSize = index.readIntLE()
            check(index.readIntLE() == index.hashLookup3(length = headerSize).first)
            check(index.readUnsignedShortLE() == Version)
            check(index.readUnsignedShortLE() == bucket)
            val lengthSize = index.readUnsignedByte().toInt()
            val locationSize = index.readUnsignedByte().toInt()
            val keySize = index.readUnsignedByte().toInt()
            val segmentBits = index.readUnsignedByte().toInt()
            check(index.readLongLE() == 0x4000000000)
            index.skipBytes(16 - ((8 + headerSize) % 16))
            val entriesSize = index.readIntLE()
            /*check(*/index.readIntLE()/* == index.hashLookup3(length = entriesSize).first) TODO*/
            repeat(entriesSize / (keySize + locationSize + lengthSize)) {
                val reference = Reference(index, keySize, locationSize, lengthSize, segmentBits)
                val referenceBucket = reference.key.toBucket()
                if (referenceBucket != bucket) check((referenceBucket + 1) and 0xF == bucket)
                _entries[reference.key] = reference
            }
        }
    }

    operator fun get(key: Key) = entries[key]

    fun write() {
        val entries = entries.values.groupBy { if (it.key.isCrossLink()) it.key.asCrossLinkToBucket() else it.key.toBucket() }
        shadowMemory.versions.forEach { (bucket, version) ->
            val index = Unpooled.buffer()
            val headerSizeIndex = index.writerIndex()
            index.writeIntLE(0)
            val headerHashIndex = index.writerIndex()
            index.writeIntLE(0)
            val headerOffset = index.writerIndex()
            index.writeShortLE(Version)
            index.writeShortLE(bucket)
            index.writeByte(Reference.LengthSize)
            index.writeByte(Reference.LocationSize)
            index.writeByte(Reference.KeySize)
            index.writeByte(Reference.SegmentBits)
            val headerSize = index.writerIndex() - headerSizeIndex
            index.writeLongLE(0x4000000000)
            index.writeZero(8)
            index.setIntLE(headerSizeIndex, headerSize)
            index.setIntLE(headerHashIndex, index.hashLookup3(headerOffset, headerSize).first)
            val entriesSizeIndex = index.writerIndex()
            index.writeIntLE(0)
            val entriesHashIndex = index.writerIndex()
            index.writeIntLE(0)
            val entriesOffset = index.writerIndex()
            entries[bucket]?.forEach { index.writeBytes(it.toBuffer()) }
            val entriesSize = index.writerIndex() - entriesOffset
            index.setIntLE(entriesSizeIndex, entriesSize)
            index.setIntLE(entriesHashIndex, index.hashLookup3(entriesOffset, entriesSize).first) // TODO
            val pad = index.writerIndex() % 65536
            if (pad != 0) index.writeZero(65536 - pad)
            File(shadowMemory.path, String.format("%02x%08x.idx", bucket, version)).writeBytes(ByteBufUtil.getBytes(index))
        }
    }

    companion object {
        const val Version = 7
        val CrossLinkKeySuffix = "99bd34280ef31a".asHexStringToByteArray()

        fun Key.toBucket(): Int {
            val value = bytes.fold(0) { value, byte -> value xor (byte.toInt() and 0xFF) }
            return value and 0xF xor (value shr 4)
        }

        fun Key.isCrossLink() = bytes.copyOfRange(2, bytes.size).contentEquals(CrossLinkKeySuffix)

        fun Key.asCrossLinkToBucket() = (toBucket() + 1) and 0xF
    }
}
