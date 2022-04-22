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

package com.valaphee.synergy.keyboard

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.valaphee.synergy.event.Event
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
@JsonTypeName("keyboard")
class KeyboardEvent(
    emitterId: UUID,
    emittedAt: Long,
    @get:JsonProperty("key") val key: Key,
    @get:JsonProperty("event") val event: Int,
    @get:JsonProperty("modifiers") val modifiers: Int
) : Event(emitterId, emittedAt) {
    @get:JsonIgnore val isPressed get() = (event and Event.Down) != 0
    @get:JsonIgnore val isReleased get() = (event and Event.Up) != 0
    @get:JsonIgnore val isAltDown get() = (modifiers and Modifier.Alt) != 0

    object Event {
        const val Down = 1 shl 0
        const val Up = 1 shl 1
    }

    object Modifier {
        const val Alt = 1 shl 0
    }
}