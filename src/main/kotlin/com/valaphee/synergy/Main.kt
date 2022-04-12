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

package com.valaphee.synergy

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.inject.Guice
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.valaphee.synergy.proxy.Proxy
import com.valaphee.synergy.proxy.SecurityModule
import com.valaphee.synergy.proxy.bgs.command.BgsPatchSecuritySubcommand
import com.valaphee.synergy.proxy.objectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.send
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.security.Security
import kotlin.concurrent.thread

@Volatile
private var hookThreadId = 0

fun main(arguments: Array<String>) {
    Security.addProvider(BouncyCastleProvider())

    val injector = Guice.createInjector(SecurityModule(File(File(System.getProperty("user.home"), ".valaphee/synergy"), "key_store.pfx")))

    val argumentParser = ArgParser("synergy")
    val host by argumentParser.option(ArgType.String, "host", "H", "Host").default("localhost")
    val port by argumentParser.option(ArgType.Int, "port", "p", "Port").default(8080)
    argumentParser.subcommands(injector.getInstance(BgsPatchSecuritySubcommand::class.java))
    argumentParser.parse(arguments)

    embeddedServer(Netty, port, host) {
        install(io.ktor.server.plugins.ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        install(WebSockets)

        routing {
            val proxyFile = File(File(System.getProperty("user.home"), ".valaphee/synergy"), "proxies.json")
            val proxies = (if (proxyFile.exists()) try {
                objectMapper.readValue<List<Proxy<Any?>>>(proxyFile).associateBy { it.id }.toMutableMap()
            } catch (_: Exception) {
                mutableMapOf()
            } else mutableMapOf()).also { Runtime.getRuntime().addShutdownHook(thread(false) { it.values.forEach { runBlocking { it.stop() } } }) }

            post("/proxy") {
                val proxy = call.receive(Proxy::class).apply(injector::injectMembers)
                @Suppress("UNCHECKED_CAST")
                if (proxies.putIfAbsent(proxy.id, proxy as Proxy<Any?>) != null) call.respond(HttpStatusCode.BadRequest)
                if (call.request.queryParameters["persist"] == "true") objectMapper.writeValue(proxyFile, proxies.values)
                if (call.request.queryParameters["autoStart"] == "true") proxy.start()
                call.respond(HttpStatusCode.OK)
            }
            delete("/proxy/{id}") {
                proxies.remove(call.parameters["id"])?.let {
                    if (call.request.queryParameters["persist"] == "true") objectMapper.writeValue(proxyFile, proxies.values)
                    it.stop()
                    call.respond(HttpStatusCode.OK)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            get("/proxy/") { call.respond(proxies.values) }
            get("/proxy/{id}/start") {
                proxies[call.parameters["id"]]?.let {
                    it.start()
                    call.respond(HttpStatusCode.OK)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            post("/proxy/{id}/update") { proxies[call.parameters["id"]]?.let { call.respondText(objectMapper.writeValueAsString(it.update(objectMapper.readValue(call.receiveText(), it.dataType.java))), ContentType.Application.Json) } ?: call.respond(HttpStatusCode.NotFound) }
            get("/proxy/{id}/stop") {
                proxies[call.parameters["id"]]?.let {
                    it.stop()
                    call.respond(HttpStatusCode.OK)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            webSocket("/event") { events.collectLatest { send(objectMapper.writeValueAsString(it)) } }
        }
    }.start()

    thread {
        hookThreadId = Kernel32.INSTANCE.GetCurrentThreadId()

        val hMod = Kernel32.INSTANCE.GetModuleHandle(null)
        val hhkKeyboardLL = User32.INSTANCE.SetWindowsHookEx(User32.WH_KEYBOARD_LL, object : WinUser.LowLevelKeyboardProc {
            override fun callback(nCode: Int, wParam: WinDef.WPARAM, lParam: WinUser.KBDLLHOOKSTRUCT): WinDef.LRESULT {
                if (nCode == 0) runBlocking { events.emit(KeyboardEvent(System.currentTimeMillis(), lParam.vkCode, if (wParam.toInt() == User32.WM_KEYDOWN) KeyboardEvent.Event.Down else 0 or if (wParam.toInt() == User32.WM_KEYUP) KeyboardEvent.Event.Up else 0, if (lParam.flags and (1 shl 5) != 0) KeyboardEvent.Modifier.Alt else 0)) }
                return User32.INSTANCE.CallNextHookEx(null, nCode, wParam, WinDef.LPARAM(Pointer.nativeValue(lParam.pointer)))
            }
        }, hMod, 0)
        /*val hhkMouseLL = User32.INSTANCE.SetWindowsHookEx(User32.WH_MOUSE_LL, object : LowLevelMouseProc {
            override fun callback(nCode: Int, wParam: WinDef.WPARAM, lParam: WinUser.MSLLHOOKSTRUCT): WinDef.LRESULT {
                if (nCode == 0)
                return User32.INSTANCE.CallNextHookEx(null, nCode, wParam, WinDef.LPARAM(Pointer.nativeValue(lParam.pointer)))
            }
        }, hMod, 0)*/
        User32.INSTANCE.GetMessage(WinUser.MSG(), WinDef.HWND(Pointer.NULL), 0, 0)
        User32.INSTANCE.UnhookWindowsHookEx(hhkKeyboardLL)
        /*User32.INSTANCE.UnhookWindowsHookEx(hhkMouseLL)*/

        hookThreadId = 0
    }

    Runtime.getRuntime().addShutdownHook(thread(false) { if (hookThreadId != 0) User32.INSTANCE.PostThreadMessage(hookThreadId, WinUser.WM_QUIT, WinDef.WPARAM(), WinDef.LPARAM()) })
}

interface LowLevelKeyboardProc : WinUser.HOOKPROC {
    fun callback(nCode: Int, wParam: WinDef.WPARAM, lParam: WinUser.KBDLLHOOKSTRUCT): WinDef.LRESULT
}

/*interface LowLevelMouseProc : WinUser.HOOKPROC {
    fun callback(nCode: Int, wParam: WinDef.WPARAM, lParam: WinUser.MSLLHOOKSTRUCT): WinDef.LRESULT
}*/
