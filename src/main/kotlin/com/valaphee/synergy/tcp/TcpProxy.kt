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
import com.sun.jna.platform.win32.Shell32
import com.valaphee.synergy.Proxy
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import kotlinx.coroutines.delay

/**
 * @author Kevin Ludwig
 */
data class TcpProxy(
    @JsonProperty("id") override val id: String,
    @JsonProperty("host") val host: String,
    @JsonProperty("port") val port: Int,
    @JsonProperty("interface_host") val interfaceHost: String,
    @JsonProperty("interface_port") val interfacePort: Int
) : Proxy {
    private var channel: Channel? = null

    override suspend fun start() {
        check(channel == null)

        Shell32.INSTANCE.ShellExecute(null, "runas", "cmd.exe", "/S /C \"netsh int ip add address \"Loopback\" $host/32\"", null, 0)
        delay(250)

        channel = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(LoggingHandler(LogLevel.INFO), TcpProxyFrontendHandler(this@TcpProxy))
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

            Shell32.INSTANCE.ShellExecute(null, "runas", "cmd.exe", "/S /C \"netsh int ip delete address \"Loopback\" $host\"", null, 0)
        }
    }

    companion object {
        internal val bossGroup = NioEventLoopGroup(1)
        internal val workerGroup = NioEventLoopGroup(0)
    }
}
