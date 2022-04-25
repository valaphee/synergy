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

package com.valaphee.synergy.ngdp.util

import com.google.common.primitives.UnsignedBytes

/**
 * @author Kevin Ludwig
 */
class Key(
    private val bytes: ByteArray
) : Comparable<Key> {
    constructor(key: String) : this(key.asHexStringToByteArray())

    override fun compareTo(other: Key) = comparator.compare(bytes, other.bytes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Key

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode() = bytes.contentHashCode()

    override fun toString() = bytes.toHexString()

    companion object {
        private val comparator = UnsignedBytes.lexicographicalComparator()
    }
}
