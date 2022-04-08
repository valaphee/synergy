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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.valaphee.synergy.tcp.TcpProxy
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import okhttp3.internal.toHexString
import java.net.InetSocketAddress
import kotlin.reflect.KClass

/**
 * @author Kevin Ludwig
 */
@JsonSubTypes(
    JsonSubTypes.Type(IpcLocation::class),
    JsonSubTypes.Type(PassthroughLocation::class)
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
interface Location {
    fun <T : Any> getAddress(address: InetSocketAddress, data: T, dataType: KClass<T>): Pair<InetSocketAddress, T>
}

/**
 * @author Kevin Ludwig
 */
@JsonTypeName("passthrough")
class PassthroughLocation : Location {
    override fun <T : Any> getAddress(address: InetSocketAddress, data: T, dataType: KClass<T>) = address to data
}

/**
 * @author Kevin Ludwig
 */
@JsonTypeName("ipc")
class IpcLocation(
    @JsonProperty("url") val url: String,
    @JsonProperty("type") val type: String,
    @JsonProperty("host") val host: String?,
    @JsonProperty("port") val port: Int?,
) : Location {
    override fun <T : Any> getAddress(address: InetSocketAddress, data: T, dataType: KClass<T>): Pair<InetSocketAddress, T> {
        val id = address.hashCode().toHexString()
        runBlocking {
            httpClient.post("$url/proxy/$type") {
                parameter("autoStart", true)
                contentType(ContentType.Application.Json)
                setBody(TcpProxy(id, host ?: address.hostString, port ?: address.port, "172.16.1.116"))
            }
        }
        val responseData = objectMapper.readValue(runBlocking {
            httpClient.post("$url/proxy/$id/update") {
                setBody(object : OutgoingContent.ByteArrayContent() {
                    override val contentType: ContentType get() = ContentType.Application.Json

                    override fun bytes() = objectMapper.writeValueAsBytes(data)
                })
            }.bodyAsText()
        }, dataType.java)
        return if (host != null && port != null) InetSocketAddress(host, port) to responseData else address to responseData
    }

    companion object {
        private val httpClient = HttpClient(OkHttp) { install(ContentNegotiation) { jackson() } }
    }
}
