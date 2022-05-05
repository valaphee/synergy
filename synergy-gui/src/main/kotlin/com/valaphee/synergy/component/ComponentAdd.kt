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

import com.valaphee.synergy.CoroutineScope
import com.valaphee.synergy.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import javafx.scene.control.TabPane
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.JMetroStyleClass
import jfxtras.styles.jmetro.Style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tornadofx.View
import tornadofx.action
import tornadofx.button
import tornadofx.buttonbar
import tornadofx.tabpane
import tornadofx.vbox

/**
 * @author Kevin Ludwig
 */
class ComponentAdd(
    componentExplorer: ComponentExplorer,
    name: String,
    component: Component
) : View("New $name") {
    override val root = vbox {
        JMetro(this, Style.DARK)
        styleClass.add(JMetroStyleClass.BACKGROUND)

        prefWidth = 600.0

        tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

            with(component) { onAdd() }
        }
        buttonbar {
            button("Create") {
                action {
                    CoroutineScope.launch {
                        if (HttpClient.post("http://localhost:8080/component") {
                                contentType(ContentType.Application.Json)
                                setBody(component)
                        }.status == HttpStatusCode.OK) {
                            componentExplorer.refresh()
                            withContext(Dispatchers.Main) { close() }
                        }
                    }
                }
            }
            button("Cancel") { action { close() } }
        }
    }
}
