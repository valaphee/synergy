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
import tornadofx.bind
import tornadofx.field
import tornadofx.fieldset
import tornadofx.filterInput
import tornadofx.form
import tornadofx.getValue
import tornadofx.isFloat
import tornadofx.isInt
import tornadofx.setValue
import tornadofx.textfield
import tornadofx.titledpane
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

    override fun config(eventTarget: EventTarget, edit: Boolean) {
        super.config(eventTarget, edit)
        with(eventTarget) {
            titledpane("Mouse") {
                // Properties
                isExpanded = true

                // Children
                form {
                    fieldset {
                        field("Sensitivity") {
                            textfield {
                                // Value
                                bind(sensitivityProperty, converter = FloatStringConverter)

                                // Properties
                                filterInput { it.controlNewText.isFloat() }
                                isEditable = edit
                            }
                        }
                        field("Precision") {
                            textfield {
                                // Value
                                bind(precisionProperty, converter = IntStringConverter)

                                // Properties
                                filterInput { it.controlNewText.isInt() }
                                isEditable = edit
                            }
                        }
                    }
                }
            }
        }
    }
}
