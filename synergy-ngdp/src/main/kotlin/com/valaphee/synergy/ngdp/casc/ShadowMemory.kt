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

import io.netty.buffer.Unpooled
import org.apache.commons.vfs2.FileObject

/**
 * @author Kevin Ludwig
 */
class ShadowMemory(
    shadowMemoryFile: FileObject
) {
    val path: FileObject
    val versions = mutableMapOf<Int, Int>()
    private val freeSpaceLength = mutableListOf<Reference>()
    private val freeSpaceOffset = mutableListOf<Reference>()

    init {
        val buffer = Unpooled.wrappedBuffer(shadowMemoryFile.content.byteArray)
        val headerType = buffer.readIntLE()
        when (headerType) {
            4 -> {
                val headerSize = buffer.readIntLE()

                val rawPath = ByteArray(0x100).apply { buffer.readBytes(this) }
                val path = rawPath.copyOf(rawPath.indexOf(0x00)).decodeToString().split('\\', limit = 2)
                this.path = shadowMemoryFile.fileSystem.fileSystemManager.resolveFile("${when (path[0]) {
                    "Global" -> "file"
                    else -> TODO(path[0])
                }}:${path[1]}")
                this.path.children.forEach { if (it.name.extension == "idx") versions[it.name.baseName.substring(0, 2).toInt(16)] = 0 }

                val blockCount = (headerSize - 264 - versions.size * 4) / (4 * 2)
                val blocks = List(blockCount) { buffer.readIntLE() to buffer.readIntLE() }
                repeat(versions.size) { versions[it] = buffer.readIntLE() }

                blocks.forEach {
                    buffer.readerIndex(it.second)

                    val freeSpaceType = buffer.readIntLE()
                    val freeSpaceSize = buffer.readIntLE()
                    when (freeSpaceType) {
                        1 -> {
                            buffer.skipBytes(24)
                            repeat(freeSpaceSize) { freeSpaceLength += Reference(buffer, 0, 5, 0, 30) }
                            repeat(freeSpaceSize) { freeSpaceOffset += Reference(buffer, 0, 5, 0, 30) }
                        }
                        else -> TODO("$freeSpaceType")
                    }
                }
            }
            else -> TODO("$headerType")
        }
    }
}
