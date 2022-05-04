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
import javafx.event.EventTarget
import javafx.scene.control.cell.TextFieldListCell
import tornadofx.bind
import tornadofx.field
import tornadofx.fieldset
import tornadofx.filterInput
import tornadofx.getValue
import tornadofx.isFloat
import tornadofx.isInt
import tornadofx.listview
import tornadofx.setValue
import tornadofx.textfield
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

    override fun EventTarget.addForm() {
        fieldset {
            field("Id") { textfield(this@HidMouse.id.toString()) { isEditable = false } }
            field("Scripts") {
                listview(scriptsProperty) {
                    prefHeight = (4 * 24 + 2).toDouble()
                    isEditable = true
                    cellFactory = TextFieldListCell.forListView()
                }
            }
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
