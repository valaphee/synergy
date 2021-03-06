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

package com.valaphee.synergy.ngdp.tank.data

import com.valaphee.synergy.ngdp.tank.Data
import com.valaphee.synergy.ngdp.tank.DataReader
import io.netty.buffer.Unpooled

/**
 * @author Kevin Ludwig
 */
class teTexturePayload(
    val mipMapCount: UInt,
    val surfaceCount: UInt,
    val dataSize: UInt,
    val headerSize: UInt
) : Data

/**
 * @author Kevin Ludwig
 */
object teTexturePayloadReader : DataReader {
    override fun read(bytes: ByteArray): teTexturePayload {
        val buffer = Unpooled.wrappedBuffer(bytes)
        val mipMapCount = buffer.readIntLE().toUInt()
        val surfaceCount = buffer.readIntLE().toUInt()
        val dataSize = buffer.readIntLE().toUInt()
        val headerSize = buffer.readIntLE().toUInt()
        return teTexturePayload(mipMapCount, surfaceCount, dataSize, headerSize)
    }
}
