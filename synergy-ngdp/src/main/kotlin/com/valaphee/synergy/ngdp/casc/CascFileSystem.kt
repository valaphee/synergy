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

import com.valaphee.synergy.ngdp.util.toBigInteger
import io.netty.util.internal.StringUtil
import org.apache.commons.vfs2.Capability
import org.apache.commons.vfs2.FileName
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.provider.AbstractFileName
import org.apache.commons.vfs2.provider.AbstractFileSystem
import kotlin.math.min

/**
 * @author Kevin Ludwig
 */
class CascFileSystem(
    rootFileName: FileName,
    parentLayer: FileObject,
    fileSystemOptions: FileSystemOptions?
) : AbstractFileSystem(rootFileName, parentLayer, fileSystemOptions) {
    private lateinit var shadowMemory: ShadowMemory
    private lateinit var index: Index

    override fun init() {
        super.init()

        shadowMemory = ShadowMemory(parentLayer.resolveFile("shmem"))
        index = Index(shadowMemory.path, shadowMemory.versions)
    }

    override fun addCapabilities(capabilities: MutableCollection<Capability>) {
        capabilities += CascFileProvider._capabilities
    }

    override fun createFile(name: AbstractFileName) = if (name.path != "/") {
        val key = if (name.path.length and 0x1 == 0) "0${name.path.substring(1)}" else name.path.substring(1)
        CascFileObject(name, this, index[StringUtil.decodeHexDump(key, 0, min(9 * 2, key.length)).toBigInteger()]?.let { Data(shadowMemory.path, it) })
    } else CascRootFileObject(name, this, index)
}
