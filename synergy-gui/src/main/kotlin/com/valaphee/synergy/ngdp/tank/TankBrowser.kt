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

package com.valaphee.synergy.ngdp.tank

import com.valaphee.synergy.ngdp.Config
import com.valaphee.synergy.ngdp.Table
import com.valaphee.synergy.ngdp.TypedTable
import com.valaphee.synergy.ngdp.blte.BlteInputStream
import com.valaphee.synergy.ngdp.blte.KeepAliveInputStream
import com.valaphee.synergy.ngdp.casc.Casc
import com.valaphee.synergy.ngdp.tact.Encoding
import com.valaphee.synergy.ngdp.tank.data.Data004TextureReader
import com.valaphee.synergy.ngdp.tank.data.Data04DTexturePayloadReader
import com.valaphee.synergy.ngdp.tank.data.Data0B2SFXReader
import com.valaphee.synergy.ngdp.util.Key
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import javafx.beans.binding.Binding
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Label
import javafx.scene.control.TableColumnBase
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import tornadofx.FileChooserMode
import tornadofx.Fragment
import tornadofx.action
import tornadofx.bindSelected
import tornadofx.button
import tornadofx.buttonbar
import tornadofx.chooseDirectory
import tornadofx.chooseFile
import tornadofx.column
import tornadofx.combobox
import tornadofx.dynamicContent
import tornadofx.hbox
import tornadofx.hgrow
import tornadofx.objectBinding
import tornadofx.onChange
import tornadofx.splitpane
import tornadofx.style
import tornadofx.tableview
import tornadofx.textarea
import tornadofx.textfield
import tornadofx.toObservable
import tornadofx.toProperty
import tornadofx.vbox
import tornadofx.vgrow
import java.io.File

/**
 * @author Kevin Ludwig
 */
class TankBrowser : Fragment("Tank Browser") {
    private lateinit var storage: Casc
    private lateinit var encoding: Encoding
    private lateinit var manifest: ContentManifest

    private val pathProperty = "".toProperty().apply {
        onChange {
            storage = Casc(File(it, "data/casc/data"))

            val buildInfo = TypedTable(File(it, ".build.info").readText())
            val buildKey = checkNotNull(buildInfo[0]["Build Key"])
            val buildConfig = Config(File(it, "data/casc/config/${buildKey.substring(0, 2)}/${buildKey.substring(2, 4)}/$buildKey").readText())

            encoding = BlteInputStream(KeepAliveInputStream(checkNotNull(checkNotNull(storage[Key(checkNotNull(buildConfig["encoding"])[1])]).inputStream))).use { Encoding(it) }

            manifestsProperty.clear()
            manifestsProperty.addAll(BlteInputStream(KeepAliveInputStream(checkNotNull(checkNotNull(storage[checkNotNull(encoding.getEKeysOrNull(Key(checkNotNull(buildConfig["root"])[0])))[0]]).inputStream))).use { Table(it.readAllBytes().decodeToString()) }.entries.map { checkNotNull(it["FILENAME"]) to checkNotNull(it["MD5"]) }.filter { it.first.endsWith(".cmf") })
        }
    }
    private val manifestsProperty = SimpleListProperty(mutableListOf<Pair<String, String>>().toObservable())

    private val dataProperty = SimpleListProperty(mutableListOf<ContentManifest.Data>().toObservable())
    private val dataEntryProperty = SimpleObjectProperty<ContentManifest.Data>()

    private val manifestProperty = SimpleObjectProperty<Pair<String, String>>()
    private val manifestBinding: Binding<ContentManifest?> = manifestProperty.objectBinding { BlteInputStream(KeepAliveInputStream(checkNotNull(checkNotNull(storage[checkNotNull(encoding.getEKeysOrNull(Key(checkNotNull(it!!.second))))[0]]).inputStream))).use { stream -> ContentManifest(checkNotNull(it.first).split('/').last(), stream) } }.apply {
        onChange {
            usagesProperty.clear()
            usagesProperty.add(null)
            usagesProperty.addAll(it!!.data.map { it.guid.usage }.distinct().sorted())
            typesProperty.clear()
            typesProperty.add(null)
            typesProperty.addAll(it.data.map { it.guid.type }.distinct().sorted())
            platformsProperty.clear()
            platformsProperty.add(null)
            platformsProperty.addAll(it.data.map { it.guid.platform }.distinct().sorted())
            regionsProperty.clear()
            regionsProperty.add(null)
            regionsProperty.addAll(it.data.map { it.guid.region }.distinct().sorted())
            localesProperty.clear()
            localesProperty.add(null)
            localesProperty.addAll(it.data.map { it.guid.locale }.distinct().sorted())

            refresh()
        }
    }

    private val usagesProperty = SimpleListProperty(mutableListOf<Int?>().toObservable())
    private val usageProperty: SimpleObjectProperty<Int?> = SimpleObjectProperty<Int?>().apply { onChange { refresh() } }
    private val typesProperty = SimpleListProperty(mutableListOf<Int?>().toObservable())
    private val typeProperty: SimpleObjectProperty<Int?> = SimpleObjectProperty<Int?>().apply { onChange { refresh() } }
    private val platformsProperty = SimpleListProperty(mutableListOf<Int?>().toObservable())
    private val platformProperty: SimpleObjectProperty<Int?> = SimpleObjectProperty<Int?>().apply { onChange { refresh() } }
    private val regionsProperty = SimpleListProperty(mutableListOf<Int?>().toObservable())
    private val regionProperty: SimpleObjectProperty<Int?> = SimpleObjectProperty<Int?>().apply { onChange { refresh() } }
    private val localesProperty = SimpleListProperty(mutableListOf<Int?>().toObservable())
    private val localeProperty: SimpleObjectProperty<Int?> = SimpleObjectProperty<Int?>().apply { onChange { refresh() } }

    override val root = vbox {
        hbox {
            textfield(pathProperty) { hgrow = Priority.ALWAYS }
            button("...") { action { chooseDirectory()?.let { pathProperty.value = it.absolutePath } } }
        }
        combobox(manifestProperty, values = manifestsProperty) {
            maxWidth = Double.MAX_VALUE

            cellFormat { text = it.first }
        }
        splitpane {
            vgrow = Priority.ALWAYS

            vbox {
                tableview(dataProperty) {
                    vgrow = Priority.ALWAYS
                    placeholder = Label("")

                    style { font = Font.font("monospaced", 8.0) }

                    column<ContentManifest.Data, String>("U") { String.format("%01X", it.value.guid.usage).toProperty() }.apply { tableColumnBaseSetWidth(this, 45.0) }
                    column<ContentManifest.Data, String>("T") { String.format("%03X", it.value.guid.type).toProperty() }.apply { tableColumnBaseSetWidth(this, 60.0) }
                    column<ContentManifest.Data, String>("P") { String.format("%01X", it.value.guid.platform).toProperty() }.apply { tableColumnBaseSetWidth(this, 45.0) }
                    column<ContentManifest.Data, String>("R") { String.format("%02X", it.value.guid.region).toProperty() }.apply { tableColumnBaseSetWidth(this, 45.0) }
                    column<ContentManifest.Data, String>("L") { String.format("%02X", it.value.guid.locale).toProperty() }.apply { tableColumnBaseSetWidth(this, 45.0) }
                    column<ContentManifest.Data, String>("I") { String.format("%08X", it.value.guid.id).toProperty() }.apply { tableColumnBaseSetWidth(this, 100.0) }

                    bindSelected(dataEntryProperty)
                }
                hbox {
                    combobox(usageProperty, values = usagesProperty) { cellFormat { text = String.format("%01X", it) } }
                    combobox(typeProperty, values = typesProperty) { cellFormat { text = String.format("%03X", it) } }
                    combobox(platformProperty, values = platformsProperty) { cellFormat { text = String.format("%01X", it) } }
                    combobox(regionProperty, values = regionsProperty) { cellFormat { text = String.format("%02X", it) } }
                    combobox(localeProperty, values = localesProperty) { cellFormat { text = String.format("%02X", it) } }
                }
            }
            vbox {
                dynamicContent(dataEntryProperty) {
                    it?.let {
                        encoding.getEKeysOrNull(it.cKey)?.let { eKey ->
                            BlteInputStream(KeepAliveInputStream(checkNotNull(checkNotNull(storage[eKey[0]]).inputStream))).use { stream ->
                                val bytes = stream.readAllBytes()
                                val buffer = Unpooled.wrappedBuffer(bytes)
                                dataReaders[it.guid.type]?.let {
                                    with(it.read(buffer)) {
                                        onShow()

                                        buttonbar {
                                            button("Save") { action { chooseFile(filters = emptyArray(), mode = FileChooserMode.Save).singleOrNull()?.writeBytes(bytes) } }
                                            button("Replace")

                                            onShowButtonBar()
                                        }
                                    }
                                } ?: run {
                                    textarea(ByteBufUtil.prettyHexDump(buffer)) {
                                        vgrow = Priority.ALWAYS

                                        style { font = Font.font("monospaced") }
                                    }
                                    buttonbar {
                                        button("Save") { action { chooseFile(filters = emptyArray(), mode = FileChooserMode.Save).singleOrNull()?.writeBytes(bytes) } }
                                        button("Replace")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun refresh() {
        val usage = usageProperty.value
        val type = typeProperty.value
        val platform = platformProperty.value
        val region = regionProperty.value
        val locale = localeProperty.value
        dataProperty.setAll(manifestBinding.value!!.data.filter {
            val guid = it.guid
            usage?.let { guid.usage == it } ?: true && type?.let { guid.type == it } ?: true && platform?.let { guid.platform == it } ?: true && region?.let { guid.region == it } ?: true && locale?.let { guid.locale == it } ?: true
        })
    }

    companion object {
        private val tableColumnBaseSetWidth = TableColumnBase::class.java.getDeclaredMethod("setWidth", Double::class.java).apply { isAccessible = true }
        private val dataReaders = mapOf(
            0x004 to Data004TextureReader,
            0x04D to Data04DTexturePayloadReader,
            0x0B2 to Data0B2SFXReader,
        )
    }
}
