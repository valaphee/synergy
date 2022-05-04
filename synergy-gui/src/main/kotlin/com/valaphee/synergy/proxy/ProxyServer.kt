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
import javafx.scene.control.cell.TextFieldListCell
import javafx.scene.layout.Priority
import tornadofx.bind
import tornadofx.checkbox
import tornadofx.field
import tornadofx.fieldset
import tornadofx.filterInput
import tornadofx.getValue
import tornadofx.hgrow
import tornadofx.isInt
import tornadofx.listview
import tornadofx.setValue
import tornadofx.textfield
import tornadofx.toProperty
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
class ProxyServer(
    id: UUID = UUID.randomUUID(),
    scripts: List<String> = emptyList(),
    localHost: String? = null,
    localPort: Int? = null,
    remoteHost: String = "",
    remotePort: Int = 0,
    viaHost: String = "",
    viaPort: Int = 0,
) : Component(id, scripts) {
    private val localHostProperty = localHost.toProperty()
    @get:JsonProperty("local_host") var localHost by localHostProperty

    private val localPortProperty = localPort.toProperty()
    @get:JsonProperty("local_port") var localPort by localPortProperty

    private val remoteHostProperty = remoteHost.toProperty()
    @get:JsonProperty("remote_host") var remoteHost by remoteHostProperty

    private val remotePortProperty = remotePort.toProperty()
    @get:JsonProperty("remote_port") var remotePort by remotePortProperty

    private val viaHostProperty = viaHost.toProperty()
    @get:JsonProperty("via_host") var viaHost by viaHostProperty

    private val viaPortProperty = viaPort.toProperty()
    @get:JsonProperty("via_port") var viaPort by viaPortProperty

    override fun EventTarget.addForm() {
        fieldset {
            field("Id") { textfield(this@ProxyServer.id.toString()) { isEditable = false } }
            field("Scripts") {
                listview(scriptsProperty) {
                    prefHeight = (4 * 24 + 2).toDouble()
                    isEditable = true
                    cellFactory = TextFieldListCell.forListView()
                }
            }
            field("Local") {
                checkbox()
                textfield(localHostProperty) { hgrow = Priority.ALWAYS }
                textfield {
                    minWidth = 65.0
                    maxWidth = 65.0

                    filterInput { it.controlNewText.isInt() }
                    bind(localPortProperty, converter = IntStringConverter)
                }
            }
            field("Via") {
                textfield(viaHostProperty) { hgrow = Priority.ALWAYS }
                textfield {
                    minWidth = 65.0
                    maxWidth = 65.0

                    filterInput { it.controlNewText.isInt() }
                    bind(viaPortProperty, converter = IntStringConverter)
                }
            }
            field("Remote") {
                textfield(remoteHostProperty) { hgrow = Priority.ALWAYS }
                textfield {
                    minWidth = 65.0
                    maxWidth = 65.0

                    filterInput { it.controlNewText.isInt() }
                    bind(remotePortProperty, converter = IntStringConverter)
                }
            }
        }
    }
}
