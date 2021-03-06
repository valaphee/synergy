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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.addSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.valaphee.foundry.math.Float2
import com.valaphee.foundry.math.Float3
import com.valaphee.foundry.math.Int3
import com.valaphee.foundry.math.Int4
import com.valaphee.synergy.math.Float2Deserializer
import com.valaphee.synergy.math.Float2Serializer
import com.valaphee.synergy.math.Float3Deserializer
import com.valaphee.synergy.math.Float3Serializer
import com.valaphee.synergy.math.Int3Deserializer
import com.valaphee.synergy.math.Int3Serializer
import com.valaphee.synergy.math.Int4Deserializer
import com.valaphee.synergy.math.Int4Serializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
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
import java.util.concurrent.ThreadFactory

val ObjectMapper: ObjectMapper = jacksonObjectMapper().registerModule(SimpleModule().addSerializer(Float2::class, Float2Serializer).addDeserializer(Float2::class, Float2Deserializer).addSerializer(Float3::class, Float3Serializer).addDeserializer(Float3::class, Float3Deserializer).addSerializer(Int3::class, Int3Serializer).addDeserializer(Int3::class, Int3Deserializer).addSerializer(Int4::class, Int4Serializer).addDeserializer(Int4::class, Int4Deserializer)).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
val HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(ObjectMapper)) }
}
