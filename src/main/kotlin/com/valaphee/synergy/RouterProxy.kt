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

package com.valaphee.synergy

import com.sun.jna.platform.win32.Shell32
import kotlinx.coroutines.delay
import java.net.InetAddress

/**
 * @author Kevin Ludwig
 */
abstract class RouterProxy<T>(
    override val id: String,
    val host: String,
    val port: Int,
    val `interface`: String,
) : Proxy<T> {
    override suspend fun start() {
        Shell32.INSTANCE.ShellExecute(null, "runas", "cmd.exe", "/S /C \"netsh int ip add address \"Loopback\" ${InetAddress.getByName(host).hostAddress}/32\"", null, 0)
        delay(250)
    }

    override suspend fun stop() {
        Shell32.INSTANCE.ShellExecute(null, "runas", "cmd.exe", "/S /C \"netsh int ip delete address \"Loopback\" ${InetAddress.getByName(host).hostAddress}\"", null, 0)
    }
}