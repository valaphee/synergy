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
import com.valaphee.synergy.component.Component
import com.valaphee.synergy.event.events
import com.valaphee.synergy.proxy.Proxy
import com.valaphee.synergy.proxy.bossGroup
import com.valaphee.synergy.proxy.objectMapper
import com.valaphee.synergy.proxy.workerGroup
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.io.IoBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.io.File
import java.security.Security
import java.util.UUID
import kotlin.concurrent.thread

suspend fun main(arguments: Array<String>) {
    System.setIn(null)
    System.setOut(IoBuilder.forLogger(LogManager.getRootLogger()).setLevel(Level.INFO).buildPrintStream())
    System.setErr(IoBuilder.forLogger(LogManager.getRootLogger()).setLevel(Level.ERROR).buildPrintStream())

    Security.addProvider(BouncyCastleProvider())

    val injector = Guice.createInjector(SecurityModule(File(File(System.getProperty("user.home"), ".valaphee/synergy"), "key_store.pfx")))

    val argumentParser = ArgParser("synergy")
    val host by argumentParser.option(ArgType.String, "host", "H", "Host").default("localhost")
    val port by argumentParser.option(ArgType.Int, "port", "p", "Port").default(8080)
    argumentParser.subcommands()
    argumentParser.parse(arguments)

    val componentFile = File(File(System.getProperty("user.home"), ".valaphee/synergy"), "components.json")
    val components = if (componentFile.exists()) try {
        objectMapper.readValue<List<Component>>(componentFile).associate { it.id to (it to it.controller.map { Context.create().eval(Source.create("js", it.readText())) }) }.toMutableMap()
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
            post("/component") {
                val component = call.receive(Component::class)
                call.respond(if (components.putIfAbsent(component.id, component to component.controller.map { Context.create().eval(Source.create("js", it.readText())) }) == null) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                if (call.request.queryParameters["persist"] == "true") {
                    val persistentActions = if (componentFile.exists()) try {
                        objectMapper.readValue<List<Component>>(componentFile).associateBy { it.id }.toMutableMap()
                    } catch (_: Exception) {
                        mutableMapOf()
                    } else mutableMapOf()
                    if (persistentActions.putIfAbsent(component.id, component) == null) objectMapper.writeValue(componentFile, persistentActions.values)
                }
            }
            delete("/component/{id}") {
                components.remove(UUID.fromString(call.parameters["id"]))?.let {
                    call.respond(HttpStatusCode.OK)
                    if (call.request.queryParameters["persist"] == "true") try {
                        val persistentControls = objectMapper.readValue<List<Component>>(componentFile).associateBy { it.id }.toMutableMap()
                        if (persistentControls.remove(it.first.id) != null) objectMapper.writeValue(componentFile, persistentControls.values)
                    } catch (_: Exception) {
                    }
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            get("/component/") { call.respond(components.values.map { it.first }) }
            post("/proxy") {
                @Suppress("UNCHECKED_CAST")
                val proxy = call.receive(Proxy::class).apply(injector::injectMembers) as Proxy<Any?>
                call.respond(if (proxies.putIfAbsent(proxy.id, proxy) == null) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                if (call.request.queryParameters["persist"] == "true") {
                    val persistentProxies = if (proxyFile.exists()) try {
                        objectMapper.readValue<List<Proxy<Any?>>>(proxyFile).associateBy { it.id }.toMutableMap()
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

    events.collectLatest {
        val eventProxy = MapProxyObject(objectMapper.convertValue(it))
        components.values.forEach { it.second.forEach { it.execute(it, eventProxy) } }
    }
}
