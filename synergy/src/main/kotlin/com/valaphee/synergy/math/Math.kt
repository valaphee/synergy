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

package com.valaphee.synergy.math

import com.fasterxml.jackson.core.JsonGenerator
import java.io.IOException

@Throws(IOException::class)
fun JsonGenerator.writeArray(array: FloatArray, offset: Int, length: Int) {
    writeStartArray(array, length)
    var i = offset
    val end = offset + length
    while (i < end) {
        writeNumber(array[i])
        ++i
    }
    writeEndArray()
}
