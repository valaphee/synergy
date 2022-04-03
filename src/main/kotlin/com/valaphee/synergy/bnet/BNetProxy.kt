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
import com.sun.jna.platform.win32.Shell32
import com.valaphee.synergy.Proxy
import com.valaphee.synergy.rootCertificate
import com.valaphee.synergy.rootContentSigner
import com.valaphee.synergy.tcp.TcpProxy
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import kotlinx.coroutines.delay
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.Calendar
import kotlin.random.asKotlinRandom

/**
 * @author Kevin Ludwig
 */
class BNetProxy(
    @JsonProperty("id") override val id: String,
    @JsonProperty("host") val host: String,
    @JsonProperty("port") val port: Int,
    @JsonProperty("interface_host") val interfaceHost: String,
    @JsonProperty("interface_port") val interfacePort: Int,
    @JsonProperty("name") val name: String
) : Proxy {
    private var channel: Channel? = null

    override suspend fun start() {
        check(channel == null)

        Shell32.INSTANCE.ShellExecute(null, "runas", "cmd.exe", "/S /C \"netsh int ip add address \"Loopback\" $host/32\"", null, 0)
        delay(250)

        val serverKeyPair = KeyPairGenerator.getInstance("RSA", "BC").apply { initialize(2048) }.generateKeyPair()
        val serverCsr = JcaPKCS10CertificationRequestBuilder(X500Name("CN=$name"), serverKeyPair.public).build(rootContentSigner)
        val sslContextBuilder = SslContextBuilder.forServer(serverKeyPair.private, listOf(JcaX509CertificateConverter().setProvider("BC").getCertificate(X509v3CertificateBuilder(X500Name("CN=Synergy Root CA"), BigInteger(SecureRandom().asKotlinRandom().nextBytes(8)), Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time, Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time, serverCsr.subject, serverCsr.subjectPublicKeyInfo).apply {
            addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.keyEncipherment or KeyUsage.digitalSignature))
            addExtension(Extension.basicConstraints, true, BasicConstraints(false))
            addExtension(Extension.authorityKeyIdentifier, false, JcaX509ExtensionUtils().createAuthorityKeyIdentifier(rootCertificate))
            addExtension(Extension.subjectKeyIdentifier, false, JcaX509ExtensionUtils().createSubjectKeyIdentifier(serverCsr.subjectPublicKeyInfo))
            addExtension(Extension.subjectAlternativeName, false, DERSequence(arrayOf(GeneralName(GeneralName.dNSName, name))))
            addExtension(Extension.extendedKeyUsage, false, ExtendedKeyUsage(arrayOf(KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth)))
        }.build(rootContentSigner)), rootCertificate)).build()

        channel = ServerBootstrap()
            .group(TcpProxy.bossGroup, TcpProxy.workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        sslContextBuilder.newHandler(ch.alloc()),
                        HttpServerCodec(),
                        HttpObjectAggregator(UShort.MAX_VALUE.toInt()),
                        LoggingHandler(LogLevel.INFO),
                        WebSocketServerProtocolHandler("/", "v1.rpc.battle.net"),
                        BNetProxyFrontendHandler(this@BNetProxy)
                    )
                }
            })
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
}
