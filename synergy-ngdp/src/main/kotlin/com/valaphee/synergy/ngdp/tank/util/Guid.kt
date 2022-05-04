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

package com.valaphee.synergy.ngdp.tank.util

/**
 * @author Kevin Ludwig
 */
class Guid(
    val engine: Int,
    val type: Int,
    val platform: Int,
    val region: Int,
    val locale: Int,
    val index: Long
) {
    constructor(value: Long) : this((value ushr 60).toInt() and 0xF, (((value ushr 48).toInt() and 0xFFF).flip() shr 20) + 1, (value ushr 44).toInt() and 0xF, (value ushr 39).toInt() and 0x1F, (value ushr 32).toInt() and 0x1F, value and 0xFFFFFFFFL)

    override fun toString() = String.format("%01X:%03X:%01X:%02X:%02X:%08X", engine, type, platform, region, locale, index)

    companion object {
        private fun Int.flip(): Int {
            var _value =   this shr  1 and 0b01010101010101010101010101010101 or (  this and 0b01010101010101010101010101010101 shl  1)
            _value = _value shr  2 and 0b00110011001100110011001100110011 or (_value and 0b00110011001100110011001100110011 shl  2)
            _value = _value shr  4 and 0b00001111000011110000111100001111 or (_value and 0b00001111000011110000111100001111 shl  4)
            _value = _value shr  8 and 0b00000000111111110000000011111111 or (_value and 0b00000000111111110000000011111111 shl  8)
            _value = _value shr 16                                        or (_value                                        shl 16)
            return _value
        }
    }
}
