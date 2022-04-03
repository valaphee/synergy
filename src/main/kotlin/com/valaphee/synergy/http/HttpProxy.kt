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

package com.valaphee.synergy.http

import com.fasterxml.jackson.annotation.JsonProperty
import com.valaphee.synergy.TransparentProxy
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.origin
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import io.ktor.util.filter
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyAndClose
import java.net.InetAddress
import java.net.InetSocketAddress
import javax.net.SocketFactory

/**
 * @author Kevin Ludwig
 */
class HttpProxy(
    @JsonProperty("id") id: String,
    @JsonProperty("host") host: String,
    @JsonProperty("port") port: Int,
    @JsonProperty("interface_host") interfaceHost: String,
    @JsonProperty("interface_port") interfacePort: Int
) : TransparentProxy(id, host, port, interfaceHost, interfacePort) {
    private var applicationEngine: ApplicationEngine? = null

    override suspend fun start() {
        if (applicationEngine == null) {
            super.start()

            applicationEngine = embeddedServer(Netty, applicationEngineEnvironment {
                connector {
                    host = this@HttpProxy.host
                    port = this@HttpProxy.port
                }

                module {
                    val httpClient = HttpClient(OkHttp) {
                        engine {
                            config {
                                socketFactory(object : SocketFactory() {
                                    private val system = getDefault()

                                    override fun createSocket() = system.createSocket().apply { bind(InetSocketAddress(InetAddress.getByName(interfaceHost), interfacePort)) }

                                    override fun createSocket(host: String, port: Int) = throw NotImplementedError()

                                    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int) = throw NotImplementedError()

                                    override fun createSocket(host: InetAddress, port: Int) = throw NotImplementedError()

                                    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int) = throw NotImplementedError()
                                })
                            }
                        }
                        expectSuccess = false
                    }
                    intercept(ApplicationCallPipeline.Call) {
                        val httpResponse = httpClient.request("${call.request.origin.scheme}://${call.request.origin.host}:${call.request.origin.port}${call.request.origin.uri}") {
                            method = call.request.httpMethod
                            setBody(object : OutgoingContent.WriteChannelContent() {
                                override val headers get() = call.request.headers

                                override suspend fun writeTo(channel: ByteWriteChannel) {
                                    call.request.receiveChannel().copyAndClose(channel)
                                }
                            })
                        }
                        call.respond(object : OutgoingContent.WriteChannelContent() {
                            override val contentType get() = httpResponse.contentType()

                            override val contentLength get() = httpResponse.contentLength()

                            override val status get() = httpResponse.status

                            override val headers get() = Headers.build { appendAll(httpResponse.headers.filter { key, _ -> !key.equals(HttpHeaders.ContentType, ignoreCase = true) && !key.equals(HttpHeaders.ContentLength, ignoreCase = true) }) }

                            override suspend fun writeTo(channel: ByteWriteChannel) {
                                httpResponse.bodyAsChannel().copyAndClose(channel)
                            }
                        })
                    }
                }
            }).also(ApplicationEngine::start)
        }
    }

    override suspend fun stop() {
        applicationEngine?.let {
            it.stop(0, Long.MAX_VALUE)
            applicationEngine = null

            super.stop()
        }
    }
}
