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

import com.fasterxml.jackson.annotation.JsonProperty
import com.valaphee.synergy.component.Component
import com.valaphee.synergy.util.IntStringConverter
import javafx.event.EventTarget
import javafx.scene.layout.Priority
import tornadofx.bind
import tornadofx.field
import tornadofx.fieldset
import tornadofx.filterInput
import tornadofx.form
import tornadofx.getValue
import tornadofx.hgrow
import tornadofx.setValue
import tornadofx.textfield
import tornadofx.titledpane
import tornadofx.toProperty
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
class ProxyServer(
    id: UUID = UUID.randomUUID(),
    scripts: List<String> = emptyList(),
    @get:JsonProperty("proxy") val proxy: Proxy,
    localHost: String,
    localPort: Int,
    viaHost: String,
    viaPort: Int,
    remoteHost: String,
    remotePort: Int,
) : Component(id, scripts) {
    private val localHostProperty = localHost.toProperty()
    @get:JsonProperty("local_host") var localHost by localHostProperty

    private val localPortProperty = localPort.toProperty()
    @get:JsonProperty("local_port") var localPort by localPortProperty

    private val viaHostProperty = viaHost.toProperty()
    @get:JsonProperty("via_host") var viaHost by viaHostProperty

    private val viaPortProperty = viaPort.toProperty()
    @get:JsonProperty("via_port") var viaPort by viaPortProperty

    private val remoteHostProperty = remoteHost.toProperty()
    @get:JsonProperty("remote_host") var remoteHost by remoteHostProperty

    private val remotePortProperty = remotePort.toProperty()
    @get:JsonProperty("remote_port") var remotePort by remotePortProperty

    override fun config(eventTarget: EventTarget, edit: Boolean) {
        super.config(eventTarget, edit)
        with(eventTarget) {
            titledpane("Proxy") {
                // Properties
                isExpanded = true

                // Children
                form {
                    fieldset {
                        field("Local") {
                            textfield(localHostProperty) {
                                // Parent Properties
                                hgrow = Priority.ALWAYS

                                // Properties
                                isEditable = edit
                            }
                            textfield {
                                // Value
                                bind(localPortProperty, converter = IntStringConverter)

                                // Properties
                                minWidth = 65.0
                                maxWidth = 65.0
                                filterInput { it.controlNewText.toIntOrNull()?.let { it in 0..UShort.MAX_VALUE.toInt() } ?: false }
                                isEditable = edit
                            }
                        }
                        field("Via") {
                            textfield(viaHostProperty) {
                                // Parent Properties
                                hgrow = Priority.ALWAYS

                                // Properties
                                isEditable = edit
                            }
                            textfield {
                                // Value
                                bind(viaPortProperty, converter = IntStringConverter)

                                // Properties
                                minWidth = 65.0
                                maxWidth = 65.0
                                filterInput { it.controlNewText.toIntOrNull()?.let { it in 0..UShort.MAX_VALUE.toInt() } ?: false }
                                isEditable = edit
                            }
                        }
                        field("Remote") {
                            textfield(remoteHostProperty) {
                                // Parent Properties
                                hgrow = Priority.ALWAYS

                                // Properties
                                isEditable = edit
                            }
                            textfield {
                                // Value
                                bind(remotePortProperty, converter = IntStringConverter)

                                // Properties
                                minWidth = 65.0
                                maxWidth = 65.0
                                filterInput { it.controlNewText.toIntOrNull()?.let { it in 0..UShort.MAX_VALUE.toInt() } ?: false }
                                isEditable = edit
                            }
                        }
                    }
                }
            }
        }
    }
}
