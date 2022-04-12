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

package com.valaphee.synergy.cheat

import com.valaphee.foundry.math.Float3
import com.valaphee.foundry.math.Float4x4
import com.valaphee.foundry.math.MutableFloat3
import java.awt.Robot
import kotlin.math.max

/**
 * @author Kevin Ludwig
 */
class GCodeMouseCheat(
    private val data: String,
    private val matrix: Float4x4
) : Cheat {
    private lateinit var lines: Iterator<String>
    private lateinit var position: Float3

    override fun enable() {
        lines = data.lines().iterator()
        position = Float3.Zero
    }

    override fun update() {
        if (lines.hasNext()) {
            val command = lines.next().substringBefore(';').split(' ').filter { it.isNotEmpty() }.associate { it.first() to it.substring(1) }
            if (command.isNotEmpty()) {
                command['G']?.let {
                    when (it.toInt()) {
                        1 -> {
                            val direction = MutableFloat3(command['X']?.toFloat() ?: position.x, command['Y']?.toFloat() ?: position.y, command['Z']?.toFloat() ?: position.z).sub(position)
                            val steps = max(direction.length().toInt(), 1)
                            repeat(steps) {
                                val (x, y, _) = matrix.transform((position + (direction * (it / steps.toFloat()))).toMutableFloat3())
                                robot.mouseMove(x.toInt(), y.toInt())
                            }
                            position += direction
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    companion object {
        private val robot = Robot()
    }
}
