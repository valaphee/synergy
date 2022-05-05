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
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil

/**
 * @author Kevin Ludwig
 */
class Data004Texture(
    val flags: UShort,
    val mipMapCount: UByte,
    val format: Format,
    val surfaceCount: UByte,
    val usage: UByte,
    val payloadCount: UByte,
    val unknown07: UByte,
    val width: UShort,
    val height: UShort,
    val dataSize: UInt,
    val unknown10: ULong,
    val unknown18: ULong,
    val data: ByteArray
) : Data {
    enum class Format {
        Unknown,
        R32G32B32A32Typeless,
        R32G32B32A32Float,
        R32G32B32A32UInt,
        R32G32B32A32SInt,
        R32G32B32Typeless,
        R32G32B32Float,
        R32G32B32UInt,
        R32G32B32SInt,
        R16G16B16A16Typeless,
        R16G16B16A16Float,
        R16G16B16A16UNorm,
        R16G16B16A16UInt,
        R16G16B16A16SNorm,
        R16G16B16A16SInt,
        R32G32Typeless,
        R32G32Float,
        R32G32UInt,
        R32G32SInt,
        R32G8X24Typeless,
        D32FloatS8X24UInt,
        R32FloatX8X24Typeless,
        X32TypelessG8X24UInt,
        R10G10B10A2Typeless,
        R10G10B10A2UNorm,
        R10G10B10A2UInt,
        R11G11B10Float,
        R8G8B8A8Typeless,
        R8G8B8A8UNorm,
        R8G8B8A8UNormSrgb,
        R8G8B8A8UInt,
        R8G8B8A8SNorm,
        R8G8B8A8SInt,
        R16G16Typeless,
        R16G16Float,
        R16G16UNorm,
        R16G16UInt,
        R16G16SNorm,
        R16G16SInt,
        R32Typeless,
        D32Float,
        R32Float,
        R32UInt,
        R32SInt,
        R24G8Typeless,
        D24UNormS8UInt,
        R24UNormX8Typeless,
        X24TypelessG8UInt,
        R8G8Typeless,
        R8G8UNorm,
        R8G8UInt,
        R8G8SNorm,
        R8G8SInt,
        R16Typeless,
        R16Float,
        D16UNorm,
        R16UNorm,
        R16UInt,
        R16SNorm,
        R16SInt,
        R8Typeless,
        R8UNorm,
        R8UInt,
        R8SNorm,
        R8SInt,
        A8UNorm,
        R1UNorm,
        R9G9B9E5SharedExp,
        R8G8B8G8UNorm,
        G8R8G8B8UNorm,
        BC1Typeless,
        BC1UNorm,
        BC1UNormSrgb,
        BC2Typeless,
        BC2UNorm,
        BC2UNormSrgb,
        BC3Typeless,
        BC3UNorm,
        BC3UNormSrgb,
        BC4Typeless,
        BC4UNorm,
        BC4SNorm,
        BC5Typeless,
        BC5UNorm,
        BC5SNorm,
        B5G6R5UNorm,
        B5G5R5A1UNorm,
        B8G8R8A8UNorm,
        B8G8R8X8UNorm,
        R10G10B10XrBiasA2UNorm,
        B8G8R8A8Typeless,
        B8G8R8A8UNormSrgb,
        B8G8R8X8Typeless,
        B8G8R8X8UNormSrgb,
        BC6HTypeless,
        BC6HUF16,
        BC6HSF16,
        BC7Typeless,
        BC7UNorm,
        BC7UNormSrgb,
        AYUV,
        Y410,
        Y416,
        NV12,
        P010,
        P016,
        `420Opaque`,
        YUY2,
        Y210,
        Y216,
        NV11,
        AI44,
        IA44,
        P8,
        A8P8,
        B4G4R4A4UNorm
    }
}

/**
 * @author Kevin Ludwig
 */
object Data004TextureReader : DataReader {
    override fun read(buffer: ByteBuf): Data004Texture {
        val flags = buffer.readShortLE().toUShort()
        val mipMapCount = buffer.readByte().toUByte()
        val format = Data004Texture.Format.values()[buffer.readByte().toUByte().toInt()]
        val surfaceCount = buffer.readByte().toUByte()
        val usage = buffer.readByte().toUByte()
        val payloadCount = buffer.readByte().toUByte()
        val unknown07 = buffer.readByte().toUByte()
        val width = buffer.readShortLE().toUShort()
        val height = buffer.readShortLE().toUShort()
        val dataSize = buffer.readIntLE().toUInt()
        val unknown10 = buffer.readLongLE().toULong()
        val unknown18 = buffer.readLongLE().toULong()
        val data = ByteBufUtil.getBytes(buffer.readBytes(dataSize.toInt()))
        return Data004Texture(flags, mipMapCount, format, surfaceCount, usage, payloadCount, unknown07, width, height, dataSize, unknown10, unknown18, data)
    }
}
