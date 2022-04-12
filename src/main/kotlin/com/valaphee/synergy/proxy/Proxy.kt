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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.valaphee.synergy.proxy.bgs.BgsProxy
import com.valaphee.synergy.proxy.http.HttpProxy
import com.valaphee.synergy.proxy.mcbe.McbeProxy
import com.valaphee.synergy.proxy.pro.ProProxy
import com.valaphee.synergy.proxy.tcp.TcpProxy
import kotlin.reflect.KClass

/**
 * @author Kevin Ludwig
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = BgsProxy::class),
    JsonSubTypes.Type(value = HttpProxy::class),
    JsonSubTypes.Type(value = McbeProxy::class),
    JsonSubTypes.Type(value = ProProxy::class),
    JsonSubTypes.Type(value = TcpProxy::class)
)
interface Proxy<T> {
    @get:JsonProperty("type") val type: String
    @get:JsonProperty("id") val id: String
    @get:JsonIgnore val dataType: KClass<*> get() = Any::class

    suspend fun start()

    suspend fun update(data: T) = data

    suspend fun stop()
}
