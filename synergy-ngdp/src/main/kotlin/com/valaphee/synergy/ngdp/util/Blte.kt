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

package com.valaphee.synergy.ngdp.util

import java.io.DataInputStream
import java.math.BigInteger

/**
 * @author Kevin Ludwig
 */
class Blte {
    class Chunk(
        val compressedSize: Int,
        val uncompressedSize: Int,
        val checksum: BigInteger
    )

    private val _inputStream: DataInputStream
    val chunks: List<Chunk>

    constructor(stream: DataInputStream) {
        this._inputStream = stream
        check(stream.readInt() == magic)
        stream.readInt()
        check(stream.readUnsignedByte() == flags)
        chunks = List(stream.readUnsignedShort() shl 8 or stream.readUnsignedByte()) { Chunk(stream.readInt(), stream.readInt(), ByteArray(0x10).apply { stream.readFully(this) }.toBigInteger()) }
    }

    val inputStream get() = BlteInputStream(this, _inputStream)

    companion object {
        private const val magic = 0x424C5445 /* BLTE */
        private const val flags = 0xF
    }
}
