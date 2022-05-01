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

package com.valaphee.synergy.proxy.mcbe.service

import com.sun.jna.Library
import com.sun.jna.Native

/**
 * @author Kevin Ludwig
 */
interface GoSignature : Library {
    fun GetPublicKeyXY(derKey: GoSlice, x: GoSlice, y: GoSlice)

    fun GenerateSignature(derKey: GoSlice, method: GoSlice, url: GoSlice, authorization: GoSlice, body: GoSlice, result: GoSlice)

    companion object {
        val Instance: GoSignature = Native.load(Native.extractFromResourcePath("/signature.dll").path, GoSignature::class.java)
    }
}
