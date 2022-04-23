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

package com.valaphee.synergy.util

import java.security.Principal
import javax.net.ssl.ExtendedSSLSession
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509KeyManager

/**
 * @author Kevin Ludwig
 */
class DefaultAliasKeyManager(
    private val keyManager: X509KeyManager,
    private val defaultAlias: String
) : X509ExtendedKeyManager(), X509KeyManager by keyManager {
    override fun chooseEngineServerAlias(keyType: String, issuers: Array<out Principal>?, engine: SSLEngine): String? {
        val session = engine.handshakeSession
        return if (session is ExtendedSSLSession) session.requestedServerNames.singleOrNull()?.let { (it as SNIHostName).asciiName } ?: defaultAlias else null
    }
}

fun X509KeyManager.defaultAlias(defaultAlias: String) = DefaultAliasKeyManager(this, defaultAlias)
