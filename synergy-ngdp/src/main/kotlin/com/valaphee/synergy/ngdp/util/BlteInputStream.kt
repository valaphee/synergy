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

import com.google.common.io.ByteStreams
import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

/**
 * @author Kevin Ludwig
 */
class BlteInputStream(
    stream: DataInputStream,
    private val keyring: Map<Key, ByteArray> = emptyMap()
) : InputStream() {
    class Chunk(
        val compressedSize: Int,
        val uncompressedSize: Int,
        val checksum: ByteArray
    )

    private val stream: InputStream = KeepAliveInputStream(stream)
    private val chunks: List<Chunk>
    private var chunkIndex = 0
    private var chunk: Pair<Chunk, InputStream>?
    private var chunkPosition = 0

    init {
        check(stream.readInt() == Magic)
        stream.readInt()
        check(stream.readUnsignedByte() == Flags)
        chunks = List(stream.readUnsignedShort() shl 8 or stream.readUnsignedByte()) { Chunk(stream.readInt(), stream.readInt(), ByteArray(0x10).apply { stream.readFully(this) }) }
        chunk = nextChunk()
    }

    override fun read(): Int {
        while (chunk != null) {
            if (chunkPosition < chunk!!.first.uncompressedSize) {
                chunkPosition++
                val value = chunk!!.second.read()
                if (value != -1) return value
            }
            chunk!!.second.close()
            chunk = nextChunk()
            chunkPosition = 0
        }
        return -1
    }

    override fun read(bytes: ByteArray?, offset: Int, length: Int): Int {
        if (chunk == null) return -1
        else if (bytes == null) throw NullPointerException()
        else if (offset < 0 || length < 0 || length > bytes.size - offset) throw IndexOutOfBoundsException()
        else if (length == 0) return 0
        do {
            if (chunkPosition < chunk!!.first.uncompressedSize) {
                val bytesRead = chunk!!.second.read(bytes, offset, min(length, chunk!!.first.uncompressedSize - chunkPosition))
                if (bytesRead > 0) {
                    chunkPosition += bytesRead
                    return bytesRead
                }
            }
            chunk!!.second.close()
            chunk = nextChunk()
            chunkPosition = 0
        } while (chunk != null)
        return -1
    }

    private fun nextChunk() = if (chunkIndex < chunks.size) {
        val chunk = chunks[chunkIndex++]
        @Suppress("UnstableApiUsage") val chunkStream = DataInputStream(ByteStreams.limit(stream, chunk.compressedSize.toLong()))
        chunk to when (val mode = chunkStream.readUnsignedByte().toChar()) {
            'N' -> chunkStream
            'Z' -> InflaterInputStream(chunkStream, Inflater(), chunk.compressedSize - 1)
            'E' -> {
                val keySize = chunkStream.readUnsignedByte()
                val key = Key(ByteArray(keySize).apply { chunkStream.readFully(this); reverse() })
                keyring[key]?.let {
                    val iv = ByteArray(8).apply { chunkStream.readFully(this, 0, chunkStream.readUnsignedByte()) }

                    var shift = 0
                    var i = 0
                    while (i < 4) {
                        iv[i] = (iv[i].toInt() xor (chunkIndex shr shift and 0xFF)).toByte()
                        shift += 8
                        i++
                    }

                    when (val encryptionMode = chunkStream.readUnsignedByte().toChar()) {
                        'S' -> CipherInputStream(chunkStream, Cipher.getInstance("SALSA20", "BC").apply { init(Cipher.DECRYPT_MODE, SecretKeySpec(it, "SALSA20"), IvParameterSpec(iv)) })
                        else -> TODO("$encryptionMode")
                    }
                } ?: error("Missing key $key")
            }
            else -> TODO("$mode")
        }
    } else null

    override fun close() {
        chunk?.second?.close()
        chunk = null
        stream.close()
    }

    companion object {
        const val Magic = 0x424C5445 /* BLTE */
        const val Flags = 0xF
    }
}
