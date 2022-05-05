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
import tornadofx.action
import tornadofx.bind
import tornadofx.button
import tornadofx.chooseFile
import tornadofx.field
import tornadofx.fieldset
import tornadofx.filterInput
import tornadofx.form
import tornadofx.getValue
import tornadofx.hbox
import tornadofx.hgrow
import tornadofx.label
import tornadofx.listview
import tornadofx.setValue
import tornadofx.textfield
import tornadofx.titledpane
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

    override fun EventTarget.config(new: Boolean) {
        titledpane("Component") {
            isExpanded = true

            form {
                fieldset {
                    field("Type") { label(this@ProxyServer::class.java.name) }
                    field("Id") { label(this@ProxyServer.id.toString()) }
                    field("Scripts") {
                        vbox {
                            listview(scriptsProperty) { prefHeight = (4 * 24 + 2).toDouble() }
                            if (new) hbox {
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
        titledpane("Proxy") {
            isExpanded = true

            form {
                fieldset {
                    field("Local") {
                        if (new) {
                            textfield(localHostProperty) { hgrow = Priority.ALWAYS }
                            textfield {
                                minWidth = 65.0
                                maxWidth = 65.0

                                filterInput { it.controlNewText.toIntOrNull()?.let { it >= 0 && it <= UShort.MAX_VALUE.toInt() } ?: false }
                                bind(localPortProperty, converter = IntStringConverter)
                            }
                        } else label("${localHostProperty.value}:${localPortProperty.value}")
                    }
                    field("Via") {
                        if (new) {
                            textfield(viaHostProperty) { hgrow = Priority.ALWAYS }
                            textfield {
                                minWidth = 65.0
                                maxWidth = 65.0

                                filterInput { it.controlNewText.toIntOrNull()?.let { it >= 0 && it <= UShort.MAX_VALUE.toInt() } ?: false }
                                bind(viaPortProperty, converter = IntStringConverter)
                            }
                        } else label("${viaHostProperty.value}:${viaPortProperty.value}")
                    }
                    field("Remote") {
                        if (new) {
                            textfield(remoteHostProperty) { hgrow = Priority.ALWAYS }
                            textfield {
                                minWidth = 65.0
                                maxWidth = 65.0

                                filterInput { it.controlNewText.toIntOrNull()?.let { it >= 1 && it <= UShort.MAX_VALUE.toInt() } ?: false }
                                bind(remotePortProperty, converter = IntStringConverter)
                            }
                        } else label("${remoteHostProperty.value}:${remotePortProperty.value}")
                    }
                }
            }
        }
    }
}
