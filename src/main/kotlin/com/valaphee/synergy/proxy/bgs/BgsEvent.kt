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

import com.fasterxml.jackson.annotation.JsonProperty
import com.valaphee.synergy.event.Event
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
abstract class BgsEvent(
    emitterId: UUID,
    emittedAt: Long,
    @get:JsonProperty("id") val id: Int,
    @get:JsonProperty("service_name") val serviceHash: Int,
    @get:JsonProperty("service_hash") val serviceName: String?,
    @get:JsonProperty("method_id") val methodId: Int,
    @get:JsonProperty("method_name") val methodName: String?,
    @get:JsonProperty("data") val data: Any?
) : Event(emitterId, emittedAt)
