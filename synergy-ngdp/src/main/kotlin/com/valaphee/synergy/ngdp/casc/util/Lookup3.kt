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

package com.valaphee.synergy.ngdp.casc.util

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil

fun ByteBuf.hashLookup3(offset: Int = readerIndex(), length: Int = readableBytes(), init: Pair<Int, Int> = 0 to 0) = ByteBufUtil.getBytes(this, offset, length).hashLookup3(init = init)

fun ByteArray.hashLookup3(offset: Int = 0, length: Int = size, init: Pair<Int, Int> = 0 to 0): Pair<Int, Int> {
    var a = 0xDEADBEEF.toInt() + length + init.first
    var b = a
    var c = a + init.second

    var _offset = offset
    var _length = length
    while (_length > 12) {
        a += this[_offset + 0].toInt()
        a += this[_offset + 1].toInt() shl 8
        a += this[_offset + 2].toInt() shl 16
        a += this[_offset + 3].toInt() shl 24
        b += this[_offset + 4].toInt()
        b += this[_offset + 5].toInt() shl 8
        b += this[_offset + 6].toInt() shl 16
        b += this[_offset + 7].toInt() shl 24
        c += this[_offset + 8].toInt()
        c += this[_offset + 9].toInt() shl 8
        c += this[_offset + 10].toInt() shl 16
        c += this[_offset + 11].toInt() shl 24

        a -= c
        a = a xor rot(c, 4).toInt()
        c += b
        b -= a
        b = b xor rot(a, 6).toInt()
        a += c
        c -= b
        c = c xor rot(b, 8).toInt()
        b += a
        a -= c
        a = a xor rot(c, 16).toInt()
        c += b
        b -= a
        b = b xor rot(a, 19).toInt()
        a += c
        c -= b
        c = c xor rot(b, 4).toInt()
        b += a

        _offset += 12
        _length -= 12
    }

    if (_length > 0) {
        if (_length == 12) c += this[_offset + 11].toInt() shl 24
        if (_length >= 11) c += this[_offset + 10].toInt() shl 16
        if (_length >= 10) c += this[_offset + 9].toInt() shl 8
        if (_length >= 9) c += this[_offset + 8].toInt()
        if (_length >= 8) b += this[_offset + 7].toInt() shl 24
        if (_length >= 7) b += this[_offset + 6].toInt() shl 16
        if (_length >= 6) b += this[_offset + 5].toInt() shl 8
        if (_length >= 5) b += this[_offset + 4].toInt()
        if (_length >= 4) a += this[_offset + 3].toInt() shl 24
        if (_length >= 3) a += this[_offset + 2].toInt() shl 16
        if (_length >= 2) a += this[_offset + 1].toInt() shl 8
        a += this[_offset + 0].toInt()

        c = c xor b
        c -= rot(b, 14).toInt()
        a = a xor c
        a -= rot(c, 11).toInt()
        b = b xor a
        b -= rot(a, 25).toInt()
        c = c xor b
        c -= rot(b, 16).toInt()
        a = a xor c
        a -= rot(c, 4).toInt()
        b = b xor a
        b -= rot(a, 14).toInt()
        c = c xor b
        c -= rot(b, 24).toInt()
    }

    return c to b
}

private fun rot(x: Int, distance: Int) = (x shl distance or (x ushr 32 - distance)).toLong()
