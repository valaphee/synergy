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
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.RandomAccessContent
import org.apache.commons.vfs2.util.RandomAccessMode
import java.security.MessageDigest

/**
 * Content-addressed Storage Container
 *
 * @author Kevin Ludwig
 */
class Casc(
    path: FileObject
) {
    private val shadowMemory = ShadowMemory(path.resolveFile("shmem"))
    private val index = Index(shadowMemory)
    private var data = mutableMapOf<Int, RandomAccessContent>()

    @Deprecated("")
    val entries get() = index.entries.values.asSequence().map { Data(data.getOrPut(it.file) { shadowMemory.path.resolveFile(String.format("data.%03d", it.file)).content.getRandomAccessContent(RandomAccessMode.READ) }, it) }

    operator fun get(key: Key) = index[key]?.let { Data(data.getOrPut(it.file) { shadowMemory.path.resolveFile(String.format("data.%03d", it.file)).content.getRandomAccessContent(RandomAccessMode.READ) }, it) }

    fun add(data: ByteArray) = Key(MessageDigest.getInstance("MD5").digest(data))

    fun remove(key: Key) = Unit
}
