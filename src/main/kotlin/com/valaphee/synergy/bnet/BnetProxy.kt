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

package com.valaphee.synergy.bnet

import com.fasterxml.jackson.annotation.JsonProperty
import com.valaphee.synergy.TransparentProxy
import com.valaphee.synergy.bossGroup
import com.valaphee.synergy.rootCertificate
import com.valaphee.synergy.rootContentSigner
import com.valaphee.synergy.underlyingNetworking
import com.valaphee.synergy.workerGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Calendar
import kotlin.random.asKotlinRandom

/**
 * @author Kevin Ludwig
 */
class BnetProxy(
    @JsonProperty("id") id: String,
    @JsonProperty("host") host: String,
    @JsonProperty("port") port: Int,
    @JsonProperty("interface_host") interfaceHost: String,
    @JsonProperty("interface_port") interfacePort: Int,
    @JsonProperty("name") val name: String
) : TransparentProxy(id, host, port, interfaceHost, interfacePort) {
    private var channel: Channel? = null

    override suspend fun start() {
        if (channel == null) {
            super.start()

            val serverKeyPair = KeyPairGenerator.getInstance("RSA", "BC").apply { initialize(2048) }.generateKeyPair()
            val serverCsr = JcaPKCS10CertificationRequestBuilder(X500Name("CN=$name"), serverKeyPair.public).build(rootContentSigner)
            val sslContextBuilder = SslContextBuilder.forServer(serverKeyPair.private, listOf(JcaX509CertificateConverter().setProvider("BC").getCertificate(X509v3CertificateBuilder(JcaX509CertificateHolder(rootCertificate).subject, BigInteger(SecureRandom().asKotlinRandom().nextBytes(8)), Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time, Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time, serverCsr.subject, serverCsr.subjectPublicKeyInfo).apply {
                addExtension(Extension.basicConstraints, true, BasicConstraints(false))
                addExtension(Extension.authorityKeyIdentifier, false, JcaX509ExtensionUtils().createAuthorityKeyIdentifier(rootCertificate))
                addExtension(Extension.subjectKeyIdentifier, false, JcaX509ExtensionUtils().createSubjectKeyIdentifier(serverCsr.subjectPublicKeyInfo))
            }.build(rootContentSigner)), rootCertificate)).build()
            println(Hex.encode(MessageDigest.getInstance("SHA256", "BC").digest(serverKeyPair.public.encoded)).decodeToString().uppercase())

            channel = ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(underlyingNetworking.serverSocketChannel)
                .handler(LoggingHandler())
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(
                            sslContextBuilder.newHandler(ch.alloc()),
                            HttpServerCodec(),
                            HttpObjectAggregator(UShort.MAX_VALUE.toInt()),
                            WebSocketServerProtocolHandler("/", "v1.rpc.battle.net"),
                            LoggingHandler(),
                            BnetProxyFrontendHandler(this@BnetProxy)
                        )
                    }
                })
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
