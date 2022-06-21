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

import com.google.inject.Guice
import com.valaphee.synergy.module.ComponentExplorer
import com.valaphee.synergy.ngdp.tank.TankBrowser
import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory
import javafx.geometry.Side
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.JMetroStyleClass
import jfxtras.styles.jmetro.Style
import tornadofx.App
import tornadofx.DIContainer
import tornadofx.FX
import tornadofx.View
import tornadofx.action
import tornadofx.item
import tornadofx.launch
import tornadofx.menu
import tornadofx.menubar
import tornadofx.tab
import tornadofx.tabpane
import tornadofx.vbox
import tornadofx.vgrow
import kotlin.reflect.KClass
import kotlin.system.exitProcess

/**
 * @author Kevin Ludwig
 */
class Main : View("Synergy") {
    override val root = vbox {
        JMetro(this, Style.DARK)
        styleClass.add(JMetroStyleClass.BACKGROUND)

        // Properties
        setPrefSize(1000.0, 800.0)

        // Children
        menubar {
            menu("File") { item("Exit") { action { close() } } }
            menu("Help") { item("About") { action { find<About>().openModal(resizable = false) } } }
        }
        tabpane {
            // Parent Properties
            vgrow = Priority.ALWAYS

            // Properties
            side = Side.LEFT
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

            // Children
            tab(ComponentExplorer::class)
            tab(TankBrowser::class)
        }
        /*borderpane {
            // Properties
            vgrow = Priority.ALWAYS

            // Children
            center = listmenu(theme = "blue") {
                item("Component Explorer") {
                    whenSelected {
                        this@vbox.children -= this@borderpane
                        this@vbox.children += find<ComponentExplorer>().root
                    }
                }
                item("Tank Browser") {
                    whenSelected {
                        this@vbox.children -= this@borderpane
                        this@vbox.children += find<TankBrowser>().root
                    }
                }
            }
        }*/
    }
}

/**
 * @author Kevin Ludwig
 */
class MainApp : App(Image(MainApp::class.java.getResourceAsStream("/app.png")), Main::class)

fun main(arguments: Array<String>) {
    SvgImageLoaderFactory.install()

    FX.dicontainer = object : DIContainer {
        private val injector = Guice.createInjector()

        override fun <T : Any> getInstance(type: KClass<T>) = injector.getInstance(type.java)
    }

    launch<MainApp>(arguments)

    exitProcess(0)
}
