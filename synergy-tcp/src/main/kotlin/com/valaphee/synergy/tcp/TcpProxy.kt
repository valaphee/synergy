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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.valaphee.synergy.proxy.RouterProxy
import com.valaphee.synergy.proxy.bossGroup
import com.valaphee.synergy.proxy.underlyingNetworking
import com.valaphee.synergy.proxy.workerGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
class TcpProxy(
    id: UUID = UUID.randomUUID(),
    host: String,
    port: Int,
    `interface`: String
) : RouterProxy<Unit>(id, host, port, `interface`) {
    @JsonIgnore private var channel: Channel? = null

    override suspend fun start() {
        require(channel == null)

        super.start()

        channel = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(underlyingNetworking.serverSocketChannel)
            .childHandler(FrontendHandler(this@TcpProxy))
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