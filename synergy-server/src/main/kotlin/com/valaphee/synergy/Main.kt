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

import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
import com.fasterxml.jackson.module.guice.GuiceAnnotationIntrospector
import com.fasterxml.jackson.module.guice.GuiceInjectableValues
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
import com.valaphee.synergy.input.WindowsHookSubcommand
import com.valaphee.synergy.proxy.bgs.security.BgsSecurityPatchSubcommand
import com.valaphee.synergy.proxy.mcbe.pack.McbePackDecryptSubcommand
import com.valaphee.synergy.proxy.mcbe.pack.McbePackEncryptSubcommand
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
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.send
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.flow.collectLatest
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.io.IoBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.security.Security
import java.util.UUID

suspend fun main(arguments: Array<String>) {
    Security.addProvider(BouncyCastleProvider())

    objectMapper.registerModule(ProtobufModule())

    val injector = Guice.createInjector(SecurityModule(), object : AbstractModule() {
        private val configFile = File(File(System.getProperty("user.home"), ".valaphee/synergy"), "config.json")

        override fun configure() {
            bind(ComponentService::class.java).to(ComponentServiceImpl::class.java)
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

    objectMapper.injectableValues = GuiceInjectableValues(injector)
    val guiceAnnotationIntrospector = GuiceAnnotationIntrospector()
    objectMapper.setAnnotationIntrospectors(AnnotationIntrospectorPair(guiceAnnotationIntrospector, objectMapper.serializationConfig.annotationIntrospector), AnnotationIntrospectorPair(guiceAnnotationIntrospector, objectMapper.deserializationConfig.annotationIntrospector))

    val argumentParser = ArgParser("synergy")
    val host by argumentParser.option(ArgType.String, "host", "H", "Host").default("localhost")
    val port by argumentParser.option(ArgType.Int, "port", "p", "Port").default(8080)
    argumentParser.subcommands(BgsSecurityPatchSubcommand().apply { injector.injectMembers(this) }, WindowsHookSubcommand(), McbePackDecryptSubcommand(), McbePackEncryptSubcommand())
    argumentParser.parse(arguments)

    System.setIn(null)
    System.setOut(IoBuilder.forLogger(LogManager.getRootLogger()).setLevel(Level.INFO).buildPrintStream())
    System.setErr(IoBuilder.forLogger(LogManager.getRootLogger()).setLevel(Level.ERROR).buildPrintStream())

    val componentService = injector.getInstance(ComponentService::class.java)

    embeddedServer(Netty, port, host, emptyList(), { configureBootstrap = { group(bossGroup, workerGroup) } }) {
        install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        install(WebSockets)

        routing {
            post("/message") {
                call.respond(HttpStatusCode.OK)
                messages.emit(call.receive())
            }
            webSocket("/message") { messages.collectLatest { send(objectMapper.writeValueAsString(it)) } }

            post("/component") { call.respond(if (componentService.add(call.receive(Component::class))) HttpStatusCode.OK else HttpStatusCode.BadRequest) }
            delete("/component/{id}") { call.respond(componentService.remove(UUID.fromString(call.parameters["id"]))?.let { HttpStatusCode.OK } ?: HttpStatusCode.NotFound) }
            get("/component/") { call.respond(componentService.components) }
        }
    }.start()

    componentService.run()
}
