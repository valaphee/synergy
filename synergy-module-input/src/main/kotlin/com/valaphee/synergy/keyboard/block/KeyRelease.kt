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

package com.valaphee.synergy.keyboard.block

import com.fasterxml.jackson.module.kotlin.convertValue
import com.valaphee.synergy.ObjectMapper
import com.valaphee.synergy.block.Block
import com.valaphee.synergy.keyboard.Keyboard
import com.valaphee.synergy.module.block.ModuleBlock
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
data class KeyRelease(
    override val id: UUID = UUID.randomUUID(),
    override val `in`: Map<String, Block>,
    override val module: Keyboard
) : ModuleBlock() {
    override suspend fun evaluate() = module.keyRelease(ObjectMapper.convertValue(requireNotNull(`in`["key"]).evaluate()))
}
