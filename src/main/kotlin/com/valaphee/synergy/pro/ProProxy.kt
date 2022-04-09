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
import bgs.protocol.v2.Attribute
import com.fasterxml.jackson.annotation.JsonProperty
import com.nimbusds.srp6.SRP6Routines
import com.valaphee.synergy.RouterProxy
import com.valaphee.synergy.bossGroup
import com.valaphee.synergy.underlyingNetworking
import com.valaphee.synergy.workerGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
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
) : RouterProxy<ByteArray>(id, host, port, `interface`) {
    override val dataType get() = ByteArray::class

    private var channel: Channel? = null

    var cid = 0L
    lateinit var k0: ByteArray
    lateinit var k1: ByteArray
    lateinit var k2: ByteArray
    lateinit var k3: ByteArray

    override suspend fun start() {
        require(channel == null)

        super.start()

        channel = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(underlyingNetworking.serverSocketChannel)
            .childHandler(FrontendHandler(this@ProProxy))
            .childOption(ChannelOption.AUTO_READ, false)
            .localAddress(host, port)
            .bind().channel()
    }

    override suspend fun update(data: ByteArray): ByteArray {
        val payload = ProcessTaskResponse.parseFrom(data)
        val results = payload.resultList.associate { it.name to it.value }.toMutableMap()
        cid = checkNotNull(results["cid"]).uintValue
        k0 = checkNotNull(results["k0"]).blobValue.toByteArray() // when invalid server doesn't accept LoginSrp1 packet, connection gets closed
        k1 = checkNotNull(results["k1"]).blobValue.toByteArray() // when invalid client doesn't accept LoginSrp2 packet, connection gets closed
        k2 = checkNotNull(results["k2"]).blobValue.toByteArray() // when invalid client doesn't accept LoginSrp2 packet, connection gets closed
        k3 = checkNotNull(results["k3"]).blobValue.toByteArray() // when invalid client doesn't accept LoginSrp2 packet, nothing happens
        return payload.toBuilder().clearResult().addAllResult(results.map { Attribute.newBuilder().setName(it.key).setValue(it.value).build() }).build().toByteArray()
    }

    override suspend fun stop() {
        channel?.let {
            it.close()
            channel = null

            super.stop()
        }
    }

    companion object {
        internal val sha256Local = ThreadLocal.withInitial { MessageDigest.getInstance("SHA-256") }
        internal val random = SecureRandom()
        internal val srpRoutines = SRP6Routines()
        internal val srpN = BigInteger.probablePrime(256, random)
        internal val srpg = BigInteger.valueOf(2)
        internal val srpk = srpRoutines.computeK(sha256Local.get(), srpN, srpg)
    }
}
