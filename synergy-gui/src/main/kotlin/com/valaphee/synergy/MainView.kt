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

package com.valaphee.synergy

import com.valaphee.synergy.component.Component
import com.valaphee.synergy.component.ComponentAddView
import com.valaphee.synergy.keyboard.HidKeyboard
import com.valaphee.synergy.keyboard.RobotKeyboard
import com.valaphee.synergy.mouse.HidMouse
import com.valaphee.synergy.mouse.RobotMouse
import com.valaphee.synergy.proxy.ProxyServer
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ContextMenu
import javafx.scene.control.ListCell
import javafx.scene.layout.Priority
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.JMetroStyleClass
import jfxtras.styles.jmetro.Style
import tornadofx.View
import tornadofx.action
import tornadofx.bindSelected
import tornadofx.drawer
import tornadofx.hbox
import tornadofx.item
import tornadofx.listview
import tornadofx.menu
import tornadofx.menubar
import tornadofx.onChange
import tornadofx.vbox
import tornadofx.vgrow

/**
 * @author Kevin Ludwig
 */
class MainView : View("Synergy") {
    private val components = SimpleListProperty<Component>()
    private val component = SimpleObjectProperty<Component?>()

    override val root = vbox {
        JMetro(this, Style.DARK)
        styleClass.add(JMetroStyleClass.BACKGROUND)
        setPrefSize(1000.0, 800.0)

        menubar {
            menu("File") { item("Exit") { action { close() } } }
            menu("Help") { item("About") { action { find<AboutView>().openModal(resizable = false) } } }
        }
        drawer {
            vgrow = Priority.ALWAYS

            item("Components", expanded = true) {
                hbox {
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

                            bindSelected(component)
                            selectionModel.selectedItems.onChange {
                                contextMenu = ContextMenu().apply {
                                    if (it.list.isEmpty()) {
                                        menu("Add") {
                                            item("Component") { action { ComponentAddView("Component", Component::class).openModal() } }
                                            item("Hid Keyboard") { action { ComponentAddView("Hid Keyboard", HidKeyboard::class).openModal() } }
                                            item("Java Robot Keyboard") { action { ComponentAddView("Java Robot Keyboard", RobotKeyboard::class).openModal() } }
                                            item("Hid Mouse") { action { ComponentAddView("Hid Mouse", HidMouse::class).openModal() } }
                                            item("Java Robot Mouse") { action { ComponentAddView("Java Robot Mouse", RobotMouse::class).openModal() } }
                                            item("Proxy") { action { ComponentAddView("Proxy", ProxyServer::class).openModal() } }
                                        }
                                    }
                                    else item("Remove")
                                }
                            }
                            if (components.isEmpty()) contextMenu = ContextMenu().apply {
                                menu("Add") {
                                    item("Component") { action { ComponentAddView("Component", Component::class).openModal() } }
                                    item("Hid Keyboard") { action { ComponentAddView("Hid Keyboard", HidKeyboard::class).openModal() } }
                                    item("Java Robot Keyboard") { action { ComponentAddView("Java Robot Keyboard", RobotKeyboard::class).openModal() } }
                                    item("Hid Mouse") { action { ComponentAddView("Hid Mouse", HidMouse::class).openModal() } }
                                    item("Java Robot Mouse") { action { ComponentAddView("Java Robot Mouse", RobotMouse::class).openModal() } }
                                    item("Proxy") { action { ComponentAddView("Proxy", ProxyServer::class).openModal() } }
                                }
                            } else selectionModel.selectLast()
                        })
                    }
                }
            }
        }
    }
}
