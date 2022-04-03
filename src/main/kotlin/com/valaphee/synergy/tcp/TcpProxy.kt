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

package com.valaphee.synergy.tcp

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
import io.netty.handler.logging.LoggingHandler

/**
 * @author Kevin Ludwig
 */
class TcpProxy(
    @JsonProperty("id") id: String,
    @JsonProperty("host") host: String,
    @JsonProperty("port") port: Int,
    @JsonProperty("interface_host") interfaceHost: String,
    @JsonProperty("interface_port") interfacePort: Int
) : TransparentProxy(id, host, port, interfaceHost, interfacePort) {
    private var channel: Channel? = null

    override suspend fun start() {
        if (channel == null) {
            super.start()

            channel = ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(underlyingNetworking.serverSocketChannel)
                .handler(LoggingHandler())
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(channel: SocketChannel) {
                        channel.pipeline().addLast(
                            LoggingHandler(),
                            TcpProxyFrontendHandler(this@TcpProxy)
                        )
                    }
                })
                .childOption(ChannelOption.AUTO_READ, false)
                .localAddress(host, port)
                .bind().channel()
        }
    }

    override suspend fun stop() {
        channel?.let {
            it.close()
            channel = null

            super.stop()
        }
    }
}
