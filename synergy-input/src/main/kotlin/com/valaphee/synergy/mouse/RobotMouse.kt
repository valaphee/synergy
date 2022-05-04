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
import java.awt.Robot
import java.net.URL
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
open class RobotMouse(
    id: UUID = UUID.randomUUID(),
    scripts: List<URL>
) : Mouse(id, scripts) {
    override suspend fun mouseMove(target: Int2) {
        robot.mouseMove(target.x, target.y)
    }

    override fun mouseMoveRaw(move: Int2) = TODO()

    override fun mousePress(button: Int) {
        robot.mousePress(button)
    }

    override fun mouseRelease(button: Int) {
        robot.mouseRelease(button)
    }

    companion object {
        private val robot = Robot()
    }
}
