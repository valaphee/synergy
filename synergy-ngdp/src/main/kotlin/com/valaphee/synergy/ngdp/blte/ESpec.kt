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

package com.valaphee.synergy.ngdp.blte

import com.valaphee.synergy.ngdp.util.asHexStringToByteArray

/**
 * Encoded specification
 *
 * @author Kevin Ludwig
 */
interface ESpec {
    class Block(
        val chunks: List<Chunk>
    ) : ESpec {
        data class Chunk(
            val size: Int,
            val count: Int,
            val eSpec: ESpec
        )
    }

    object None : ESpec

    class Zip(
        val level: Int,
        val bits: Int
    ) : ESpec

    class Encrypt(
        val key: ByteArray,
        val iv: ByteArray,
        val eSpec: ESpec
    ) : ESpec

    companion object {
        fun String.toESpec(): ESpec {
            val data = if (indexOf(':') == 1) {
                if (indexOf('{') == 2) {
                    check(lastIndexOf('}') == length - 1)
                    val data = mutableListOf<String>()
                    var i = 3
                    var depth = 0
                    var prevData = 3
                    var nextData: Int
                    while (i < length - 1) {
                        when (this[i]) {
                            ',' -> if (depth == 0) {
                                nextData = i
                                data += substring(prevData, nextData)
                                prevData = nextData + 1
                            }
                            '{' -> depth++
                            '}' -> depth--
                        }
                        i++
                    }
                    data += substring(prevData, length - 1)
                    data
                } else listOf(substring(2, length))
            } else {
                check(length - 1 == 0)
                emptyList()
            }

            return when (this[0]) {
                'n' -> None
                'z' -> Zip(data.getOrNull(0)?.toInt() ?: 9, data.getOrNull(1)?.toInt() ?: 15)
                'e' -> Encrypt(data[0].asHexStringToByteArray(), data[1].asHexStringToByteArray(), data[2].toESpec())
                'b' -> Block(data.map {
                    val (sizeAndCount, eSpec) = it.split('=', limit = 2)
                    val _sizeAndCount = sizeAndCount.split('*', limit = 2)
                    val _size = _sizeAndCount[0]
                    val _count = _sizeAndCount.getOrNull(1)


                    Block.Chunk(when (_size.lastOrNull()) {
                        'K' -> _size.substring(0, _size.length - 1).toInt() * 1024
                        'M' -> _size.substring(0, _size.length - 1).toInt() * 1024 * 1024
                        null -> -1
                        else -> _size.toInt()
                    }, if (_count.isNullOrEmpty()) -1 else _count.toInt(), eSpec.toESpec())
                })
                else -> TODO("")
            }
        }
    }
}
