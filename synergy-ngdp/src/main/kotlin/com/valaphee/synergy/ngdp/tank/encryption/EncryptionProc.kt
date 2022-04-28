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

package com.valaphee.synergy.ngdp.tank.encryption

import io.netty.buffer.ByteBuf

/**
 * @author Kevin Ludwig
 */
interface EncryptionProc {
    fun getKey(headerBuffer: ByteBuf, length: Int): ByteArray

    fun getIv(nameHash: ByteArray, headerBuffer: ByteBuf, length: Int): ByteArray

    companion object {
        private val byVersion = mapOf(
            96894 to EncryptionProc96894
        )

        fun byVersionOrNull(version: Int) = byVersion[version]
    }
}
