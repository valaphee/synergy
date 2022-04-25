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

import com.valaphee.synergy.ngdp.util.Blte
import com.valaphee.synergy.ngdp.util.toBigInteger
import io.netty.buffer.Unpooled
import org.apache.commons.vfs2.RandomAccessContent
import java.io.DataInputStream

/**
 * @author Kevin Ludwig
 */
class Data(
    private val data: RandomAccessContent,
    private val reference: Reference
) {
    init {
        data.seek(reference.offset.toLong())
        val dataHeader = Unpooled.wrappedBuffer(ByteArray(dataHeaderSize).apply { data.readFully(this) })
        check(ByteArray(eKeySize).apply {
            dataHeader.readBytes(this)
            reverse()
        }.copyOf(9).toBigInteger() == reference.key)
        check(dataHeader.readIntLE() == reference.size)
        dataHeader.readUnsignedShortLE()
        dataHeader.readIntLE()
        dataHeader.readIntLE()
    }

    val blte get() = Blte(DataInputStream(data.apply { seek(reference.offset.toLong() + dataHeaderSize) }.inputStream))
    val blteInputStream get() = blte.inputStream

    companion object {
        private const val eKeySize = 0x10
        private const val dataHeaderSize = eKeySize + 4 + 2 + 4 + 4
    }
}
