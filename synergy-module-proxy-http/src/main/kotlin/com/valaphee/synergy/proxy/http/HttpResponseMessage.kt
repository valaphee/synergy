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

package com.valaphee.synergy.proxy.http

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
@JsonTypeName("http_response")
class HttpResponseMessage(
    emitterId: UUID,
    emittedAt: Long,
    @get:JsonProperty("status") val status: Int,
    @get:JsonProperty("message") val message: String,
    headers: Map<String, String>
) : HttpMessage(emitterId, emittedAt, headers)
