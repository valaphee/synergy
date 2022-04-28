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
    val usage: Int,
    val type: Int,
    val platform: Int,
    val region: Int,
    val locale: Int,
    val id: Int
) {
    constructor(value: Long) : this((value ushr 60).toInt() and 0xF, (value ushr 48).toInt() and 0xFFF, (value ushr 44).toInt() and 0xF, (value ushr 39).toInt() and 0x1F, (value ushr 34).toInt() and 0x1F, value.toInt())
}
