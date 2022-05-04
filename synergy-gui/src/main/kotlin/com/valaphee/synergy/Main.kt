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
import com.google.inject.Guice
import com.valaphee.synergy.component.Components
import com.valaphee.synergy.tank.Tank
import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.JMetroStyleClass
import jfxtras.styles.jmetro.Style
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import tornadofx.App
import tornadofx.DIContainer
import tornadofx.FX
import tornadofx.View
import tornadofx.action
import tornadofx.drawer
import tornadofx.item
import tornadofx.launch
import tornadofx.menu
import tornadofx.menubar
import tornadofx.vbox
import tornadofx.vgrow
import kotlin.reflect.KClass
import kotlin.system.exitProcess

val CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
val HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(jacksonObjectMapper())) }
}

/**
 * @author Kevin Ludwig
 */
class Main : View("Synergy") {
    override val root = vbox {
        setPrefSize(1000.0, 800.0)

        JMetro(this, Style.DARK)
        styleClass.add(JMetroStyleClass.BACKGROUND)

        menubar {
            menu("File") { item("Exit") { action { close() } } }
            menu("Help") { item("About") { action { find<About>().openModal(resizable = false) } } }
        }
        drawer {
            vgrow = Priority.ALWAYS

            item(Components(), true)
            item(Tank())
        }
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
