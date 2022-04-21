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
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Named
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import com.valaphee.synergy.component.Component
import com.valaphee.synergy.component.ComponentService
import com.valaphee.synergy.component.ComponentServiceImpl
import com.valaphee.synergy.config.Config
import com.valaphee.synergy.hid.WindowsHookSubcommand
import com.valaphee.synergy.event.events
import com.valaphee.synergy.proxy.Proxy
import com.valaphee.synergy.proxy.ProxyService
import com.valaphee.synergy.proxy.ProxyServiceImpl
import com.valaphee.synergy.bgs.security.BgsSecurityPatchSubcommand
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
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.io.IoBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.security.Security
import java.util.UUID

suspend fun main(arguments: Array<String>) {
    System.setIn(null)
    System.setOut(IoBuilder.forLogger(LogManager.getRootLogger()).setLevel(Level.INFO).buildPrintStream())
    System.setErr(IoBuilder.forLogger(LogManager.getRootLogger()).setLevel(Level.ERROR).buildPrintStream())

    Security.addProvider(BouncyCastleProvider())

    objectMapper.registerModule(ProtobufModule())

    val injector = Guice.createInjector(SecurityModule(), object : AbstractModule() {
        private val configFile = File(File(System.getProperty("user.home"), ".valaphee/synergy"), "config.json")

        override fun configure() {
            bind(ComponentService::class.java).to(ComponentServiceImpl::class.java)
            bind(ProxyService::class.java).to(ProxyServiceImpl::class.java)
        }

        @Named("config")
        @Provides
        fun configFile() = configFile

        @Singleton
        @Provides
        fun config() = if (configFile.exists()) try {
            objectMapper.readValue(configFile)
        } catch (_: Exception) {
            Config().also { objectMapper.writeValue(configFile, it) }
        } else Config().also { objectMapper.writeValue(configFile, it) }
    })

    val argumentParser = ArgParser("synergy")
    val host by argumentParser.option(ArgType.String, "host", "H", "Host").default("localhost")
    val port by argumentParser.option(ArgType.Int, "port", "p", "Port").default(8080)
    argumentParser.subcommands(WindowsHookSubcommand, BgsSecurityPatchSubcommand().apply { injector.injectMembers(this) })
    argumentParser.parse(arguments)

    val componentService = injector.getInstance(ComponentService::class.java)
    val proxyService = injector.getInstance(ProxyService::class.java)

    embeddedServer(Netty, port, host, emptyList(), { configureBootstrap = { group(bossGroup, workerGroup) } }) {
        install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        install(WebSockets)

        routing {
            post("/event") {
                call.respond(HttpStatusCode.OK)
                events.emit(call.receive())
            }
            /*webSocket("/event") { events.collectLatest { send(objectMapper.writeValueAsString(it)) } }*/

            post("/component") { call.respond(if (componentService.add(call.receive(Component::class))) HttpStatusCode.OK else HttpStatusCode.BadRequest) }
            delete("/component/{id}") { call.respond(componentService.remove(UUID.fromString(call.parameters["id"]))?.let { HttpStatusCode.OK } ?: HttpStatusCode.NotFound) }
            get("/component/") { call.respond(componentService.components) }

            post("/proxy") {
                @Suppress("UNCHECKED_CAST")
                val proxy = call.receive(Proxy::class).apply(injector::injectMembers) as Proxy<Any?>
                call.respond(if (proxyService.add(proxy)) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                if (call.request.queryParameters["autoStart"] == "true") proxy.start()
            }
            delete("/proxy/{id}") { call.respond(proxyService.remove(UUID.fromString(call.parameters["id"]))?.let { HttpStatusCode.OK } ?: HttpStatusCode.NotFound) }
            get("/proxy/") { call.respond(proxyService.proxies) }
            get("/proxy/{id}/start") {
                proxyService.get(UUID.fromString(call.parameters["id"]))?.let {
                    call.respond(HttpStatusCode.OK)
                    it.start()
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            get("/proxy/{id}/stop") {
                proxyService.get(UUID.fromString(call.parameters["id"]))?.let {
                    call.respond(HttpStatusCode.OK)
                    it.stop()
                } ?: call.respond(HttpStatusCode.NotFound)
            }
        }
    }.start()

    componentService.run()
}
