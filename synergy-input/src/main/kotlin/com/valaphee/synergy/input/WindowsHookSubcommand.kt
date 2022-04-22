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

package com.valaphee.synergy.input

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.valaphee.synergy.keyboard.Key
import com.valaphee.synergy.keyboard.KeyboardEvent
import com.valaphee.synergy.proxy.httpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * @author Kevin Ludwig
 */
object WindowsHookSubcommand : Subcommand("windows-hook", "Windows Hook")/*, CoroutineScope*/ {
    /*override val coroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher() + SupervisorJob()*/

    private val id by option(ArgType.String, "id", null, "Id").default(UUID.randomUUID().toString())
    private val url by option(ArgType.String, "url", null, "Url").default("http://localhost:8080")

    @Volatile private var hookThreadId = 0

    override fun execute() {
        check(hookThreadId == 0)

        /*runBlocking {
            this@EventPumpSubcommand.launch {
                synchronized(httpClient) {
                    runBlocking {
                        httpClient.post("http://$host:$port/event") {
                            contentType(ContentType.Application.Json)
                            setBody(KeyboardEvent(System.currentTimeMillis(), 0, 0, 0))
                        }
                    }
                }
            }.join()
        }*/
        Runtime.getRuntime().addShutdownHook(thread(false) { if (hookThreadId != 0) User32.INSTANCE.PostThreadMessage(hookThreadId, WinUser.WM_QUIT, WinDef.WPARAM(), WinDef.LPARAM()) })

        hookThreadId = Kernel32.INSTANCE.GetCurrentThreadId()
        val hMod = Kernel32.INSTANCE.GetModuleHandle(null)
        val hhkKeyboardLL = User32.INSTANCE.SetWindowsHookEx(User32.WH_KEYBOARD_LL, object : WinUser.LowLevelKeyboardProc {
            private val emitterId = UUID.fromString(id)

            override fun callback(nCode: Int, wParam: WinDef.WPARAM, lParam: WinUser.KBDLLHOOKSTRUCT): WinDef.LRESULT {
                if (nCode == 0) {
                    val now = System.currentTimeMillis()
                    try {
                        Key.byVkCode(lParam.vkCode)?.let {
                            runBlocking {
                                httpClient.post("$url/event") {
                                    contentType(ContentType.Application.Json)
                                    setBody(KeyboardEvent(emitterId, now, it, if (wParam.toInt() == User32.WM_KEYDOWN) KeyboardEvent.Event.Down else 0 or if (wParam.toInt() == User32.WM_KEYUP) KeyboardEvent.Event.Up else 0, if (lParam.flags and (1 shl 5) != 0) KeyboardEvent.Modifier.Alt else 0))
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                return User32.INSTANCE.CallNextHookEx(null, nCode, wParam, WinDef.LPARAM(Pointer.nativeValue(lParam.pointer)))
            }
        }, hMod, 0)
        User32.INSTANCE.GetMessage(WinUser.MSG(), WinDef.HWND(Pointer.NULL), 0, 0)
        User32.INSTANCE.UnhookWindowsHookEx(hhkKeyboardLL)
        hookThreadId = 0

        exitProcess(0)
    }
}
