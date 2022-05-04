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

package com.valaphee.synergy.mouse

import com.fasterxml.jackson.annotation.JsonProperty
import com.valaphee.synergy.util.FloatStringConverter
import com.valaphee.synergy.util.IntStringConverter
import javafx.beans.property.SimpleFloatProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.control.TabPane
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
import tornadofx.isFloat
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
class HidMouse(
    id: UUID = UUID.randomUUID(),
    scripts: List<String> = emptyList(),
    sensitivity: Float = 1.0f,
    precision: Int = 16
) : Mouse(id, scripts) {
    private val sensitivityProperty = SimpleFloatProperty(sensitivity)
    @get:JsonProperty("sensitivity") var sensitivity by sensitivityProperty

    private val precisionProperty = SimpleIntegerProperty(precision)
    @get:JsonProperty("precision") var precision by precisionProperty

    override fun TabPane.addForm() {
        tab("Component") {
            form {
                fieldset {
                    field("Id") { label(this@HidMouse.id.toString()) }
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
        tab("Mouse") {
            form {
                fieldset {
                    field("Sensitivity") {
                        textfield {
                            bind(sensitivityProperty, converter = FloatStringConverter)

                            filterInput { it.controlNewText.isFloat() }
                        }
                    }
                    field("Precision") {
                        textfield {
                            bind(precisionProperty, converter = IntStringConverter)

                            filterInput { it.controlNewText.isInt() }
                        }
                    }
                }
            }
        }
    }
}
