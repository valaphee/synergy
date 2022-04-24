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

import org.apache.commons.vfs2.FileSystemException
import org.apache.commons.vfs2.FileType
import org.apache.commons.vfs2.provider.AbstractFileName
import org.apache.commons.vfs2.provider.AbstractFileObject

/**
 * @author Kevin Ludwig
 */
class CascFileObject(
    name: AbstractFileName,
    fileSystem: CascFileSystem,
    val data: Data?
) : AbstractFileObject<CascFileSystem>(name, fileSystem) {
    override fun doGetType() = if (data != null) FileType.FILE else FileType.IMAGINARY

    override fun doListChildren() = null

    override fun doGetContentSize() = data?.blte?.chunks?.sumOf { it.uncompressedSize }?.toLong() ?: 0L

    override fun doGetInputStream() = data?.blteInputStream ?: throw FileSystemException("vfs.provider/read-not-file.error", name)
}
