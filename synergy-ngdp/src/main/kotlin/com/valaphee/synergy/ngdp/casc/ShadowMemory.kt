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

import io.netty.buffer.ByteBuf
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager

/**
 * @author Kevin Ludwig
 */
class ShadowMemory(
    fileSystemManager: FileSystemManager,
    shadowMemory: ByteBuf,
) {
    val path: FileObject
    val versions = mutableMapOf<Int, Int>()
    private val freeSpace = mutableListOf<Reference>()

    init {
        require(shadowMemory.readIntLE() == HeaderType)
        val headerSize = shadowMemory.readIntLE()
        val rawPath = ByteArray(0x100).apply { shadowMemory.readBytes(this) }
        val path = rawPath.copyOf(rawPath.indexOf(0)).decodeToString().split('\\', limit = 2)
        this.path = fileSystemManager.resolveFile(
            "${
                when (path[0]) {
                    "Global" -> "file"
                    else -> TODO(path[0])
                }
            }:${path[1]}"
        )
        this.path.children.forEach { if (it.name.extension == "idx") versions[it.name.baseName.substring(0, 2).toInt(16)] = 0 }
        val blocks = List((headerSize - shadowMemory.readerIndex() - versions.size * 4) / (4 * 2)) { shadowMemory.readIntLE() to shadowMemory.readIntLE() }
        repeat(versions.size) { versions[it] = shadowMemory.readIntLE() }
        val freeSpaceLength = mutableListOf<Reference>()
        val freeSpaceOffset = mutableListOf<Reference>()
        blocks.forEach {
            shadowMemory.readerIndex(it.second)
            require(shadowMemory.readIntLE() == FreeSpaceType)
            val freeSpaceSize = shadowMemory.readIntLE()
            shadowMemory.skipBytes(0x18)
            repeat(freeSpaceSize) { freeSpaceLength += Reference(shadowMemory, 0, 5, 0, 30) }
            repeat(freeSpaceSize) { freeSpaceOffset += Reference(shadowMemory, 0, 5, 0, 30) }
        }
        freeSpace += freeSpaceLength.zip(freeSpaceOffset).map { Reference(file = it.second.file, offset = it.second.offset, size = it.first.offset) }
    }

    companion object {
        const val FreeSpaceType = 1
        const val HeaderType = 4
    }
}
