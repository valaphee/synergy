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

package com.valaphee.synergy.keyboard.impl

import com.valaphee.synergy.keyboard.Key
import com.valaphee.synergy.keyboard.Keyboard
import io.netty.buffer.Unpooled
import org.hid4java.HidDevice
import org.hid4java.HidManager
import java.util.BitSet
import java.util.UUID
import kotlin.concurrent.thread

/**
 * @author Kevin Ludwig
 */
class HidKeyboard(
    override val id: UUID = UUID.randomUUID()
) : Keyboard() {
    override fun keyPress(key: Key) = if (!keys.contains(key) && keys.size <= 6) {
        keys += key
        write()
        true
    } else false

    override fun keyRelease(key: Key) = if (keys.contains(key)) {
        keys -= key
        write()
        true
    } else false

    companion object {
        private var hidDevice: HidDevice? = null
        private const val path = "\\\\?\\hid#variable_6&col04#1"

        private val keys = mutableSetOf<Key>()

        init {
            Runtime.getRuntime().addShutdownHook(thread(false) {
                if (keys.isNotEmpty()) {
                    keys.clear()
                    write()
                }
                hidDevice?.close()
            })

            hidDevice = HidManager.getHidServices().attachedHidDevices.find { it.path.startsWith(path) }?.also { it.open() }
        }

        private fun write() {
            hidDevice?.let {
                if (it.isOpen) {
                    val buffer = Unpooled.buffer()
                    try {
                        val modifiers = BitSet()
                        if (keys.contains(Key.LeftControl)) modifiers.set(0)
                        if (keys.contains(Key.LeftShift)) modifiers.set(1)
                        if (keys.contains(Key.LeftAlt)) modifiers.set(2)
                        if (keys.contains(Key.LeftMeta)) modifiers.set(3)
                        if (keys.contains(Key.RightControl)) modifiers.set(4)
                        if (keys.contains(Key.RightShift)) modifiers.set(5)
                        if (keys.contains(Key.RightAlt)) modifiers.set(6)
                        if (keys.contains(Key.RightMeta)) modifiers.set(7)
                        buffer.writeByte(if (modifiers.isEmpty) 0 else modifiers.toByteArray()[0].toInt())
                        buffer.writeByte(0x00)
                        keys.forEach { buffer.writeByte(it.scanCode) }
                        it.write(buffer.array(), buffer.readableBytes(), 0x04)
                    } finally {
                        buffer?.release()
                    }
                }
            }
        }
    }
}
