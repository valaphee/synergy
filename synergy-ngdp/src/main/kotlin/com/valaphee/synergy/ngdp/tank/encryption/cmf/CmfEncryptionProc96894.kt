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

package com.valaphee.synergy.ngdp.tank.encryption.cmf

import com.valaphee.synergy.ngdp.tank.ContentManifest
import com.valaphee.synergy.ngdp.tank.encryption.EncryptionProc
import com.valaphee.synergy.ngdp.tank.encryption.EncryptionProc96894
import com.valaphee.synergy.ngdp.util.fmod
import java.security.MessageDigest

/**
 * @author Kevin Ludwig
 */
object CmfEncryptionProc96894 : EncryptionProc<ContentManifest.Header> {
    override fun getKey(header: ContentManifest.Header, length: Int): ByteArray {
        var keyIndex = EncryptionProc96894.KeyTable[length + 256].toInt()
        return ByteArray(length) {
            EncryptionProc96894.KeyTable[keyIndex fmod 512].also {
                keyIndex += 3
            }
        }
    }

    override fun getIv(header: ContentManifest.Header, name: String, length: Int): ByteArray {
        val nameHash = MessageDigest.getInstance("SHA1").digest(name.toByteArray())
        var keyIndex = length * header.version
        val _keyIndex = keyIndex
        return ByteArray(length) {
            (EncryptionProc96894.KeyTable[keyIndex fmod 512].also {
                keyIndex += if ((nameHash[3].toInt() and 1) != 0) 37 else _keyIndex fmod 61
            }.toInt() xor nameHash[(keyIndex - it) fmod 20].toInt()).toByte()
        }
    }
}
