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

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.inject.Guice
import com.valaphee.synergy.action.Action
import com.valaphee.synergy.event.command.EventPumpSubcommand
import com.valaphee.synergy.event.events
import com.valaphee.synergy.proxy.Proxy
import com.valaphee.synergy.proxy.SecurityModule
import com.valaphee.synergy.proxy.bgs.command.BgsPatchSecuritySubcommand
import com.valaphee.synergy.proxy.bossGroup
import com.valaphee.synergy.proxy.objectMapper
import com.valaphee.synergy.proxy.workerGroup
import com.valaphee.synergy.util.MapProxyObject
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import java.io.File
import java.security.Security
import java.util.UUID
import kotlin.concurrent.thread

val context: Context = Context.newBuilder().allowHostAccess(HostAccess.ALL).allowAllAccess(true).build()

suspend fun main(arguments: Array<String>) {
    Security.addProvider(BouncyCastleProvider())

    val injector = Guice.createInjector(SecurityModule(File(File(System.getProperty("user.home"), ".valaphee/synergy"), "key_store.pfx")))

    context.getBindings("js").putMember("coroutineScope", CoroutineScope(Dispatchers.IO + SupervisorJob()))

    val argumentParser = ArgParser("synergy")
    val host by argumentParser.option(ArgType.String, "host", "H", "Host").default("localhost")
    val port by argumentParser.option(ArgType.Int, "port", "p", "Port").default(8080)
    argumentParser.subcommands(EventPumpSubcommand, injector.getInstance(BgsPatchSecuritySubcommand::class.java))
    argumentParser.parse(arguments)

    val actionFile = File(File(System.getProperty("user.home"), ".valaphee/synergy"), "actions.json")
    val actions = if (actionFile.exists()) try {
        objectMapper.readValue<List<Action>>(actionFile).associateBy { it.id }.toMutableMap()
    } catch (_: Exception) {
        mutableMapOf()
    } else mutableMapOf()

    val proxyFile = File(File(System.getProperty("user.home"), ".valaphee/synergy"), "proxies.json")
    val proxies = (if (proxyFile.exists()) try {
        objectMapper.readValue<List<Proxy<Any?>>>(proxyFile).associateBy { it.id }.toMutableMap()
    } catch (_: Exception) {
        mutableMapOf()
    } else mutableMapOf()).also { Runtime.getRuntime().addShutdownHook(thread(false) { it.values.forEach { runBlocking { it.stop() } } }) }

    embeddedServer(Netty, port, host, emptyList(), { configureBootstrap = { group(bossGroup, workerGroup) } }) {
        install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        install(WebSockets)

        routing {
            post("/event") {
                call.respond(HttpStatusCode.OK)
                events.emit(call.receive())
            }
            webSocket("/event") { events.collectLatest { send(objectMapper.writeValueAsString(it)) } }

            post("/action") {
                @Suppress("UNCHECKED_CAST")
                val action = call.receive(Action::class)
                call.respond(if (actions.putIfAbsent(action.id, action) == null) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                if (call.request.queryParameters["persist"] == "true") {
                    val persistentActions = if (actionFile.exists()) try {
                        objectMapper.readValue<List<Action>>(actionFile).associateBy { it.id }.toMutableMap()
                    } catch (_: Exception) {
                        mutableMapOf()
                    } else mutableMapOf()
                    if (persistentActions.putIfAbsent(action.id, action) == null) objectMapper.writeValue(actionFile, persistentActions.values)
                }
            }
            delete("/action/{id}") {
                actions.remove(UUID.fromString(call.parameters["id"]))?.let {
                    call.respond(HttpStatusCode.OK)
                    if (call.request.queryParameters["persist"] == "true") try {
                        val persistentActions = objectMapper.readValue<List<Action>>(actionFile).associateBy { it.id }.toMutableMap()
                        if (persistentActions.remove(it.id) != null) objectMapper.writeValue(actionFile, persistentActions.values)
                    } catch (_: Exception) {
                    }
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            get("/action/") { call.respond(actions.values) }

            post("/proxy") {
                @Suppress("UNCHECKED_CAST")
                val proxy = call.receive(Proxy::class).apply(injector::injectMembers) as Proxy<Any?>
                call.respond(if (proxies.putIfAbsent(proxy.id, proxy) == null) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                if (call.request.queryParameters["persist"] == "true") {
                    val persistentProxies = if (proxyFile.exists()) try {
                        objectMapper.readValue<List<Proxy<Any?>>>(proxyFile).associateBy {it.id }.toMutableMap()
                    } catch (_: Exception) {
                        mutableMapOf()
                    } else mutableMapOf()
                    if (persistentProxies.putIfAbsent(proxy.id, proxy) == null) objectMapper.writeValue(proxyFile, persistentProxies.values)
                }
                if (call.request.queryParameters["autoStart"] == "true") proxy.start()
            }
            delete("/proxy/{id}") {
                proxies.remove(UUID.fromString(call.parameters["id"]))?.let { proxy ->
                    call.respond(HttpStatusCode.OK)
                    if (call.request.queryParameters["persist"] == "true") try {
                        val persistentProxies = objectMapper.readValue<List<Proxy<Any?>>>(proxyFile).associateBy { it.id }.toMutableMap()
                        if (persistentProxies.remove(proxy.id) != null) objectMapper.writeValue(proxyFile, persistentProxies.values)
                    } catch (_: Exception) {
                    }
                    proxy.stop()
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            get("/proxy/") { call.respond(proxies.values) }
            get("/proxy/{id}/start") {
                proxies[UUID.fromString(call.parameters["id"])]?.let { proxy ->
                    call.respond(HttpStatusCode.OK)
                    proxy.start()
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            post("/proxy/{id}/update") { proxies[UUID.fromString(call.parameters["id"])]?.let { call.respondText(objectMapper.writeValueAsString(it.update(objectMapper.readValue(call.receiveText(), it.dataType.java))), ContentType.Application.Json) } ?: call.respond(HttpStatusCode.NotFound) }
            get("/proxy/{id}/stop") {
                proxies[UUID.fromString(call.parameters["id"])]?.let { proxy ->
                    call.respond(HttpStatusCode.OK)
                    proxy.stop()
                } ?: call.respond(HttpStatusCode.NotFound)
            }
        }
    }.start()

    runBlocking {
        events.collectLatest {
            val eventProxy = MapProxyObject(objectMapper.convertValue(it))
            actions.values.forEach { it.eventHandler.execute(context.asValue(it), eventProxy) }
        }
    }
}
