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

import com.valaphee.synergy.ngdp.casc.Reference
import io.netty.buffer.Unpooled
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.RandomAccessContent
import org.apache.commons.vfs2.util.RandomAccessMode
import java.io.Closeable

/**
 * @author Kevin Ludwig
 */
class Data(
    path: FileObject,
    private val reference: Reference
) : Closeable {
    private val content: RandomAccessContent = path.resolveFile(String.format("data.%03d", reference.file)).content.getRandomAccessContent(RandomAccessMode.READ)

    init {
        content.seek(reference.offset.toLong())
        val headerBuffer = Unpooled.wrappedBuffer(ByteArray(0x1E).apply { content.readFully(this) })
        ByteArray(0x10).apply { headerBuffer.readBytes(this) }
        val size = headerBuffer.readIntLE()
        check(size == reference.size)
        headerBuffer.readShortLE()
        headerBuffer.readIntLE()
        headerBuffer.readIntLE()
    }

    val blteStream get() = BlteStream(content, reference.offset)

    override fun close() {
        content.close()
    }
}
