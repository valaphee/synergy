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

import com.valaphee.synergy.ngdp.tank.encryption.trg.TrgEncryptionProc96894
import io.netty.buffer.Unpooled
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * @author Kevin Ludwig
 */
class TankResourceGraph {
    class Header(
        val unknown00: Int,
        val version: Int,
        val unknown08: Int,
        val unknown0C: Int,
        val unknown10: Int,
        val unknown14: Int,
        val packageCount: Int,
        val packageBlockSize: Int,
        val skinCount: Int,
        val skinBlockSize: Int,
        val typeBundleIndexCount: Int,
        val typeBundleIndexBlockSize: Int,
        val unknown30: Int,
        val unknown34: Int,
        val graphBlockSize: Int,
        val magic: Int
    )

    private val header: Header

    constructor(name: String, stream: InputStream) {
        val headerBuffer = Unpooled.wrappedBuffer(stream.readNBytes(16 * 4))
        header = Header(headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE(), headerBuffer.readIntLE())
        val buffer = Unpooled.wrappedBuffer((if (header.magic ushr 8 == EncryptedMagic) {
            val encryptionProc = checkNotNull(encryptionProcByVersion[header.version])
            CipherInputStream(stream, Cipher.getInstance("AES/CBC/NoPadding").apply { init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionProc.getKey(header, 32), "AES") as java.security.Key, IvParameterSpec(encryptionProc.getIv(header, name, 16))) })
        } else stream).readAllBytes())
    }

    companion object {
        const val EncryptedMagic = 0x677274
        const val UnencryptedMagic = 0x747267
        private val encryptionProcByVersion = mapOf(
            96894 to TrgEncryptionProc96894
        )
    }
}
