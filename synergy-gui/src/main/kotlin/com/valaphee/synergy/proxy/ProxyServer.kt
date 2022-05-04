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
import javafx.scene.control.TabPane
import javafx.scene.layout.Priority
import tornadofx.action
import tornadofx.bind
import tornadofx.button
import tornadofx.checkbox
import tornadofx.chooseFile
import tornadofx.field
import tornadofx.fieldset
import tornadofx.filterInput
import tornadofx.form
import tornadofx.getValue
import tornadofx.hbox
import tornadofx.hgrow
import tornadofx.isInt
import tornadofx.label
import tornadofx.listview
import tornadofx.setValue
import tornadofx.tab
import tornadofx.textfield
import tornadofx.toProperty
import tornadofx.vbox
import java.io.File
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
class ProxyServer(
    id: UUID = UUID.randomUUID(),
    scripts: List<String> = emptyList(),
    @get:JsonProperty("proxy") val proxy: Proxy,
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

    override fun TabPane.addForm() {
        tab("Component") {
            form {
                fieldset {
                    field("Id") { label(this@ProxyServer.id.toString()) }
                    field("Scripts") {
                        vbox {
                            listview(scriptsProperty) { prefHeight = (4 * 24 + 2).toDouble() }
                            hbox {
                                val scriptProperty = "".toProperty()
                                button("+") {
                                    action {
                                        scripts += scriptProperty.value
                                        scriptProperty.value = ""
                                    }
                                }
                                textfield(scriptProperty) { hgrow = Priority.ALWAYS }
                                button("...") {
                                    action {
                                        val parentPath = if (scriptProperty.value.isEmpty()) null else File(scriptProperty.value).parentFile
                                        chooseFile(filters = emptyArray(), initialDirectory = if (parentPath?.isDirectory == true) parentPath else null).firstOrNull()?.let { scriptProperty.value = it.absolutePath }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        tab("Proxy") {
            form {
                fieldset {
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
    }
}
