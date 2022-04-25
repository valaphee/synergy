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
import java.io.InputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlin.math.min

/**
 * @author Kevin Ludwig
 */
class BlteInputStream(
    blte: Blte,
    private val stream: DataInputStream
) : InputStream() {
    private val chunkIterator: Iterator<Blte.Chunk> = blte.chunks.iterator()
    private var chunk: Pair<Blte.Chunk, InputStream>? = nextChunk()
    private var position: Int = 0

    override fun read(): Int {
        while (chunk != null) {
            if (position < chunk!!.first.uncompressedSize) {
                position++
                val value = chunk!!.second.read()
                if (value != -1) return value
            }
            chunk = nextChunk()
            position = 0
        }
        return -1
    }

    override fun read(bytes: ByteArray?, offset: Int, length: Int): Int {
        if (chunk == null) return -1
        else if (bytes == null) throw NullPointerException()
        else if (offset < 0 || length < 0 || length > bytes.size - offset) throw IndexOutOfBoundsException()
        else if (length == 0) return 0
        do {
            if (position < chunk!!.first.uncompressedSize) {
                val bytesRead = chunk!!.second.read(bytes, offset, min(length, chunk!!.first.uncompressedSize - position))
                if (bytesRead > 0) {
                    position += bytesRead
                    return bytesRead
                }
            }
            chunk = nextChunk()
            position = 0
        } while (chunk != null)
        return -1
    }

    private fun nextChunk() = if (chunkIterator.hasNext()) {
        val chunk = chunkIterator.next()
        chunk to when (val mode = this@BlteInputStream.stream.readUnsignedByte().toChar()) {
            'N' -> this@BlteInputStream.stream
            'Z' -> InflaterInputStream(this@BlteInputStream.stream, Inflater(), chunk.compressedSize - 1)
            else -> TODO("$mode")
        }
    } else null

    override fun close() {
        chunk = null
        position = 0
    }
}
