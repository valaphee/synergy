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

import com.valaphee.synergy.ngdp.tank.Data
import com.valaphee.synergy.ngdp.tank.DataReader
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import javafx.scene.control.ButtonBar
import tornadofx.FileChooserMode
import tornadofx.action
import tornadofx.button
import tornadofx.chooseFile
import java.io.File

/**
 * @author Kevin Ludwig
 */
class Data0B2SFX(
    val data: ByteArray
) : Data {
    override fun ButtonBar.onShowButtonBar() {
        button("Export Ogg") {
            action {
                chooseFile(filters = emptyArray(), mode = FileChooserMode.Save).singleOrNull()?.let {
                    val file = File.createTempFile("tmp", ".wem")
                    file.writeBytes(data)

                    Runtime.getRuntime().exec("""tools/ww2ogg024/ww2ogg "${file.path}" -o "${it.path}" --pcb "tools/ww2ogg024/packed_codebooks_aoTuV_603.bin"""").waitFor()
                    Runtime.getRuntime().exec("""tools/ReVorb "${it.path}"""").waitFor()

                    file.delete()
                }
            }
        }
    }
}

/**
 * @author Kevin Ludwig
 */
object Data0B2SFXReader : DataReader {
    override fun read(buffer: ByteBuf) = Data0B2SFX(ByteBufUtil.getBytes(buffer))
}
