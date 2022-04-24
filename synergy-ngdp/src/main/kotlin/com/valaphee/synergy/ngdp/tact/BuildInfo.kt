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

package com.valaphee.synergy.ngdp.tact

/**
 * @author Kevin Ludwig
 */
class BuildInfo {
    class Key(
        val name: String,
        val type: String,
        val length: Int
    )

    private val keys = mutableListOf<Key>()
    private val _entries = mutableListOf<Map<String, String>>()
    val entries: List<Map<String, String>> get() = _entries

    constructor(buildInfo: String) {
        buildInfo.lines().forEach {
            val row = it.split('|')
            if (keys.isEmpty()) keys += row.map {
                val (name, typeAndLength) = it.split('!', limit = 2)
                val (type, length) = typeAndLength.split(':', limit = 2)
                Key(name, type, length.toInt())
            } else if (row.size == keys.size) _entries += row.mapIndexed { i, cell -> keys[i].name to cell }.toMap()
        }
    }

    operator fun get(index: Int) = entries[index]
}
