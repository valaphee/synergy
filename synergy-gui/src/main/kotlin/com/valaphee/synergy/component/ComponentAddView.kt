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

import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.JMetroStyleClass
import jfxtras.styles.jmetro.Style
import tornadofx.View
import tornadofx.action
import tornadofx.button
import tornadofx.buttonbar
import tornadofx.form
import tornadofx.vbox
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * @author Kevin Ludwig
 */
class ComponentAddView(
    name: String,
    `class`: KClass<out Component>
) : View("New $name") {
    override val root = vbox {
        JMetro(this, Style.DARK)
        styleClass.add(JMetroStyleClass.BACKGROUND)
        prefWidth = 600.0

        form { with(`class`.createInstance()) { addForm() } }
        buttonbar {
            button("Create")
            button("Cancel") { action { close() } }
        }
    }
}