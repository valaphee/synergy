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

package com.valaphee.synergy.ngdp

/**
 * @author Kevin Ludwig
 */
class Config {
    private val _entries = mutableMapOf<String, List<String>>()
    val entries: Map<String, List<String>> get() = _entries

    constructor(config: String) {
        config.lines().forEach {
            val row = it.split('#', limit = 2).first().split('=', limit = 2)
            if (row.size == 2) _entries[row[0].trim()] = row[1].trim().split(' ')
        }
    }

    operator fun get(key: String) = _entries[key]
}
