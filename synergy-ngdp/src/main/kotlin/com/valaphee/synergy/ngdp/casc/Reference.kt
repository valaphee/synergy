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
import io.netty.buffer.ByteBuf

/**
 * @author Kevin Ludwig
 */
class Reference {
    val key: Int
    val file: Int
    val offset: Int
    val size: Int

    constructor(key: Int, file: Int, offset: Int, size: Int) {
        this.key = key
        this.file = file
        this.offset = offset
        this.size = size
    }

    constructor(buffer: ByteBuf, keySize: Int, locationSize: Int, lengthSize: Int, segmentBits: Int) {
        val offsetSize = (segmentBits + 7) / 8
        val fileSize = locationSize - offsetSize
        key = buffer.hashLookup3(length = keySize).first
        buffer.skipBytes(keySize)
        val file = buffer.readIntLE(fileSize)
        val offset = buffer.readInt(offsetSize)
        val extraBits = (offsetSize * 8) - segmentBits
        this.file = (file shl extraBits) or (offset ushr segmentBits)
        this.offset = offset and (UInt.MAX_VALUE.toLong() ushr extraBits).toInt()
        size = buffer.readIntLE(lengthSize)
    }

    companion object {
        private fun ByteBuf.readInt(size: Int): Int {
            var value = 0
            repeat(size) { value = value shl 8 or (readByte().toInt() and 0xFF) }
            return value
        }

        private fun ByteBuf.readIntLE(size: Int): Int {
            var value = 0
            repeat(size) { value = value or (readByte().toInt() and 0xFF shl (it * 8)) }
            return value
        }
    }
}
