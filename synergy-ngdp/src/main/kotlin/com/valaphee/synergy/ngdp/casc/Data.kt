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
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.RandomAccessContent
import org.apache.commons.vfs2.util.RandomAccessMode
import java.io.Closeable
import java.io.DataInputStream
import java.io.InputStream

/**
 * @author Kevin Ludwig
 */
class Data(
    path: FileObject,
    reference: Reference
) : Closeable {
    private val content: RandomAccessContent = path.resolveFile(String.format("data.%03d", reference.file)).content.getRandomAccessContent(RandomAccessMode.READ)

    init {
        content.seek(reference.offset.toLong())
        val buffer = Unpooled.wrappedBuffer(ByteArray(eKeySize + 4 + 2 + 4 + 4).apply { content.readFully(this) })
        check(ByteArray(eKeySize).apply {
            buffer.readBytes(this)
            reverse()
        }.copyOf(9).toBigInteger() == reference.key)
        check(buffer.readIntLE() == reference.size)
        buffer.readUnsignedShortLE()
        buffer.readIntLE()
        buffer.readIntLE()
    }

    val blte get() = Blte(DataInputStream(content.inputStream))
    val blteInputStream: InputStream get() = blte.inputStream

    override fun close() {
        content.close()
    }

    companion object {
        private const val eKeySize = 0x10
    }
}
