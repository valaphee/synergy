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

package com.valaphee.synergy.util

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyObject

/**
 * @author Kevin Ludwig
 */
class MapProxyObject(
    private val map: MutableMap<String, Any>
) : ProxyObject {
    override fun getMember(key: String): Any? {
        val value = map[key]
        @Suppress("UNCHECKED_CAST")
        return if (value is MutableMap<*, *>) MapProxyObject(value as MutableMap<String, Any>) else value
    }

    override fun getMemberKeys() = object : ProxyArray {
        private val keys = map.keys.toTypedArray()

        override fun set(index: Long, value: Value) = throw UnsupportedOperationException()

        override fun getSize() = keys.size.toLong()

        override fun get(index: Long): Any {
            if (index < 0 || index > Int.MAX_VALUE) throw ArrayIndexOutOfBoundsException()
            return keys[index.toInt()]
        }
    }

    override fun hasMember(key: String) = map.containsKey(key)

    override fun putMember(key: String, value: Value) {
        map[key] = if (value.isHostObject) value.asHostObject() else value
    }

    override fun removeMember(key: String) = if (map.containsKey(key)) {
        map.remove(key)
        true
    } else false
}
