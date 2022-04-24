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

import com.valaphee.synergy.ngdp.util.toBigInteger
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest

/**
 * @author Kevin Ludwig
 */
class Encoding {
    private val eSpecs = mutableListOf<String>()
    private val ceKeyPageTable = mutableListOf<CeKeyPage>()
    private val eKeySpecPageTable = mutableListOf<EKeySpecPage>()

    class CeKeyPage(
        val firstCKey: BigInteger,
        val checksum: BigInteger
    ) {
        lateinit var ceKeys: List<CeKey>
    }

    class CeKey(
        val cKey: BigInteger,
        val eKeys: List<BigInteger>,
        val fileSize: Long,
    )

    class EKeySpecPage(
        val firstEKey: BigInteger,
        val checksum: BigInteger
    ) {
        lateinit var eKeySpecs: List<EKeySpec>
    }

    class EKeySpec(
        val eKey: BigInteger,
        val eSpecIndex: Int,
        val fileSize: Long,
    )

    constructor(stream: DataInputStream) {
        check(stream.readUnsignedShort() == magic)
        check(stream.readUnsignedByte() == version)
        val cKeySize = stream.readUnsignedByte()
        val eKeySize = stream.readUnsignedByte()
        val ceKeyPageTableSize = stream.readUnsignedShort()
        val eKeySpecPageTableSize = stream.readUnsignedShort()
        val ceKeyPageTableCount = stream.readInt()
        val eKeySpecPageTableCount = stream.readInt()
        check(stream.readByte() == 0.toByte())
        val eSpecBlock = ByteArrayInputStream(ByteArray(stream.readInt()).apply { stream.readFully(this) })
        while (eSpecBlock.available() != 0) eSpecs += eSpecBlock.readString()
        repeat(ceKeyPageTableCount) { ceKeyPageTable += CeKeyPage(ByteArray(cKeySize).apply { stream.readFully(this) }.toBigInteger(), ByteArray(0x10).apply { stream.readFully(this) }.toBigInteger()) }
        ceKeyPageTable.forEach {
            val ceKeyPage = Unpooled.wrappedBuffer(ByteArray(ceKeyPageTableSize * 1024).apply { stream.readFully(this) })
            check(it.checksum == MessageDigest.getInstance("MD5").digest(ByteBufUtil.getBytes(ceKeyPage)).toBigInteger())
            it.ceKeys = mutableListOf<CeKey>().apply {
                while (ceKeyPage.isReadable(6 + cKeySize)) {
                    val keyCount = ceKeyPage.readUnsignedByte().toInt()
                    val fileSize = ceKeyPage.readUnsignedInt() shl 8 and ceKeyPage.readUnsignedByte().toLong()
                    val cKey = ByteArray(cKeySize).apply { ceKeyPage.readBytes(this) }.toBigInteger()
                    val eKeys = List(keyCount) { ByteArray(eKeySize).apply { ceKeyPage.readBytes(this) }.toBigInteger() }
                    add(CeKey(cKey, eKeys, fileSize))
                }
            }
        }
        repeat(eKeySpecPageTableCount) { eKeySpecPageTable += EKeySpecPage(ByteArray(eKeySize).apply { stream.readFully(this) }.toBigInteger(), ByteArray(0x10).apply { stream.readFully(this) }.toBigInteger()) }
        eKeySpecPageTable.forEach {
            val eKeySpecPage = Unpooled.wrappedBuffer(ByteArray(eKeySpecPageTableSize * 1024).apply { stream.readFully(this) })
            check(it.checksum == MessageDigest.getInstance("MD5").digest(ByteBufUtil.getBytes(eKeySpecPage)).toBigInteger())
            it.eKeySpecs = mutableListOf<EKeySpec>().apply {
                while (eKeySpecPage.isReadable(9 + eKeySize)) {
                    val eKey = ByteArray(eKeySize).apply { eKeySpecPage.readBytes(this) }.toBigInteger()
                    val eSpecIndex = eKeySpecPage.readInt()
                    val fileSize = eKeySpecPage.readUnsignedInt() shl 8 and eKeySpecPage.readUnsignedByte().toLong()
                    add(EKeySpec(eKey, eSpecIndex, fileSize))
                }
            }
        }
    }

    fun getEKeysOrNull(cKey: BigInteger) = ceKeyPageTable.lastOrNull { it.firstCKey <= cKey }?.ceKeys?.find { it.cKey == cKey }?.eKeys

    fun getESpecOrNull(eKey: BigInteger) = eKeySpecPageTable.lastOrNull { it.firstEKey <= eKey }?.eKeySpecs?.find { it.eKey == eKey }?.eSpecIndex?.let { eSpecs[it] }

    companion object {
        private const val magic = 0x454E
        private const val version = 1

        private fun InputStream.readString() = StringBuilder().apply {
            var value = read()
            while (value > 0) {
                append(value.toChar())
                value = read()
            }
        }.toString()
    }
}
