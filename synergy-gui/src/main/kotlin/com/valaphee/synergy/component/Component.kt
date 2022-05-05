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

package com.valaphee.synergy.component

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import javafx.beans.property.SimpleListProperty
import javafx.scene.control.TabPane
import javafx.scene.layout.Priority
import tornadofx.action
import tornadofx.button
import tornadofx.chooseFile
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
import tornadofx.getValue
import tornadofx.hbox
import tornadofx.hgrow
import tornadofx.label
import tornadofx.listview
import tornadofx.tab
import tornadofx.textfield
import tornadofx.toObservable
import tornadofx.toProperty
import tornadofx.vbox
import java.io.File
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
open class Component(
    @get:JsonProperty("id") val id: UUID = UUID.randomUUID(),
    scripts: List<String> = emptyList(),
) {
    protected val scriptsProperty = SimpleListProperty(scripts.toObservable())
    @get:JsonProperty("scripts") val scripts: MutableList<String> by scriptsProperty

    open fun TabPane.onAdd() {
        tab("Component") {
            form {
                fieldset {
                    field("Id") { label(this@Component.id.toString()) }
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
    }
}
