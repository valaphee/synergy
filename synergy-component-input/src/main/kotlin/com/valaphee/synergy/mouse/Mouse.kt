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

package com.valaphee.synergy.mouse

import com.valaphee.foundry.math.Int2
import com.valaphee.synergy.component.Component
import kotlinx.coroutines.runBlocking
import org.graalvm.polyglot.HostAccess
import java.net.URL
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
abstract class Mouse(
    id: UUID,
    scripts: List<URL>
) : Component(id, scripts) {
    @HostAccess.Export
    fun mouseMove(x: Int, y: Int) = runBlocking { mouseMove(Int2(x, y)) }

    abstract suspend fun mouseMove(target: Int2)

    @HostAccess.Export
    fun mouseMoveRaw(x: Int, y: Int) = mouseMoveRaw(Int2(x, y))

    abstract fun mouseMoveRaw(move: Int2)

    abstract fun mousePress(button: Int)

    abstract fun mouseRelease(button: Int)
}
