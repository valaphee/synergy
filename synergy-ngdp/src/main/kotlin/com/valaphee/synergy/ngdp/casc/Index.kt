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

import com.valaphee.synergy.casc.com.valaphee.synergy.ngdp.util.hashLookup3
import io.netty.buffer.Unpooled
import org.apache.commons.vfs2.FileObject
import java.math.BigInteger

/**
 * @author Kevin Ludwig
 */
class Index(
    path: FileObject,
    versions: Map<Int, Int>
) {
    private val entries = mutableMapOf<BigInteger, Reference>()

    init {
        versions.forEach { (bucket, version) ->
            val indexFile = path.getChild(String.format("%02x%08x.idx", bucket, version))
            val buffer = Unpooled.wrappedBuffer(indexFile.content.byteArray)
            val headerSize = buffer.readIntLE()
            check(buffer.readIntLE() == buffer.hashLookup3(length = headerSize).first)
            buffer.readUnsignedShortLE()
            check(bucket == buffer.readUnsignedShortLE())
            val lengthSize = buffer.readUnsignedByte().toInt()
            val locationSize = buffer.readUnsignedByte().toInt()
            val keySize = buffer.readUnsignedByte().toInt()
            val segmentBits = buffer.readUnsignedByte().toInt()
            repeat((headerSize - 8) / 8) {
                val dataBegin = buffer.readIntLE()
                val dataEnd = buffer.readIntLE()
            }
            buffer.skipBytes(16 - ((8 + headerSize) % 16))
            val entriesSize = buffer.readIntLE()
            val entriesHash = buffer.readIntLE()
            /*var entryHash = 0 to 0*/
            repeat(entriesSize / (keySize + locationSize + lengthSize)) {
                /*entryHash = buffer.hashLookup3(length = keySize + locationSize + lengthSize, init = entryHash)*/
                val reference = Reference(buffer, keySize, locationSize, lengthSize, segmentBits)
                entries[reference.key] = reference
            }
            /*check(entryHash.first == entriesHash)*/
        }
    }

    operator fun get(key: BigInteger) = entries[key]
}
