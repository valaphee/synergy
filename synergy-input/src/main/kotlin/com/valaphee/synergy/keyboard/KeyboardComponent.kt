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

import com.valaphee.synergy.component.Component
import org.graalvm.polyglot.HostAccess
import java.net.URL
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
abstract class KeyboardComponent(
    id: UUID,
    controller: List<URL>
) : Component(id, controller) {
    @HostAccess.Export
    fun keyPress(key: Int) = keyPress(Key.values()[key])

    abstract fun keyPress(key: Key): Boolean

    @HostAccess.Export
    fun keyRelease(key: Int) = keyRelease(Key.values()[key])

    abstract fun keyRelease(key: Key): Boolean
}
