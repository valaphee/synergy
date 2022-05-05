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
import javafx.beans.property.SimpleObjectProperty
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
import tornadofx.bindSelected
import tornadofx.dynamicContent
import tornadofx.item
import tornadofx.listview
import tornadofx.menu
import tornadofx.onChange
import tornadofx.splitpane
import tornadofx.toObservable
import tornadofx.vbox
import tornadofx.vgrow
import java.util.UUID

/**
 * @author Kevin Ludwig
 */
class ComponentExplorer : Fragment("Component Explorer") {
    private val componentsProperty = SimpleListProperty(mutableListOf<Component>().toObservable())
    private val componentProperty = SimpleObjectProperty<Component>()

    override val root = splitpane() {
        listview(componentsProperty) {
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
                        item("Component") { action { ComponentAdd(this@ComponentExplorer, "Component", Component()).openModal() } }
                        menu("Input") {
                            item("HID Keyboard") { action { ComponentAdd(this@ComponentExplorer, "Hid Keyboard", HidKeyboard()).openModal() } }
                            item("Java Robot Keyboard") { action { ComponentAdd(this@ComponentExplorer, "Java Robot Keyboard", RobotKeyboard()).openModal() } }
                            item("HID Mouse") { action { ComponentAdd(this@ComponentExplorer, "Hid Mouse", HidMouse()).openModal() } }
                            item("Java Robot Mouse") { action { ComponentAdd(this@ComponentExplorer, "Java Robot Mouse", RobotMouse()).openModal() } }
                        }
                        menu("Proxy") { item("Minecraft") { action { ComponentAdd(this@ComponentExplorer, "Minecraft Proxy", ProxyServer(UUID.randomUUID(), emptyList(), McbeProxy(), "127.0.0.1", 19134, "", 0, "127.0.0.1", 19132)).openModal() } } }
                    }
                }
                else item("Remove") {
                    action {
                        CoroutineScope.launch {
                            selectedComponents.map { launch { HttpClient.delete("http://localhost:8080/component/${it.id}") } }.joinAll()

                            this@ComponentExplorer.refresh()
                        }
                    }
                }
            }

            contextMenu = contextMenu(selectionModel.selectedItems)
            selectionModel.selectedItems.onChange { contextMenu = contextMenu(it.list) }

            bindSelected(componentProperty)

            CoroutineScope.launch { this@ComponentExplorer.refresh() }
        }
        vbox { dynamicContent(componentProperty) { it?.let { with(it) { config(false) } } } }
    }

    suspend fun refresh() {
        HttpClient.get("http://localhost:8080/component/").body<List<Component>>().also { withContext(Dispatchers.Main) { componentsProperty.setAll(it) } }
    }
}
