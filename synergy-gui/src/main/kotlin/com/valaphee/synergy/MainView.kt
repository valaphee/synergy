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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.valaphee.synergy.component.Component
import com.valaphee.synergy.component.ComponentAddView
import com.valaphee.synergy.keyboard.HidKeyboard
import com.valaphee.synergy.keyboard.RobotKeyboard
import com.valaphee.synergy.mouse.HidMouse
import com.valaphee.synergy.mouse.RobotMouse
import com.valaphee.synergy.proxy.ProxyServer
import com.valaphee.synergy.proxy.mcbe.McbeProxy
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.scene.control.ContextMenu
import javafx.scene.control.ListCell
import javafx.scene.layout.Priority
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.JMetroStyleClass
import jfxtras.styles.jmetro.Style
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import tornadofx.toObservable
import tornadofx.vbox
import tornadofx.vgrow

/**
 * @author Kevin Ludwig
 */
class MainView : View("Synergy"), CoroutineScope {
    override val coroutineContext = SupervisorJob() + Dispatchers.IO

    private val components = SimpleListProperty(mutableListOf<Component>().toObservable())
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

                            fun contextMenu(selectedComponents: ObservableList<out Component>) = ContextMenu().apply {
                                if (selectedComponents.isEmpty()) {
                                    menu("Add") {
                                        item("Component") { action { ComponentAddView("Component", Component()).openModal() } }
                                        menu("Input") {
                                            item("HID Keyboard") { action { ComponentAddView("Hid Keyboard", HidKeyboard()).openModal() } }
                                            item("Java Robot Keyboard") { action { ComponentAddView("Java Robot Keyboard", RobotKeyboard()).openModal() } }
                                            item("HID Mouse") { action { ComponentAddView("Hid Mouse", HidMouse()).openModal() } }
                                            item("Java Robot Mouse") { action { ComponentAddView("Java Robot Mouse", RobotMouse()).openModal() } }
                                        }
                                        menu("Proxy") { item("Minecraft") { action { ComponentAddView("Minecraft Proxy", ProxyServer(proxy = McbeProxy())).openModal() } } }
                                    }
                                }
                                else item("Remove") {
                                    action {
                                        launch(Dispatchers.Main) {
                                            selectedComponents.map { launch { HttpClient.delete("http://localhost:8080/component/${it.id}") } }.joinAll()

                                            refresh()
                                        }
                                    }
                                }
                            }

                            bindSelected(component)
                            contextMenu = contextMenu(selectionModel.selectedItems)
                            selectionModel.selectedItems.onChange { contextMenu = contextMenu(it.list) }

                            refresh()
                        })
                    }
                }
            }
        }
    }

    fun refresh() {
        launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) { HttpClient.get("http://localhost:8080/component/").body<List<Component>>() }.also {
                components.clear()
                components.addAll(it)
            }
        }
    }

    companion object {
        val HttpClient = HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(jacksonObjectMapper())) }
        }
    }
}
