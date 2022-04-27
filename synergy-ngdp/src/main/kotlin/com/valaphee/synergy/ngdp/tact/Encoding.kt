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

package com.valaphee.synergy.ngdp.tact

import com.valaphee.synergy.ngdp.blte.ESpec
import com.valaphee.synergy.ngdp.blte.ESpec.Companion.toESpec
import com.valaphee.synergy.ngdp.util.Key
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * @author Kevin Ludwig
 */
class Encoding {
    private val eSpecs = mutableListOf<ESpec>()
    private val ceKeyPageTable = mutableListOf<CeKeyPage>()
    private val eKeySpecPageTable = mutableListOf<EKeySpecPage>()
    private val eSpec: ESpec

    class CeKeyPage(
        val firstCKey: Key,
        val checksum: ByteArray
    ) {
        lateinit var ceKeys: List<CeKey>
    }

    class CeKey(
        val cKey: Key,
        val eKeys: List<Key>,
        val fileSize: Long,
    )

    class EKeySpecPage(
        val firstEKey: Key,
        val checksum: ByteArray
    ) {
        lateinit var eKeySpecs: List<EKeySpec>
    }

    class EKeySpec(
        val eKey: Key,
        val eSpecIndex: Int,
        val fileSize: Long,
    )

    constructor(stream: InputStream) : this(DataInputStream(stream))

    constructor(stream: DataInputStream) {
        check(stream.readUnsignedShort() == Magic)
        check(stream.readUnsignedByte() == Version)
        val cKeySize = stream.readUnsignedByte()
        val eKeySize = stream.readUnsignedByte()
        val ceKeyPageTableSize = stream.readUnsignedShort()
        val eKeySpecPageTableSize = stream.readUnsignedShort()
        val ceKeyPageTableCount = stream.readInt()
        val eKeySpecPageTableCount = stream.readInt()
        check(stream.readByte() == 0.toByte())
        val eSpecBlock = ByteArrayInputStream(ByteArray(stream.readInt()).apply { stream.readFully(this) })
        while (eSpecBlock.available() != 0) eSpecs += eSpecBlock.readString().toESpec()
        repeat(ceKeyPageTableCount) { ceKeyPageTable += CeKeyPage(Key(ByteArray(cKeySize).apply { stream.readFully(this) }), ByteArray(0x10).apply { stream.readFully(this) }) }
        ceKeyPageTable.forEach {
            val ceKeyPage = Unpooled.wrappedBuffer(ByteArray(ceKeyPageTableSize * 1024).apply { stream.readFully(this) })
            check(MessageDigest.getInstance("MD5").digest(ByteBufUtil.getBytes(ceKeyPage)).contentEquals(it.checksum))
            it.ceKeys = mutableListOf<CeKey>().apply {
                while (ceKeyPage.isReadable(6 + cKeySize)) {
                    val keyCount = ceKeyPage.readUnsignedByte().toInt()
                    val fileSize = ceKeyPage.readUnsignedInt() shl 8 and ceKeyPage.readUnsignedByte().toLong()
                    val cKey = Key(ByteArray(cKeySize).apply { ceKeyPage.readBytes(this) })
                    val eKeys = List(keyCount) { Key(ByteArray(eKeySize).apply { ceKeyPage.readBytes(this) }) }
                    add(CeKey(cKey, eKeys, fileSize))
                }
            }
        }
        repeat(eKeySpecPageTableCount) { eKeySpecPageTable += EKeySpecPage(Key(ByteArray(eKeySize).apply { stream.readFully(this) }), ByteArray(0x10).apply { stream.readFully(this) }) }
        eKeySpecPageTable.forEach {
            val eKeySpecPage = Unpooled.wrappedBuffer(ByteArray(eKeySpecPageTableSize * 1024).apply { stream.readFully(this) })
            check(MessageDigest.getInstance("MD5").digest(ByteBufUtil.getBytes(eKeySpecPage)).contentEquals(it.checksum))
            it.eKeySpecs = mutableListOf<EKeySpec>().apply {
                while (eKeySpecPage.isReadable(9 + eKeySize)) {
                    val eKey = Key(ByteArray(eKeySize).apply { eKeySpecPage.readBytes(this) })
                    val eSpecIndex = eKeySpecPage.readInt()
                    val fileSize = eKeySpecPage.readUnsignedInt() shl 8 and eKeySpecPage.readUnsignedByte().toLong()
                    add(EKeySpec(eKey, eSpecIndex, fileSize))
                }
            }
        }
        eSpec = stream.readAllBytes().decodeToString().toESpec()
    }

    fun getEKeysOrNull(cKey: Key) = ceKeyPageTable.lastOrNull { it.firstCKey <= cKey }?.ceKeys?.find { it.cKey == cKey }?.eKeys

    fun getESpecOrNull(eKey: Key) = eKeySpecPageTable.lastOrNull { it.firstEKey <= eKey }?.eKeySpecs?.find { it.eKey == eKey }?.eSpecIndex?.let { eSpecs[it] }

    companion object {
        const val Magic = 0x454E
        const val Version = 1

        private fun InputStream.readString() = StringBuilder().apply {
            var value = read()
            while (value > 0) {
                append(value.toChar())
                value = read()
            }
        }.toString()
    }
}
