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
import com.valaphee.synergy.bossGroup
import com.valaphee.synergy.underlyingNetworking
import com.valaphee.synergy.workerGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.X509ExtendedKeyManager

/**
 * @author Kevin Ludwig
 */
class HttpProxy(
    @JsonProperty("id") id: String,
    @JsonProperty("host") host: String,
    @JsonProperty("port") port: Int = 443,
    @JsonProperty("interface") `interface`: String,
    @JsonProperty("ssl") private val ssl: Boolean = true
) : TransparentProxy<Unit>(id, host, port, `interface`) {
    private var channel: Channel? = null

    override suspend fun start() {
        require(channel == null)

        super.start()

        val sslContext = if (ssl) SslContextBuilder.forServer(object : X509ExtendedKeyManager() {
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> = TODO()

            override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) = TODO()

            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> {
                TODO("Not yet implemented")
            }

            override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?) = TODO()

            override fun getCertificateChain(alias: String?): Array<X509Certificate> {
                TODO("Not yet implemented")
            }

            override fun getPrivateKey(alias: String?): PrivateKey {
                TODO("Not yet implemented")
            }
        }).build() else null

        channel = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(underlyingNetworking.serverSocketChannel)
            .handler(LoggingHandler())
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(channel: SocketChannel) {
                    sslContext?.let {
                        channel.pipeline().addLast(sslContext.newHandler(channel.alloc()))
                    }
                    channel.pipeline().addLast(
                        HttpServerCodec(),
                        HttpObjectAggregator(1 * 1024 * 1024),
                        LoggingHandler(),
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
