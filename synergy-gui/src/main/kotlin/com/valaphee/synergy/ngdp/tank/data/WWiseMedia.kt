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

package com.valaphee.synergy.ngdp.tank.data

import javafx.event.EventTarget
import javafx.stage.FileChooser
import tornadofx.FileChooserMode
import tornadofx.action
import tornadofx.button
import tornadofx.buttonbar
import tornadofx.chooseFile
import java.io.File

/**
 * @author Kevin Ludwig
 */
class WWiseMedia(
    val bytes: ByteArray
) : Data {
    override fun EventTarget.preview() {
        buttonbar {
            button("Save") {
                action {
                    chooseFile(filters = arrayOf(FileChooser.ExtensionFilter("All Files", "*.*"), FileChooser.ExtensionFilter("Ogg Vorbis Audio", "*.ogg"), FileChooser.ExtensionFilter("Wwise Encoded Media", "*.wem")), mode = FileChooserMode.Save).singleOrNull()?.let {
                        when (it.extension) {
                            "ogg" -> {
                                val wemFile = File.createTempFile("tmp", ".wem")
                                wemFile.writeBytes(bytes)
                                Runtime.getRuntime().exec("""tools/ww2ogg024/ww2ogg "${wemFile.path}" -o "${it.path}" --pcb "tools/ww2ogg024/packed_codebooks_aoTuV_603.bin"""").waitFor()
                                Runtime.getRuntime().exec("""tools/ReVorb "${it.path}"""").waitFor()
                                wemFile.delete()
                            }
                            else -> it.writeBytes(bytes)
                        }
                    }
                }
            }
        }
    }
}

/**
 * @author Kevin Ludwig
 */
object WWiseMediaReader : DataReader {
    override fun read(bytes: ByteArray) = WWiseMedia(bytes)
}
