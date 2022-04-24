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

package com.valaphee.synergy.ngdp

import com.valaphee.synergy.ngdp.casc.CascFileProvider
import com.valaphee.synergy.ngdp.tact.BuildInfo
import com.valaphee.synergy.ngdp.tact.Config
import com.valaphee.synergy.ngdp.tact.Encoding
import com.valaphee.synergy.ngdp.tank.ContentManifest
import com.valaphee.synergy.ngdp.tank.Root
import com.valaphee.synergy.ngdp.util.toBigInteger
import io.netty.util.internal.StringUtil
import org.apache.commons.vfs2.cache.DefaultFilesCache
import org.apache.commons.vfs2.impl.DefaultFileSystemManager
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider
import java.io.DataInputStream

fun main() {
    val fileSystemManager = DefaultFileSystemManager().apply {
        addProvider("file", DefaultLocalFileProvider())
        addProvider("casc", CascFileProvider())
        filesCache = DefaultFilesCache()
        init()
    }

    val casc = fileSystemManager.resolveFile("casc:file:C:/Program Files (x86)/Overwatch/data/casc/data")

    val buildInfo = BuildInfo(fileSystemManager.resolveFile("file:C:/Program Files (x86)/Overwatch/.build.info").content.getString(Charsets.UTF_8))
    val buildKey = checkNotNull(buildInfo[0]["Build Key"])
    val buildConfig = Config(fileSystemManager.resolveFile("file:C:/Program Files (x86)/Overwatch/data/casc/config/${buildKey.substring(0, 2)}/${buildKey.substring(2, 4)}/$buildKey").content.getString(Charsets.UTF_8))
    val encoding = casc.resolveFile(checkNotNull(buildConfig["encoding"])[1]).content.inputStream.use { Encoding(DataInputStream(it)) }
    val root = casc.resolveFile(StringUtil.toHexString(checkNotNull(encoding.getEKeysOrNull(StringUtil.decodeHexDump(checkNotNull(buildConfig["root"])[0]).toBigInteger()))[0].toByteArray())).content.inputStream.use { Root(it.readAllBytes().decodeToString()) }
    val cmfFile = checkNotNull(root.entries.find { it["FILENAME"] == "TactManifest/Win_SPWin_RCN_LdeDE_text_EExt.cmf" })
    val cmf = casc.resolveFile(StringUtil.toHexString(checkNotNull(encoding.getEKeysOrNull(StringUtil.decodeHexDump(cmfFile["MD5"]).toBigInteger()))[0].toByteArray())).content.inputStream.use { ContentManifest(checkNotNull(cmfFile["FILENAME"]).split('/').last(), it) }

    casc.children.forEach {
        /*(it as CascFileObject).data?.let {
            println(it.blte.chunks)
        }*/
    }
    println(casc.children.size)

}
