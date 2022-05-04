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
import com.valaphee.synergy.util.defaultAlias
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.ssl.SslContextBuilder
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509ExtendedTrustManager

/**
 * @author Kevin Ludwig
 */
class HttpProxy(
    @get:JsonProperty("ssl") val ssl: Boolean = true
) : Proxy {
    @Inject private lateinit var keyManager: X509ExtendedKeyManager
    @Inject private lateinit var trustManager: X509ExtendedTrustManager
    @get:JsonIgnore internal val clientSslContext by lazy { SslContextBuilder.forClient().trustManager(trustManager).build() }

    override fun getChildHandler(connection: Connection): ChannelHandler {
        val sslContext = if (ssl) SslContextBuilder.forServer(keyManager.defaultAlias(connection.remoteHost)).build() else null
        return object : ChannelInitializer<Channel>() {
            override fun initChannel(channel: Channel) {
                channel.config().isAutoRead = false

                sslContext?.let { channel.pipeline().addLast(it.newHandler(channel.alloc())) }
                channel.pipeline().addLast(
                    HttpServerCodec(),
                    HttpObjectAggregator(1 * 1024 * 1024),
                    FrontendHandler(this@HttpProxy, connection)
                )
            }
        }
    }
}
