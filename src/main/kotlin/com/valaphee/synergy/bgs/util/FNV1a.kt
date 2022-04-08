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

package com.valaphee.synergy.bgs

fun String.hashFnv1a() = toByteArray().hashFnv1a()

fun ByteArray.hashFnv1a(): Int {
    var hash = fnv1aInit
    forEach {
        hash = hash xor (it.toInt() and 0xFF)
        hash *= fnv1aPrime
    }
    return hash
}

private const val fnv1aInit = 0x811C9DC5.toInt()
private const val fnv1aPrime = 16777619
