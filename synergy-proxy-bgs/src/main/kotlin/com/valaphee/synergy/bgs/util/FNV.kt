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

package com.valaphee.synergy.bgs.util

fun String.hashFnva32() = toByteArray().hashFnva32()

fun ByteArray.hashFnva32(): Int {
    var hash = fnv32Init
    forEach {
        hash = hash xor (it.toInt() and 0xFF)
        hash *= fnv32Prime
    }
    return hash
}

private const val fnv32Init = 0x811C9dC5.toInt()
private const val fnv32Prime = 16777619
