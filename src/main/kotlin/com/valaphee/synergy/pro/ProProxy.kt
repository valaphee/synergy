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

package com.valaphee.synergy.pro

import bgs.protocol.game_utilities.v2.client.ProcessTaskResponse
import com.fasterxml.jackson.annotation.JsonProperty
import com.nimbusds.srp6.SRP6Routines
import com.valaphee.synergy.TransparentProxy
import com.valaphee.synergy.bossGroup
import com.valaphee.synergy.underlyingNetworking
import com.valaphee.synergy.workerGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.logging.LoggingHandler
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * @author Kevin Ludwig
 */
class ProProxy(
    @JsonProperty("id") id: String,
    @JsonProperty("host") host: String,
    @JsonProperty("port") port: Int,
    @JsonProperty("interface") `interface`: String
) : TransparentProxy<ByteArray>(id, host, port, `interface`) {
    override val dataType get() = ByteArray::class

    private var channel: Channel? = null

    internal lateinit var srpN: BigInteger
    internal lateinit var srpk: BigInteger
    internal var srpI = 0L

    override suspend fun start() {
        require(channel == null)

        super.start()

        channel = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(underlyingNetworking.serverSocketChannel)
            .handler(LoggingHandler())
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(channel: SocketChannel) {
                    channel.pipeline().addLast(
                        LoggingHandler(),
                        ProProxyFrontendHandler(this@ProProxy)
                    )
                }
            })
            /*.childOption(ChannelOption.AUTO_READ, false)*/
            .localAddress(host, port)
            .bind().channel()
    }

    override suspend fun update(data: ByteArray): ByteArray {
        val payload = ProcessTaskResponse.parseFrom(data)
        val results = payload.resultList.associate { it.name to it.value }
        val srpN = ByteArray(64 * 4)
        checkNotNull(results["k0"]).blobValue.toByteArray().copyInto(srpN)
        checkNotNull(results["k1"]).blobValue.toByteArray().copyInto(srpN, 64)
        checkNotNull(results["k2"]).blobValue.toByteArray().copyInto(srpN, 64 + 64)
        checkNotNull(results["k3"]).blobValue.toByteArray().copyInto(srpN, 64 + 64 + 64)
        this.srpN = BigInteger(sha256Local.get().digest(srpN))
        srpI = checkNotNull(results["cid"]).uintValue
        srpk = srpRoutines.computeK(sha256Local.get(), this.srpN, srpG)
        return data
    }

    override suspend fun stop() {
        channel?.let {
            it.close()
            channel = null

            super.stop()
        }
    }

    companion object {
        internal val random = SecureRandom()
        internal val srpRoutines = SRP6Routines()
        internal val srpG = BigInteger.valueOf(2)
        internal val sha256Local = ThreadLocal.withInitial { MessageDigest.getInstance("SHA-256") }
    }
}