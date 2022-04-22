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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.inject.Inject
import com.valaphee.synergy.proxy.RouterProxy
import com.valaphee.synergy.proxy.bossGroup
import com.valaphee.synergy.proxy.underlyingNetworking
import com.valaphee.synergy.proxy.workerGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.ssl.SslContextBuilder
import java.util.UUID
import javax.net.ssl.KeyManager
import javax.net.ssl.TrustManager

/**
 * @author Kevin Ludwig
 */
class HttpProxy(
    id: UUID = UUID.randomUUID(),
    host: String,
    port: Int = 443,
    `interface`: String,
    @get:JsonProperty("ssl") val ssl: Boolean = true
) : RouterProxy<Unit>(id, host, port, `interface`) {
    @Inject @JsonIgnore private lateinit var keyManager: KeyManager
    @get:JsonIgnore private val serverSslContext by lazy { SslContextBuilder.forServer(keyManager).build() }
    @Inject @JsonIgnore private lateinit var trustManager: TrustManager
    @get:JsonIgnore internal val clientSslContext by lazy { SslContextBuilder.forClient().trustManager(trustManager).build() }

    @JsonIgnore private var channel: Channel? = null

    override suspend fun start() {
        require(channel == null)

        super.start()

        channel = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(underlyingNetworking.serverSocketChannel)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(channel: SocketChannel) {
                    if (ssl) channel.pipeline().addLast(serverSslContext.newHandler(channel.alloc()))
                    channel.pipeline().addLast(
                        HttpServerCodec(),
                        HttpObjectAggregator(1 * 1024 * 1024),
                        FrontendHandler(this@HttpProxy)
                    )
                }
            })
            .childOption(ChannelOption.AUTO_READ, false)
            .localAddress(host, port)
            .bind().channel()
    }

    override suspend fun stop() {
        channel?.let {
            it.close()
            channel = null

            super.stop()
        }
    }
}
