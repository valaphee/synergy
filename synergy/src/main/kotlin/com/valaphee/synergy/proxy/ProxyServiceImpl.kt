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

import com.google.inject.Inject
import com.google.inject.Singleton
import com.valaphee.synergy.config.Config
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.concurrent.thread

/**
 * @author Kevin Ludwig
 */
@Singleton
class ProxyServiceImpl @Inject constructor(
    config: Config
) : ProxyService {
    private val _proxies = mutableMapOf<UUID, Proxy<*>>()
    override val proxies get() = _proxies.values.toList()

    init {
        config.proxies.forEach(::add)
        Runtime.getRuntime().addShutdownHook(thread(false) { proxies.forEach { runBlocking { it.stop() } } })
    }

    override fun add(proxy: Proxy<*>) = _proxies.putIfAbsent(proxy.id, proxy) == null

    override fun remove(id: UUID) = _proxies.remove(id)

    override fun get(id: UUID) = _proxies[id]
}
