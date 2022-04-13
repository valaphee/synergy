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

package com.valaphee.synergy.proxy.bgs

import bgs.protocol.ServiceOptionsProto
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.inject.Inject
import com.google.inject.name.Named
import com.google.protobuf.Descriptors
import com.google.protobuf.RpcChannel
import com.google.protobuf.Service
import com.google.protobuf.kotlin.get
import com.valaphee.synergy.proxy.Location
import com.valaphee.synergy.proxy.RouterProxy
import com.valaphee.synergy.proxy.bgs.util.hashFnv1a
import com.valaphee.synergy.proxy.bossGroup
import com.valaphee.synergy.proxy.underlyingNetworking
import com.valaphee.synergy.proxy.workerGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
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
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Calendar
import java.util.UUID
import kotlin.random.asKotlinRandom

/**
 * @author Kevin Ludwig
 */
@JsonTypeName("bgs")
class BgsProxy(
    id: UUID = UUID.randomUUID(),
    host: String,
    port: Int = 1119,
    `interface`: String,
    @get:JsonProperty("referral") val referral: Location
) : RouterProxy<Unit>(id, host, port, `interface`) {
    override val type get() = "bgs"

    @JsonIgnore @Inject private lateinit var keyStore: KeyStore
    @JsonIgnore @Inject @Named("key-store") private lateinit var keyStoreFile: File

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
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(channel: SocketChannel) {
                    channel.pipeline().addLast(
                        sslContextBuilder.newHandler(channel.alloc()),
                        HttpServerCodec(),
                        HttpObjectAggregator(UShort.MAX_VALUE.toInt()),
                        WebSocketServerProtocolHandler("/", "v1.rpc.battle.net"),
                        PacketCodec(services),
                        FrontendHandler(this@BgsProxy)
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

            super.stop()
        }
    }

    companion object {
        private val dummyRpcChannel = RpcChannel { _, _, _, _, _ -> }
        internal val services = listOf(
            bgs.protocol.account.v1.AccountListener::class,
            bgs.protocol.account.v1.AccountService::class,
            bgs.protocol.authentication.v1.AuthenticationListener::class,
            bgs.protocol.authentication.v1.AuthenticationService::class,
            bgs.protocol.challenge.v1.ChallengeListener::class,
            bgs.protocol.channel.v1.ChannelListener::class,
            bgs.protocol.channel.v1.ChannelService::class,
            bgs.protocol.channel.v1.ChannelVoiceService::class,
            bgs.protocol.channel.v2.ChannelListener::class,
            bgs.protocol.channel.v2.ChannelService::class,
            bgs.protocol.channel.v2.membership.ChannelMembershipListener::class,
            bgs.protocol.channel.v2.membership.ChannelMembershipService::class,
            bgs.protocol.club.v1.membership.ClubMembershipListener::class,
            bgs.protocol.club.v1.membership.ClubMembershipService::class,
            bgs.protocol.connection.v1.ConnectionService::class,
            bgs.protocol.diag.v1.DiagService::class,
            bgs.protocol.friends.v1.FriendsListener::class,
            bgs.protocol.friends.v1.FriendsService::class,
            bgs.protocol.game_utilities.v2.client.GameUtilitiesService::class,
            bgs.protocol.presence.v1.PresenceListener::class,
            bgs.protocol.presence.v1.PresenceService::class,
            bgs.protocol.report.v1.ReportService::class,
            bgs.protocol.report.v2.ReportService::class,
            bgs.protocol.resources.v1.ResourcesService::class,
            bgs.protocol.session.v1.SessionListener::class,
            bgs.protocol.session.v1.SessionService::class,
            bgs.protocol.sns.v1.SocialNetworkListener::class,
            bgs.protocol.sns.v1.SocialNetworkService::class,
            bgs.protocol.user_manager.v1.UserManagerListener::class,
            bgs.protocol.user_manager.v1.UserManagerService::class,
            bgs.protocol.whisper.v1.WhisperListener::class,
            bgs.protocol.whisper.v1.WhisperService::class
        ).associate { (it.java.getDeclaredMethod("getDescriptor")(null) as Descriptors.ServiceDescriptor).options[ServiceOptionsProto.serviceOptions].descriptorName.hashFnv1a() to it.java.getDeclaredMethod("newStub", RpcChannel::class.java)(null, dummyRpcChannel) as Service }
    }
}
