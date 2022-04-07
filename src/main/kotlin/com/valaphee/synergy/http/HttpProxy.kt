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
import com.valaphee.synergy.keyStore
import com.valaphee.synergy.keyStoreFile
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
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Calendar
import kotlin.random.asKotlinRandom

/**
 * @author Kevin Ludwig
 */
class HttpProxy(
    @JsonProperty("id") id: String,
    @JsonProperty("host") host: String,
    @JsonProperty("port") port: Int = 443,
    @JsonProperty("interface") `interface`: String
) : TransparentProxy<Unit>(id, host, port, `interface`) {
    private var channel: Channel? = null

    override suspend fun start() {
        require(channel == null)

        super.start()

        val rootCertificate = keyStore.getCertificate("synergy") as X509Certificate
        val serverCertificate = keyStore.getCertificate(host) as X509Certificate? ?: run {
            val serverKeyPair = KeyPairGenerator.getInstance("RSA", "BC").apply { initialize(2048) }.generateKeyPair()
            val rootContentSigner = JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyStore.getKey("synergy", null) as PrivateKey)
            val serverCsr = JcaPKCS10CertificationRequestBuilder(X500Name("CN=$host"), serverKeyPair.public).build(rootContentSigner)
            val serverCertificate = JcaX509CertificateConverter().setProvider("BC").getCertificate(X509v3CertificateBuilder(JcaX509CertificateHolder(rootCertificate).subject, BigInteger(SecureRandom().asKotlinRandom().nextBytes(8)), Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time, Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time, serverCsr.subject, serverCsr.subjectPublicKeyInfo).apply {
                addExtension(Extension.basicConstraints, true, BasicConstraints(false))
                addExtension(Extension.authorityKeyIdentifier, false, JcaX509ExtensionUtils().createAuthorityKeyIdentifier(rootCertificate))
                addExtension(Extension.subjectKeyIdentifier, false, JcaX509ExtensionUtils().createSubjectKeyIdentifier(serverCsr.subjectPublicKeyInfo))
                addExtension(Extension.subjectAlternativeName, false, GeneralNames(GeneralName(GeneralName.dNSName, host)))
            }.build(rootContentSigner))
            keyStore.setKeyEntry(host, serverKeyPair.private, null, arrayOf(serverCertificate))
            keyStoreFile.outputStream().use { keyStore.store(it, "".toCharArray()) }
            serverCertificate
        }
        val sslContextBuilder = SslContextBuilder.forServer(keyStore.getKey(host, null) as PrivateKey, listOf(serverCertificate, rootCertificate)).build()

        channel = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(underlyingNetworking.serverSocketChannel)
            .handler(LoggingHandler())
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(channel: SocketChannel) {
                    channel.pipeline().addLast(
                        sslContextBuilder.newHandler(channel.alloc()),
                        HttpServerCodec(),
                        HttpObjectAggregator(1 * 1024 * 1024),
                        LoggingHandler(),
                        HttpProxyFrontendHandler(this@HttpProxy)
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
