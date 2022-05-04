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

import javafx.util.StringConverter

/**
 * @author Kevin Ludwig
 */
object IntStringConverter : StringConverter<Number>() {
    override fun toString(`object`: Number?) = `object`?.toString() ?: ""

    override fun fromString(string: String?) = string?.toIntOrNull()
}
