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

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.math.IntMath.pow
import com.valaphee.foundry.math.Int2
import io.netty.buffer.Unpooled
import kotlinx.coroutines.delay
import org.hid4java.HidDevice
import org.hid4java.HidManager
import java.awt.MouseInfo
import java.net.URL
import java.util.BitSet
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * @author Kevin Ludwig
 */
open class HidMouse(
    id: UUID,
    scripts: List<URL>,
    @get:JsonProperty("sensitivity") val sensitivity: Float,
    @get:JsonProperty("epsilon") val epsilon: Int
) : Mouse(id, scripts) {
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

    override suspend fun mouseMove(target: Int2) {
        var current = MouseInfo.getPointerInfo().location
        while (pow(target.x - current.x, 2) + pow(target.y - current.y, 2) > epsilon) {
            mouseMoveRaw(Int2(target.x - current.x, target.y - current.y))
            delay(1L)
            current = MouseInfo.getPointerInfo().location
        }
    }

    override fun mouseMoveRaw(move: Int2) {
        val moveX = abs(move.x)
        val moveY = abs(move.y)
        val _moveX = if (moveX >= moves.size) moves.size - 1 else moves[moveX]
        val _moveY = if (moveY >= moves.size) moves.size - 1 else moves[moveY]
        write(Int2(if (move.x > 0) _moveX else -_moveX, if (move.y > 0) _moveY else -_moveY))
    }

    override fun mousePress(button: Int) {
        buttons.set(button)
        write(Int2.Zero)
    }

    override fun mouseRelease(button: Int) {
        buttons.clear(button)
        write(Int2.Zero)
    }

    companion object {
        private var hidDevice: HidDevice? = null
        private const val path = "\\\\?\\hid#variable_6&col02#1"

        private val buttons = BitSet()

        init {
            Runtime.getRuntime().addShutdownHook(thread(false) { hidDevice?.close() })

            hidDevice = HidManager.getHidServices().attachedHidDevices.find { it.path.startsWith(path) }?.also { it.open() }
        }

        fun write(move: Int2) {
            hidDevice?.let {
                if (it.isOpen) {
                    val buffer = Unpooled.buffer()
                    try {
                        buffer.writeByte(if (buttons.isEmpty) 0 else buttons.toByteArray()[0].toInt())
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
