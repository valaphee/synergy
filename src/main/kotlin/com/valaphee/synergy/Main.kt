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

import com.google.inject.Guice
import com.valaphee.synergy.bgs.BgsProxy
import com.valaphee.synergy.bgs.command.BgsPatchSecuritySubcommand
import com.valaphee.synergy.http.HttpProxy
import com.valaphee.synergy.mcbe.McbeProxy
import com.valaphee.synergy.pro.ProProxy
import com.valaphee.synergy.tcp.TcpProxy
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
import kotlin.reflect.KClass

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
            val proxyTypes = mapOf<String, KClass<out Proxy<*>>>(
                "bgs" to BgsProxy::class,
                "http" to HttpProxy::class,
                "mcbe" to McbeProxy::class,
                "pro" to ProProxy::class,
                "tcp" to TcpProxy::class
            )
            val proxies = mutableMapOf<String, Proxy<Any?>>().also { Runtime.getRuntime().addShutdownHook(thread(false) { it.values.forEach { runBlocking { it.stop() } } }) }

            post("/proxy/{type}") {
                proxyTypes[call.parameters["type"]]?.let {
                    val proxy = call.receive(it).apply(injector::injectMembers)
                    @Suppress("UNCHECKED_CAST")
                    if (proxies.putIfAbsent(proxy.id, proxy as Proxy<Any?>) != null) call.respond(HttpStatusCode.BadRequest)
                    if (call.request.queryParameters["autoStart"] == "true") proxy.start()
                    call.respond(HttpStatusCode.OK)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            delete("/proxy/{id}") {
                proxies.remove(call.parameters["id"])?.let {
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
    }.start(true)
}
