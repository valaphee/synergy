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

package com.valaphee.synergy.proxy.util

import java.net.InetSocketAddress
import java.util.regex.Pattern

fun address(value: CharSequence, defaultPort: Int): InetSocketAddress? {
    AddressParser.parsers.forEach { it.to(value, defaultPort)?.let { return it } }
    return null
}

/**
 * @author Kevin Ludwig
 */
private interface AddressParser {
    fun to(value: CharSequence, defaultPort: Int): InetSocketAddress?

    companion object {
        private fun parse(string: CharSequence, defaultPort: Int, pattern: Pattern, patternHostIndex: Int, patternPortIndex: Int): InetSocketAddress? {
            val matcher = pattern.matcher(string)
            if (matcher.matches() && matcher.reset().find()) {
                val portString = matcher.group(patternPortIndex)
                var port = defaultPort
                try {
                    if (portString != null && portString.isNotEmpty()) port = portString.toInt()
                } catch (_: NumberFormatException) {
                }
                return InetSocketAddress(matcher.group(patternHostIndex), port)
            }
            return null
        }

        private val hostnamePattern = Pattern.compile("([a-zA-Z][\\w-]*[\\w]*(\\.[a-zA-Z][\\w-]*[\\w]*)*)(:(6553[0-5]|6(55[012]|(5[0-4]|[0-4]\\d)\\d)\\d|[1-5]?\\d{1,4}))?")
        private val v4AddressPattern = Pattern.compile("(((25[0-5]|(2[0-4]|1\\d|[1-9]?)\\d)(\\.|\\b)){4}(?<!\\.))(:(6553[0-5]|6(55[012]|(5[0-4]|[0-4]\\d)\\d)\\d|[1-5]?\\d{1,4}))?")
        private val v6AddressPattern = Pattern.compile("((((?=(?>.*?::)(?!.*::)))(::)?([0-9a-f]{1,4}::?){0,5}|([0-9a-f]{1,4}:){6})(((25[0-5]|(2[0-4]|1[0-9]|[1-9])?[0-9])(\\.|\\b)){4}|\\3([0-9a-f]{1,4}(::?|\\b)){0,2}|[0-9a-f]{1,4}:[0-9a-f]{1,4})(?<![^:]:)(?<!\\.))(?:([#.])(6553[0-5]|6(55[012]|(5[0-4]|[0-4]\\d)\\d)\\d|[1-5]?\\d{1,4}))?$")
        private val bracketV6AddressPattern = Pattern.compile("\\[((((?=(?>.*?::)(?!.*::)))(::)?([0-9a-f]{1,4}::?){0,5}|([0-9a-f]{1,4}:){6})(((25[0-5]|(2[0-4]|1[0-9]|[1-9])?[0-9])(\\.|\\b)){4}|\\3([0-9a-f]{1,4}(::?|\\b)){0,2}|[0-9a-f]{1,4}:[0-9a-f]{1,4})(?<![^:]:)(?<!\\.))](:(6553[0-5]|6(55[012]|(5[0-4]|[0-4]\\d)\\d)\\d|[1-5]?\\d{1,4}))?$")
        internal val parsers = listOf(
            object : AddressParser {
                override fun to(value: CharSequence, defaultPort: Int) = parse(value, defaultPort, hostnamePattern, 1, 5)
            },
            object : AddressParser {
                override fun to(value: CharSequence, defaultPort: Int) = parse(value, defaultPort, v4AddressPattern, 1, 7)
            },
            object : AddressParser {
                override fun to(value: CharSequence, defaultPort: Int) = parse(value, defaultPort, v6AddressPattern, 1, 15)
            },
            object : AddressParser {
                override fun to(value: CharSequence, defaultPort: Int) = parse(value, defaultPort, bracketV6AddressPattern, 1, 15)
            }
        )
    }
}
