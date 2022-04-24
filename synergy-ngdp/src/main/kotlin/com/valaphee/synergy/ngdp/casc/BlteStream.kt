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

package com.valaphee.synergy.casc.com.valaphee.synergy.ngdp.casc

import org.apache.commons.vfs2.RandomAccessContent
import java.io.InputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlin.math.min

/**
 * @author Kevin Ludwig
 */
class BlteStream(
    private val content: RandomAccessContent,
    offset: Int
) : InputStream() {
    private val chunkIterator: Iterator<Chunk>
    private var chunk: Chunk?
    private var position: Int = 0

    init {
        check(content.readInt() == 0x424C5445)
        val headerSize = content.readInt()

        content.readByte()
        var chunkOffset = offset + 0x1E + headerSize
        chunkIterator = List(content.readUnsignedShort() shl 8 or content.readUnsignedByte()) {
            val compressedSize = content.readInt()
            val uncompressedSize = content.readInt()
            ByteArray(0x10).apply { content.readFully(this) }
            Chunk(compressedSize, uncompressedSize, chunkOffset.also { chunkOffset += compressedSize })
        }.iterator()
        chunk = chunkIterator.next()
    }

    override fun read(): Int {
        while (chunk != null) {
            if (position < chunk!!.uncompressedSize) {
                position++
                val value = chunk!!.stream.read()
                if (value != -1) return value
            }
            chunk = if (chunkIterator.hasNext()) chunkIterator.next() else null
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
            if (position < chunk!!.uncompressedSize) {
                val bytesRead = chunk!!.stream.read(bytes, offset, min(length, chunk!!.uncompressedSize - position))
                if (bytesRead > 0) {
                    position += bytesRead
                    return bytesRead
                }
            }
            chunk = if (chunkIterator.hasNext()) chunkIterator.next() else null
            position = 0
        } while (chunk != null)
        return -1
    }

    override fun close() {
        chunk = null
        position = 0
        content.close()
    }

    private inner class Chunk(
        compressedSize: Int,
        val uncompressedSize: Int,
        offset: Int
    ) {
        val stream: InputStream by lazy {
            content.seek(offset.toLong())
            when (val mode = content.readUnsignedByte().toChar()) {
                'N' -> content.inputStream
                'Z' -> InflaterInputStream(content.inputStream, Inflater(), compressedSize - 1)
                else -> TODO("$mode")
            }
        }
    }
}
