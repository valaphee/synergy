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
import com.valaphee.synergy.config.Config
import com.valaphee.synergy.proxy.mcbe.pack.McbePackDecryptSubcommand
import com.valaphee.synergy.proxy.mcbe.pack.McbePackEncryptSubcommand
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
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

suspend fun main(arguments: Array<String>) {
    Security.addProvider(BouncyCastleProvider())

    ObjectMapper.registerModule(ProtobufModule())

    val injector = Guice.createInjector(SecurityModule(), object : AbstractModule() {
        private val configFile = File(File(System.getProperty("user.home"), ".valaphee/synergy"), "config.json")

        @Named("config")
        @Provides
        fun configFile() = configFile

        @Singleton
        @Provides
        fun config() = if (configFile.exists()) try {
            ObjectMapper.readValue(configFile)
        } catch (_: Exception) {
            Config().also { ObjectMapper.writeValue(configFile, it) }
        } else Config().also { ObjectMapper.writeValue(configFile, it) }
    })

    val guiceAnnotationIntrospector = GuiceAnnotationIntrospector()
    ObjectMapper.setAnnotationIntrospectors(AnnotationIntrospectorPair(guiceAnnotationIntrospector, ObjectMapper.serializationConfig.annotationIntrospector), AnnotationIntrospectorPair(guiceAnnotationIntrospector, ObjectMapper.deserializationConfig.annotationIntrospector))
    ObjectMapper.injectableValues = GuiceInjectableValues(injector)

    val argumentParser = ArgParser("synergy")
    val host by argumentParser.option(ArgType.String, "host", "H", "Host").default("localhost")
    val port by argumentParser.option(ArgType.Int, "port", "p", "Port").default(8080)
    argumentParser.subcommands(McbePackDecryptSubcommand(), McbePackEncryptSubcommand())
    argumentParser.parse(arguments)

    System.setIn(null)
    System.setOut(IoBuilder.forLogger(LogManager.getRootLogger()).setLevel(Level.INFO).buildPrintStream())
    System.setErr(IoBuilder.forLogger(LogManager.getRootLogger()).setLevel(Level.ERROR).buildPrintStream())

    embeddedServer(Netty, port, host, emptyList(), { /*configureBootstrap = { group(BossGroup, WorkerGroup) }*/ }) {
        install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(ObjectMapper)) }
        install(WebSockets)

        routing {
        }
    }.start()
}
