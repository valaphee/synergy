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
import com.google.inject.Inject
import com.google.protobuf.Descriptors
import com.google.protobuf.RpcChannel
import com.google.protobuf.Service
import com.google.protobuf.kotlin.get
import com.valaphee.synergy.proxy.Connection
import com.valaphee.synergy.proxy.Proxy
import com.valaphee.synergy.proxy.bgs.util.hashFnva32
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.ssl.SslContextBuilder
import javax.net.ssl.KeyManager
import javax.net.ssl.TrustManager

/**
 * @author Kevin Ludwig
 */
class BgsProxy : Proxy {
    @Inject private lateinit var keyManager: KeyManager
    @get:JsonIgnore private val serverSslContext by lazy { SslContextBuilder.forServer(keyManager).build() }
    @Inject private lateinit var trustManager: TrustManager
    @get:JsonIgnore internal val clientSslContext by lazy { SslContextBuilder.forClient().trustManager(trustManager).build() }

    override fun newHandler(connection: Connection) = object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(channel: SocketChannel) {
            channel.pipeline().addLast(
                serverSslContext.newHandler(channel.alloc()),
                HttpServerCodec(),
                HttpObjectAggregator(UShort.MAX_VALUE.toInt()),
                WebSocketServerProtocolHandler("/", "v1.rpc.battle.net"),
                PacketCodec(services),
                FrontendHandler(this@BgsProxy, connection)
            )
        }
    }

    companion object {
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
        ).associate { (it.java.getDeclaredMethod("getDescriptor")(null) as Descriptors.ServiceDescriptor).options[ServiceOptionsProto.serviceOptions].descriptorName.hashFnva32() to it.java.getDeclaredMethod("newStub", RpcChannel::class.java)(null, null) as Service }
    }
}
