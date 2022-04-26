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

import com.valaphee.synergy.ngdp.util.Key
import io.netty.buffer.Unpooled
import org.apache.commons.vfs2.RandomAccessContent

/**
 * @author Kevin Ludwig
 */
class Data(
    private val data: RandomAccessContent,
    val reference: Reference
) {
    private val noData: Boolean

    init {
        data.seek(reference.offset.toLong())
        val dataHeader = Unpooled.wrappedBuffer(ByteArray(HeaderSize).apply { data.readFully(this) })
        check(Key(ByteArray(KeySize).apply {
            dataHeader.readBytes(this)
            reverse()
        }.copyOf(9)) == reference.key)
        check(dataHeader.readIntLE() == reference.length)
        noData = dataHeader.readUnsignedShortLE() and 0b1 == 1
        dataHeader.readIntLE()
        dataHeader.readIntLE()
    }

    val inputStream get() = if (!noData) data.apply { seek(reference.offset.toLong() + HeaderSize) }.inputStream else null

    companion object {
        const val KeySize = 0x10
        const val HeaderSize = KeySize + 4 + 2 + 4 + 4
    }
}
