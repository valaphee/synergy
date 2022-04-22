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

package com.valaphee.synergy.proxy

import com.fasterxml.jackson.annotation.JsonProperty
import com.sun.jna.platform.win32.Shell32
import com.valaphee.synergy.component.Component
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.graalvm.polyglot.HostAccess
import java.net.InetAddress
import java.net.URL
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
abstract class Proxy(
    id: UUID,
    scripts: List<URL>,
    @get:JsonProperty("local_host") val localHost: String?,
    @get:JsonProperty("local_port") val localPort: Int?,
    @get:JsonProperty("remote_host") val remoteHost: String,
    @get:JsonProperty("remote_port") val remotePort: Int,
    @get:JsonProperty("via_host") val viaHost: String?,
    @get:JsonProperty("via_port") val viaPort: Int,
) : Component(id, scripts) {
    @HostAccess.Export
    fun start() = runBlocking { _start() }

    open suspend fun _start() {
        if (localHost == null) {
            Shell32.INSTANCE.ShellExecute(null, "runas", "cmd.exe", "/S /C \"netsh int ip add address \"Loopback\" ${InetAddress.getByName(remoteHost).hostAddress}/32\"", null, 0)
            delay(250)
        }
    }

    @HostAccess.Export
    fun stop() = runBlocking { _stop() }

    open suspend fun _stop() {
        if (localHost == null) {
            Shell32.INSTANCE.ShellExecute(null, "runas", "cmd.exe", "/S /C \"netsh int ip delete address \"Loopback\" ${InetAddress.getByName(remoteHost).hostAddress}\"", null, 0)
        }
    }
}
