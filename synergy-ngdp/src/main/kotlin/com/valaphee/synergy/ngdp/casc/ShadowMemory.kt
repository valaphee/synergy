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
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import java.io.File

/**
 * @author Kevin Ludwig
 */
class ShadowMemory(
    private val shadowMemoryFile: File
) {
    val path: File
    val versions = mutableMapOf<Int, Int>()
    private val freeSpace = mutableListOf<Reference>()

    init {
        val shadowMemory = Unpooled.wrappedBuffer(shadowMemoryFile.readBytes())
        val headerType = shadowMemory.readIntLE()
        check(headerType == HeaderType)
        val headerSize = shadowMemory.readIntLE()
        val rawPath = ByteArray(0x100).apply { shadowMemory.readBytes(this) }
        val path = rawPath.copyOf(rawPath.indexOf(0)).decodeToString().split('\\', limit = 2)
        check(path[0] == "Global")
        this.path = File(path[1])
        this.path.listFiles { file -> file.extension == "idx" }!!.forEach { versions[it.name.substring(0, 2).toInt(16)] = 0 }
        val blocks = List((headerSize - shadowMemory.readerIndex() - versions.size * 4) / (4 * 2)) { shadowMemory.readIntLE() to shadowMemory.readIntLE() }
        repeat(versions.size) { versions[it] = shadowMemory.readIntLE() }
        val freeSpaceLength = mutableListOf<Reference>()
        val freeSpaceOffset = mutableListOf<Reference>()
        blocks.forEach {
            shadowMemory.readerIndex(it.second)
            check(shadowMemory.readIntLE() == FreeSpaceType)
            val freeSpaceSize = shadowMemory.readIntLE()
            shadowMemory.skipBytes(0x18)
            repeat(freeSpaceSize) { freeSpaceLength += Reference(shadowMemory, keySize = 0, lengthSize = 0) }
            repeat(freeSpaceSize) { freeSpaceOffset += Reference(shadowMemory, keySize = 0, lengthSize = 0) }
        }
        freeSpace += freeSpaceLength.zip(freeSpaceOffset).map { Reference(emptyKey, it.second.file, it.second.offset, it.first.offset) }
    }

    fun write() {
        val shadowMemory = Unpooled.buffer()
        shadowMemory.writeIntLE(HeaderType)
        val headerSizeIndex = shadowMemory.writerIndex()
        shadowMemory.writeIntLE(0)
        /*shadowMemory.writeBytes("${when (path.uri.scheme) {
            "file" -> "Global"
            else -> TODO(path.uri.scheme)
        }}\\${path.uri.path.removePrefix("/")}".toByteArray().copyOf(0x100)) TODO*/
        val blockSizeIndex = shadowMemory.writerIndex()
        shadowMemory.writeIntLE(0)
        val blockOffsetIndex = shadowMemory.writerIndex()
        shadowMemory.writeIntLE(0)
        versions.forEach { shadowMemory.writeIntLE(it.value) }
        shadowMemory.setIntLE(headerSizeIndex, shadowMemory.writerIndex())
        shadowMemory.setIntLE(blockOffsetIndex, shadowMemory.writerIndex())
        val blockOffset = shadowMemory.writerIndex()
        shadowMemory.writeIntLE(FreeSpaceType)
        shadowMemory.writeIntLE(freeSpace.size)
        shadowMemory.writeZero(0x18)
        freeSpace.forEach { shadowMemory.writeBytes(Reference(emptyKey, 0, it.length, 0).toBuffer(keySize = 0, lengthSize = 0)) }
        freeSpace.forEach { shadowMemory.writeBytes(Reference(emptyKey, it.file, it.offset, 0).toBuffer(keySize = 0, lengthSize = 0)) }
        shadowMemory.setIntLE(blockSizeIndex, shadowMemory.writerIndex() - blockOffset)
        shadowMemory.writeZero(0x0B)
        shadowMemoryFile.writeBytes(ByteBufUtil.getBytes(shadowMemory))
    }

    companion object {
        const val FreeSpaceType = 1
        const val HeaderType = 4
        const val HeaderIndexType = 5
        private val emptyKey = Key(byteArrayOf())
    }
}
