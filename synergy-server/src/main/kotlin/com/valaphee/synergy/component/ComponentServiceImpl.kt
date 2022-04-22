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
import com.valaphee.synergy.messages
import com.valaphee.synergy.objectMapper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.util.UUID
import kotlin.concurrent.thread

/**
 * @author Kevin Ludwig
 */
@Singleton
class ComponentServiceImpl @Inject constructor(
    config: Config
) : ComponentService {
    private val _components = mutableMapOf<UUID, Component>()
    override val components get() = _components.values.toList()

    private val onMessage = mutableMapOf<UUID, MutableList<Value>>()
    private val onRemove = mutableMapOf<UUID, Value>()

    init {
        config.components.forEach(::add)
        Runtime.getRuntime().addShutdownHook(thread(false) { components.forEach { runBlocking { remove(it.id) } } })
    }

    override fun add(component: Component) = if (_components.putIfAbsent(component.id, component) == null) {
        component.scripts.forEach {
            val context = Context.create()
            context.getBindings("js").putMember("component", component)
            val script = context.eval(Source.create("js", it.readText()))
            if (script.hasMember("on_remove")) onRemove[component.id] = script.getMember("on_remove")
            if (script.hasMember("on_message")) onMessage.getOrPut(component.id) { mutableListOf() } += script.getMember("on_message")
            if (script.hasMember("on_add")) script.getMember("on_add").executeVoid()
        }
        true
    } else false

    override fun remove(id: UUID): Component? {
        val component = _components.remove(id)
        onMessage.remove(id)
        onRemove.remove(id)?.executeVoid()
        return component
    }

    override suspend fun run() {
        messages.collectLatest {
            val eventProxy = MapProxyObject(objectMapper.convertValue(it))
            onMessage.values.forEach { it.forEach { it.executeVoid(eventProxy) } }
        }
    }
}
