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

package com.valaphee.synergy.ngdp.blte

import com.igormaznitsa.jbbp.io.JBBPBitInputStream
import com.valaphee.synergy.ngdp.util.Key
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.io.CipherInputStream
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.InflaterInputStream
import kotlin.math.min

/**
 * Block Table-encoded
 *
 * @author Kevin Ludwig
 */
class BlteInputStream(
    private val stream: InputStream,
    private val keyring: Map<Key, ByteArray> = emptyMap()
) : InputStream() {
    private val header = BlteHeader()

    private var chunkIndex = 0
    private var chunk: Pair<BlteHeader.CHUNK, InputStream>?
    private var chunkPosition = 0

    init {
        header.read(JBBPBitInputStream(stream))
        check(header.magic == 0x424C5445)
        check(header.flags.code == 0xF)
        check(header.size == 4 + 4 + 1 + 2 + 1 + header.chunk_count.code * (4 + 4 + 0x10))
        chunk = nextChunk()
    }

    override fun read(): Int {
        while (chunk != null) {
            if (chunkPosition < chunk!!.first.uncompressed_size) {
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
            if (chunkPosition < chunk!!.first.uncompressed_size) {
                val bytesRead = chunk!!.second.read(bytes, offset, min(length, chunk!!.first.uncompressed_size - chunkPosition))
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

    private fun nextChunk() = if (chunkIndex < header.chunk_count.code) {
        val chunk = header.chunk[chunkIndex]
        chunk to ByteArrayInputStream(stream.readNBytes(chunk.compressed_size).also { check(MessageDigest.getInstance("MD5").digest(it).contentEquals(chunk.checksum)) }).handleChunk().also { chunkIndex++ }
    } else null

    private fun InputStream.handleChunk(): InputStream = when (val mode = read().toChar()) {
        'N' -> this
        'Z' -> InflaterInputStream(this)
        'E' -> {
            val keySize = read()
            val key = Key(ByteArray(keySize).apply { read(this); reverse() })
            keyring[key]?.let {
                val iv = ByteArray(read()).apply { read(this) }
                CipherInputStream(this, when (val encryptionMode = read().toChar()) {
                    'S' -> {
                        val _iv = iv.copyOf(8)
                        var shift = 0
                        var i = 0
                        while (i < 4) {
                            _iv[i] = (_iv[i].toInt() xor (chunkIndex shr shift and 0xFF)).toByte()
                            shift += 8
                            i++
                        }
                        Salsa20Engine().apply { init(false, ParametersWithIV(KeyParameter(it), _iv)) }
                    }
                    else -> TODO("$encryptionMode")
                }).handleChunk()
            } ?: error("Missing key $key")
        }
        else -> TODO("$mode")
    }

    override fun close() {
        chunk?.second?.close()
        chunk = null
        stream.close()
    }
}
