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

package com.valaphee.synergy.proxy.http

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.inject.Inject
import com.valaphee.synergy.proxy.Connection
import com.valaphee.synergy.proxy.Proxy
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.ssl.SslContextBuilder
import javax.net.ssl.KeyManager
import javax.net.ssl.TrustManager

/**
 * @author Kevin Ludwig
 */
class HttpProxy(
    @get:JsonProperty("ssl") val ssl: Boolean = true
) : Proxy {
    @Inject private lateinit var keyManager: KeyManager
    @get:JsonIgnore private val serverSslContext by lazy { SslContextBuilder.forServer(keyManager).build() }
    @Inject private lateinit var trustManager: TrustManager
    @get:JsonIgnore internal val clientSslContext by lazy { SslContextBuilder.forClient().trustManager(trustManager).build() }

    override fun newHandler(connection: Connection) = object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(channel: SocketChannel) {
            channel.config().isAutoRead = false
            if (ssl) channel.pipeline().addLast(serverSslContext.newHandler(channel.alloc()))
            channel.pipeline().addLast(
                HttpServerCodec(),
                HttpObjectAggregator(1 * 1024 * 1024),
                FrontendHandler(this@HttpProxy, connection)
            )
        }
    }
}
