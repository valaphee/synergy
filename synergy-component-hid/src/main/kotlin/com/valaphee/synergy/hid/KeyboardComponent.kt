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

import com.valaphee.synergy.component.Component
import io.netty.buffer.Unpooled
import org.hid4java.HidDevice
import org.hid4java.HidManager
import java.net.URL
import java.util.UUID
import kotlin.concurrent.thread

/**
 * @author Kevin Ludwig
 */
class KeyboardComponent(
    id: UUID,
    _controller: List<URL>
) : Component(id, _controller) {
    fun keyPress(key: Key) = if (!pressedKeys.contains(key) && pressedKeys.size <= 6) {
        pressedKeys += key
        write()
        true
    } else false

    fun keyRelease(key: Key) = if (pressedKeys.contains(key)) {
        pressedKeys -= key
        write()
        true
    } else false

    companion object {
        private var hidDevice: HidDevice? = null
        private const val path = "\\\\?\\hid#variable_6&col04#1"

        private val pressedKeys = mutableSetOf<Key>()

        init {
            Runtime.getRuntime().addShutdownHook(thread(false) {
                if (pressedKeys.isNotEmpty()) {
                    pressedKeys.clear()
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
                        buffer.writeByte(0x00)
                        buffer.writeByte(0x00)
                        pressedKeys.forEach { buffer.writeByte(it.scanCode) }
                        it.write(buffer.array(), buffer.readableBytes(), 0x04)
                    } finally {
                        buffer?.release()
                    }
                }
            }
        }
    }
}
