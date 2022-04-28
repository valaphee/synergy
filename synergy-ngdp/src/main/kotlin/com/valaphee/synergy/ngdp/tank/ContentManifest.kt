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

package com.valaphee.synergy.ngdp.tank

import com.valaphee.synergy.ngdp.util.Key
import com.valaphee.synergy.ngdp.tank.encryption.EncryptionProc
import com.valaphee.synergy.ngdp.tank.util.Guid
import io.netty.buffer.Unpooled
import java.io.InputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * @author Kevin Ludwig
 */
class ContentManifest {
    class Header(
        val buildVersion: Int,
        val unknown04: Int,
        val unknown08: Int,
        val unknown0C: Int,
        val unknown10: Int,
        val unknown14: Int,
        val unknown18: Int,
        val dataPatchRecordCount: Int,
        val dataCount: Int,
        val entryPatchRecordCount: Int,
        val entryCount: Int,
        val magic: Int
    )

    data class Entry(
        val index: Int,
        val hashA: Long,
        val hashB: Long
    )

    data class Data(
        val guid: Guid,
        val size: Int,
        val unknown0C: Byte,
        val cKey: Key
    )

    val header: Header
    val entries: List<Entry>
    val data: List<Data>

    constructor(name: String, stream: InputStream) {
        val headerBuffer = Unpooled.wrappedBuffer(stream.readNBytes(12 * 4))
        header = Header(headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE())
        val buffer = Unpooled.wrappedBuffer((if (header.magic ushr 8 == EncryptionMagic) {
            val encryptionProc = checkNotNull(EncryptionProc.byVersionOrNull(header.buildVersion))
            CipherInputStream(stream, Cipher.getInstance("AES/CBC/NoPadding").apply { init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionProc.getKey(headerBuffer, 32), "AES"), IvParameterSpec(encryptionProc.getIv(MessageDigest.getInstance("SHA1").digest(name.toByteArray()), headerBuffer, 16))) })
        } else stream).readAllBytes())
        entries = List(header.entryCount) { Entry(buffer.readIntLE(), buffer.readLongLE(), buffer.readLongLE()) }
        data = List(header.dataCount) { Data(Guid(buffer.readLongLE()), buffer.readIntLE(), buffer.readByte(), Key(ByteArray(16).apply { buffer.readBytes(this) })) }
    }

    companion object {
        const val EncryptionMagic = 0x636D66
    }
}
