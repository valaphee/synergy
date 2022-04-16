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

package com.valaphee.synergy.component

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

/**
 * @author Kevin Ludwig
 */
enum class Key(
    val scanCode: Int
) {
    None(0x00),
    A(0x04),
    B(0x05),
    C(0x06),
    D(0x07),
    E(0x08),
    F(0x09),
    G(0x0A),
    H(0x0B),
    I(0x0C),
    J(0x0D),
    K(0x0E),
    L(0x0F),
    M(0x10),
    N(0x11),
    O(0x12),
    P(0x13),
    Q(0x14),
    R(0x15),
    S(0x16),
    T(0x17),
    U(0x18),
    V(0x19),
    W(0x1A),
    X(0x1B),
    Y(0x1C),
    Z(0x1D),
    `1`(0x1E),
    `2`(0x1F),
    `3`(0x20),
    `4`(0x21),
    `5`(0x22),
    `6`(0x23),
    `7`(0x24),
    `8`(0x25),
    `9`(0x26),
    `0`(0x27),
    Enter(0x28),
    Escape(0x29),
    Backspace(0x2A),
    Tab(0x2B),
    Space(0x2C),
    OemMinus(0x2D),
    OemPlus(0x2E),
    Oem4(0x2F),
    Oem6(0x30),
    Oem5(0x31),
    Oem1(0x33),
    Oem7(0x34),
    Oem3(0x35),
    OemComma(0x36),
    OemPeriod(0x37),
    Oem2(0x38),
    Capslock(0x39),
    F1(0x3A),
    F2(0x3B),
    F3(0x3C),
    F4(0x3D),
    F5(0x3E),
    F6(0x3F),
    F7(0x40),
    F8(0x41),
    F9(0x42),
    F10(0x43),
    F11(0x44),
    F12(0x45),
    SysRq(0x46),
    ScrollLock(0x47),
    Pause(0x48),
    Insert(0x49),
    Home(0x4A),
    PageUp(0x4B),
    Delete(0x4C),
    End(0x4D),
    PageDown(0x4E),
    Right(0x4F),
    Left(0x50),
    Down(0x51),
    Up(0x52),
    NumLock(0x53),
    NumPadDivide(0x54),
    NumPadMultiply(0x55),
    NumPadSubtract(0x56),
    NumPadAdd(0x57),
    NumPadEnter(0x58),
    NumPad1(0x59),
    NumPad2(0x5a),
    NumPad3(0x5b),
    NumPad4(0x5c),
    NumPad5(0x5d),
    NumPad6(0x5e),
    NumPad7(0x5f),
    NumPad8(0x60),
    NumPad9(0x61),
    NumPad0(0x62),
    NumPadDecimal(0x63),
    Oem102(0x64),
    LeftControl(0xE0),
    LeftShift(0xE1),
    LeftAlt(0xE2),
    LeftMeta(0xE3),
    RightControl(0xE4),
    RightShift(0xE5),
    RightAlt(0xE6),
    RightMeta(0xE7)
}