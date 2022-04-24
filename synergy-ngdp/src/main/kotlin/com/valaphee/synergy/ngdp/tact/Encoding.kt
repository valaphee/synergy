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

package com.valaphee.synergy.casc.com.valaphee.synergy.ngdp.tact

import com.valaphee.synergy.casc.com.valaphee.synergy.ngdp.util.toBigInteger
import io.netty.buffer.Unpooled
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.math.BigInteger

/**
 * @author Kevin Ludwig
 */
class Encoding(
    stream: DataInputStream
) {
    private val ceKeyPageTable: List<CeKeyPage>
    private val eKeySpecPageTable: List<EKeySpecPage>

    class CeKeyPage(
        val firstCKey: BigInteger,
        val md5: BigInteger
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
        val md5: BigInteger
    ) {
        lateinit var eKeySpecs: List<EKeySpec>
    }

    class EKeySpec(
        val eKey: BigInteger,
        val eSpecIndex: Int,
        val fileSize: Long,
    )

    init {
        check(stream.readUnsignedShort() == 0x454E)
        check(stream.readUnsignedByte() == 1)
        val cKeyHashSize = stream.readUnsignedByte()
        val eKeyHashSize = stream.readUnsignedByte()
        val ceKeyPageTableSize = stream.readUnsignedShort()
        val eKeySpecPageTableSize = stream.readUnsignedShort()
        val ceKeyPageTableCount = stream.readInt()
        val eKeySpecPageTableCount = stream.readInt()
        check(stream.readByte() == 0.toByte())
        val especBlock = ByteArrayInputStream(ByteArray(stream.readInt()).apply { stream.readFully(this) })
        while (especBlock.available() != 0) especBlock.readString()
        ceKeyPageTable = List(ceKeyPageTableCount) { CeKeyPage(ByteArray(cKeyHashSize).apply { stream.readFully(this) }.toBigInteger(), ByteArray(0x10).apply { stream.readFully(this) }.toBigInteger()) }
        ceKeyPageTable.forEach {
            it.ceKeys = mutableListOf<CeKey>().apply {
                val ceKeyPage = Unpooled.wrappedBuffer(ByteArray(ceKeyPageTableSize * 1024).apply { stream.readFully(this) })
                while (ceKeyPage.isReadable(6 + cKeyHashSize)) {
                    val keyCount = ceKeyPage.readUnsignedByte().toInt()
                    val fileSize = ceKeyPage.readUnsignedInt() shl 8 and ceKeyPage.readUnsignedByte().toLong()
                    val cKey = ByteArray(cKeyHashSize).apply { ceKeyPage.readBytes(this) }.toBigInteger()
                    val eKeys = List(keyCount) { ByteArray(eKeyHashSize).apply { ceKeyPage.readBytes(this) }.toBigInteger() }
                    add(CeKey(cKey, eKeys, fileSize))
                }
            }
        }
        eKeySpecPageTable = List(eKeySpecPageTableCount) { EKeySpecPage(ByteArray(eKeyHashSize).apply { stream.readFully(this) }.toBigInteger(), ByteArray(0x10).apply { stream.readFully(this) }.toBigInteger()) }
        eKeySpecPageTable.forEach {
            it.eKeySpecs = mutableListOf<EKeySpec>().apply {
                val eKeySpecPage = Unpooled.wrappedBuffer(ByteArray(eKeySpecPageTableSize * 1024).apply { stream.readFully(this) })
                while (eKeySpecPage.isReadable(9 + eKeyHashSize)) {
                    val eKey = ByteArray(eKeyHashSize).apply { eKeySpecPage.readBytes(this) }.toBigInteger()
                    val eSpecIndex = eKeySpecPage.readInt()
                    val fileSize = eKeySpecPage.readUnsignedInt() shl 8 and eKeySpecPage.readUnsignedByte().toLong()
                    add(EKeySpec(eKey, eSpecIndex, fileSize))
                }
            }
        }
        stream.readString()
    }

    fun getCeKey(cKey: BigInteger) = ceKeyPageTable.lastOrNull { it.firstCKey <= cKey }?.ceKeys?.find { it.cKey == cKey }

    fun getEKeySpec(eKey: BigInteger) = eKeySpecPageTable.lastOrNull { it.firstEKey <= eKey }?.eKeySpecs?.find { it.eKey == eKey }

    companion object {
        private fun InputStream.readString() = StringBuilder().apply {
            var value = read()
            while (value > 0) {
                append(value.toChar())
                value = read()
            }
        }.toString()
    }
}
