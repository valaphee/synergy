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

import com.google.inject.Singleton
import com.valaphee.synergy.CoroutineScope
import com.valaphee.synergy.HttpClient
import com.valaphee.synergy.keyboard.HidKeyboard
import com.valaphee.synergy.keyboard.RobotKeyboard
import com.valaphee.synergy.mouse.HidMouse
import com.valaphee.synergy.mouse.RobotMouse
import com.valaphee.synergy.proxy.ProxyServer
import com.valaphee.synergy.proxy.mcbe.McbeProxy
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import javafx.scene.control.ContextMenu
import javafx.scene.control.ListCell
import javafx.scene.layout.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tornadofx.Fragment
import tornadofx.action
import tornadofx.hbox
import tornadofx.item
import tornadofx.listview
import tornadofx.menu
import tornadofx.onChange
import tornadofx.toObservable
import tornadofx.vbox
import tornadofx.vgrow

/**
 * @author Kevin Ludwig
 */
@Singleton
class Components : Fragment("Components") {
    private val components = SimpleListProperty(mutableListOf<Component>().toObservable())

    override val root = hbox {
        vbox {
            add(listview(components) {
                vgrow = Priority.ALWAYS
                setCellFactory {
                    object : ListCell<Component>() {
                        init {
                            setOnMouseClicked {
                                if (isEmpty) selectionModel.clearSelection()

                                it.consume()
                            }
                        }

                        override fun updateItem(item: Component?, empty: Boolean) {
                            super.updateItem(item, empty)

                            text = if (empty || item == null) "" else item.id.toString()
                        }
                    }
                }

                fun contextMenu(selectedComponents: ObservableList<out Component>) = ContextMenu().apply {
                    if (selectedComponents.isEmpty()) {
                        menu("Add") {
                            item("Component") { action { ComponentAdd(this@Components, "Component", Component()).openModal() } }
                            menu("Input") {
                                item("HID Keyboard") { action { ComponentAdd(this@Components, "Hid Keyboard", HidKeyboard()).openModal() } }
                                item("Java Robot Keyboard") { action { ComponentAdd(this@Components, "Java Robot Keyboard", RobotKeyboard()).openModal() } }
                                item("HID Mouse") { action { ComponentAdd(this@Components, "Hid Mouse", HidMouse()).openModal() } }
                                item("Java Robot Mouse") { action { ComponentAdd(this@Components, "Java Robot Mouse", RobotMouse()).openModal() } }
                            }
                            menu("Proxy") { item("Minecraft") { action { ComponentAdd(this@Components, "Minecraft Proxy", ProxyServer(proxy = McbeProxy())).openModal() } } }
                        }
                    }
                    else item("Remove") {
                        action {
                            CoroutineScope.launch {
                                selectedComponents.map { launch { HttpClient.delete("http://localhost:8080/component/${it.id}") } }.joinAll()

                                this@Components.refresh()
                            }
                        }
                    }
                }

                contextMenu = contextMenu(selectionModel.selectedItems)
                selectionModel.selectedItems.onChange { contextMenu = contextMenu(it.list) }

                CoroutineScope.launch { this@Components.refresh() }
            })
        }
    }

    suspend fun refresh() {
        HttpClient.get("http://localhost:8080/component/").body<List<Component>>().also {
            withContext(Dispatchers.Main) {
                components.clear()
                components.addAll(it)
            }
        }
    }
}
