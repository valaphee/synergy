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

import org.apache.commons.vfs2.Capability
import org.apache.commons.vfs2.FileName
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.FileType
import org.apache.commons.vfs2.provider.AbstractLayeredFileProvider
import org.apache.commons.vfs2.provider.LayeredFileName

/**
 * @author Kevin Ludwig
 */
class CascFileProvider : AbstractLayeredFileProvider() {
    override fun getCapabilities() = _capabilities

    override fun doCreateFileSystem(scheme: String, file: FileObject, fileSystemOptions: FileSystemOptions?) = CascFileSystem(LayeredFileName(scheme, file.name, FileName.ROOT_PATH, FileType.FOLDER), file, fileSystemOptions)

    companion object {
        internal val _capabilities = listOf(Capability.READ_CONTENT, Capability.LIST_CHILDREN)
    }
}
