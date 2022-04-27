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

import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory
import javafx.scene.image.Image
import tornadofx.App
import tornadofx.launch
import kotlin.system.exitProcess

/**
 * @author Kevin Ludwig
 */
class Main : App(Image(Main::class.java.getResourceAsStream("/app.png")), MainView::class)

fun main(arguments: Array<String>) {
    SvgImageLoaderFactory.install()

    launch<Main>(arguments)

    exitProcess(0)
}
