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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.util.concurrent.ThreadFactoryBuilder
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
import io.ktor.server.plugins.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueDatagramChannel
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramChannel
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.runBlocking
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.util.Calendar
import java.util.concurrent.ThreadFactory
import kotlin.concurrent.thread
import kotlin.random.asKotlinRandom
import kotlin.reflect.KClass

val underlyingNetworking = if (Epoll.isAvailable()) UnderlyingNetworking.Epoll else if (KQueue.isAvailable()) UnderlyingNetworking.Kqueue else UnderlyingNetworking.Nio
val bossGroup = underlyingNetworking.groupFactory(0, ThreadFactoryBuilder().setNameFormat("boss-%d").build())
val workerGroup = underlyingNetworking.groupFactory(0, ThreadFactoryBuilder().setNameFormat("worker-%d").build())
lateinit var keyStoreFile: File
lateinit var keyStore: KeyStore
internal val objectMapper = jacksonObjectMapper()

fun main(arguments: Array<String>) {
    Security.addProvider(BouncyCastleProvider())

    val path = File(System.getProperty("user.home"), ".valaphee/synergy").also(File::mkdirs)
    keyStoreFile = File(path, "key_store.pfx")
    keyStore = if (!keyStoreFile.exists()) {
        val rootKeyPair = KeyPairGenerator.getInstance("RSA", "BC").apply { initialize(2048) }.generateKeyPair()
        val rootCertificate = JcaX509CertificateConverter().setProvider("BC").getCertificate(JcaX509v3CertificateBuilder(X500Name("CN=Synergy"), BigInteger(SecureRandom().asKotlinRandom().nextBytes(8)), Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time, Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time, X500Name("CN=Synergy"), rootKeyPair.public).apply {
            addExtension(Extension.basicConstraints, true, BasicConstraints(true))
            addExtension(Extension.subjectKeyIdentifier, false, JcaX509ExtensionUtils().createSubjectKeyIdentifier(rootKeyPair.public))
        }.build(JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(rootKeyPair.private)))
        KeyStore.getInstance("PKCS12", "BC").apply {
            load(null, null)
            setKeyEntry("synergy", rootKeyPair.private, null, arrayOf(rootCertificate))
            keyStoreFile.outputStream().use { store(it, "".toCharArray()) }
        }
    } else KeyStore.getInstance("PKCS12", "BC").apply { keyStoreFile.inputStream().use { load(it, "".toCharArray()) } }

    val argumentParser = ArgParser("synergy")
    val host by argumentParser.option(ArgType.String, "host", "H", "Host").default("localhost")
    val port by argumentParser.option(ArgType.Int, "port", "p", "Port").default(8080)
    argumentParser.subcommands(BgsPatchSecuritySubcommand)
    argumentParser.parse(arguments)

    val proxyTypes = mapOf<String, KClass<out Proxy<*>>>(
        "bgs" to BgsProxy::class,
        "http" to HttpProxy::class,
        "mcbe" to McbeProxy::class,
        "pro" to ProProxy::class,
        "tcp" to TcpProxy::class
    )
    val proxies = mutableMapOf<String, Proxy<Any?>>()

    Runtime.getRuntime().addShutdownHook(thread(false) { proxies.values.forEach { runBlocking { it.stop() } } })

    embeddedServer(Netty, port, host) {
        install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }

        routing {
            post("/proxy/{type}") {
                proxyTypes[call.parameters["type"]]?.let {
                    val proxy = call.receive(it)
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
            get("/proxy") { call.respond(proxies.values) }
            get("/proxy/{id}/start") {
                proxies[call.parameters["id"]]?.let {
                    it.start()
                    call.respond(HttpStatusCode.OK)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            post("/proxy/{id}/update") {
                proxies[call.parameters["id"]]?.let {
                    // Use object mapper for correct interpretation of "update" structures, See IpcLocation
                    call.respondText(objectMapper.writeValueAsString(it.update(objectMapper.readValue(call.receiveText(), it.dataType.java))), ContentType.Application.Json)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            get("/proxy/{id}/stop") {
                proxies[call.parameters["id"]]?.let {
                    it.stop()
                    call.respond(HttpStatusCode.OK)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
        }
    }.start(true)
}

enum class UnderlyingNetworking(
    val groupFactory: (Int, ThreadFactory) -> EventLoopGroup,
    val serverSocketChannel: Class<out ServerSocketChannel>,
    val datagramChannel: Class<out DatagramChannel>
) {
    Epoll({ threadCount, threadFactory -> EpollEventLoopGroup(threadCount, threadFactory) }, EpollServerSocketChannel::class.java, EpollDatagramChannel::class.java),
    Kqueue({ threadCount, threadFactory -> KQueueEventLoopGroup(threadCount, threadFactory) }, KQueueServerSocketChannel::class.java, KQueueDatagramChannel::class.java),
    Nio({ threadCount, threadFactory -> NioEventLoopGroup(threadCount, threadFactory) }, NioServerSocketChannel::class.java, NioDatagramChannel::class.java)
}
