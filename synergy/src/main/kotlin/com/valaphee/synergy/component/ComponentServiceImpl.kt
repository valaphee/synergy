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

package com.valaphee.synergy.component

import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.inject.Inject
import com.google.inject.Singleton
import com.valaphee.synergy.config.Config
import com.valaphee.synergy.event.events
import com.valaphee.synergy.proxy.objectMapper
import kotlinx.coroutines.flow.collectLatest
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
@Singleton
class ComponentServiceImpl @Inject constructor(
    config: Config
) : ComponentService {
    private val _components = mutableMapOf<UUID, Pair<Component, List<Value>>>()
    override val components get() = _components.values.map { it.first }.toList()

    init {
        config.components.forEach(::add)
    }

    override fun add(component: Component) = _components.putIfAbsent(component.id, component to component.controller.map { Context.create().eval(Source.create("js", it.readText())) }) == null

    override fun remove(id: UUID) = _components.remove(id)?.first

    override suspend fun run() {
        events.collectLatest {
            val eventProxy = MapProxyObject(objectMapper.convertValue(it))
            _components.values.forEach { (component, controller) -> controller.forEach { it.execute(component, eventProxy) } }
        }
    }
}
