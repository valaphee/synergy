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

package com.valaphee.synergy.hid

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.math.IntMath.pow
import com.valaphee.foundry.math.Int2
import com.valaphee.synergy.component.Component
import io.netty.buffer.Unpooled
import kotlinx.coroutines.delay
import org.hid4java.HidDevice
import org.hid4java.HidManager
import java.awt.MouseInfo
import java.net.URL
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * @author Kevin Ludwig
 */
open class MouseComponent(
    id: UUID,
    _controller: List<URL>,
    @get:JsonProperty("sensitivity") val sensitivity: Float,
    @get:JsonProperty("epsilon") val epsilon: Int
) : Component(id, _controller) {
    private val moves: IntArray

    init {
        val moves = mutableListOf<Int>()
        var moveIndex = 0
        repeat(Byte.MAX_VALUE.toInt()) {
            val move = (it * sensitivity).toInt()
            while (moveIndex <= move) {
                moves += move
                moveIndex++
            }
        }
        this.moves = moves.toIntArray()
    }

    suspend fun mouseMove(point: Int2) {
        var currentPoint = MouseInfo.getPointerInfo().location
        while (pow(point.x - currentPoint.x, 2) + pow(point.y - currentPoint.y, 2) > epsilon) {
            mouseMoveRaw(Int2(point.x - currentPoint.x, point.y - currentPoint.y))
            delay(1L)
            currentPoint = MouseInfo.getPointerInfo().location
        }
    }

    fun mouseMoveRaw(move: Int2) {
        val _moveX = abs(move.x)
        val _moveY = abs(move.y)
        val moveX = if (_moveX >= moves.size) moves.size - 1 else moves[_moveX]
        val moveY = if (_moveY >= moves.size) moves.size - 1 else moves[_moveY]
        write(Int2(if (move.x > 0) moveX else -moveX, if (move.y > 0) moveY else -moveY))
    }

    companion object {
        private var hidDevice: HidDevice? = null
        private const val path = "\\\\?\\hid#variable_6&col02#1"

        init {
            Runtime.getRuntime().addShutdownHook(thread(false) {
                hidDevice?.close()
            })

            hidDevice = HidManager.getHidServices().attachedHidDevices.find { it.path.startsWith(path) }?.also { it.open() }
        }

        fun write(move: Int2) {
            hidDevice?.let {
                if (it.isOpen) {
                    val buffer = Unpooled.buffer()
                    try {
                        buffer.writeByte(0x00)
                        buffer.writeByte(move.x)
                        buffer.writeByte(move.y)
                        it.write(buffer.array(), buffer.readableBytes(), 0x02)
                    } finally {
                        buffer?.release()
                    }
                }
            }
        }
    }
}
