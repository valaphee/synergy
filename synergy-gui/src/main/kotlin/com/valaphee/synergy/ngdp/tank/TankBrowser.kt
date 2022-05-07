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
import com.valaphee.synergy.ngdp.blte.util.KeepAliveInputStream
import com.valaphee.synergy.ngdp.casc.Casc
import com.valaphee.synergy.ngdp.tact.Encoding
import com.valaphee.synergy.ngdp.tank.data.WWiseMediaReader
import com.valaphee.synergy.ngdp.tank.data.teTexturePayloadReader
import com.valaphee.synergy.ngdp.tank.data.teTextureReader
import com.valaphee.synergy.ngdp.util.Key
import com.valaphee.synergy.ngdp.util.asHexStringToByteArray
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.transformation.FilteredList
import javafx.scene.control.Label
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.stage.FileChooser
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
import tornadofx.fixedWidth
import tornadofx.hbox
import tornadofx.hgrow
import tornadofx.onChange
import tornadofx.smartResize
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
    /*private lateinit var data: List<ContentManifest.Data>*/

    private val pathProperty = "".toProperty()
    private val manifestsProperty = SimpleListProperty(mutableListOf<Pair<String, String>>().toObservable())
    private val manifestProperty = SimpleObjectProperty<Pair<String, String>>().apply {
        onChange {
            val manifest = BlteInputStream(KeepAliveInputStream(checkNotNull(checkNotNull(storage[checkNotNull(encoding.getEKeysOrNull(Key(checkNotNull(it!!.second))))[0]]).inputStream))).use { stream -> ContentManifest(checkNotNull(it.first).split('/').last(), stream) }

            dataProperty.setAll(manifest.data)
            usagesProperty.clear()
            usagesProperty.add(null)
            usagesProperty.addAll(manifest.data.map { it.guid.usage }.distinct().sorted())
            typesProperty.clear()
            typesProperty.add(null)
            typesProperty.addAll(manifest.data.map { it.guid.type }.distinct().sorted())
            platformsProperty.clear()
            platformsProperty.add(null)
            platformsProperty.addAll(manifest.data.map { it.guid.platform }.distinct().sorted())
            regionsProperty.clear()
            regionsProperty.add(null)
            regionsProperty.addAll(manifest.data.map { it.guid.region }.distinct().sorted())
            localesProperty.clear()
            localesProperty.add(null)
            localesProperty.addAll(manifest.data.map { it.guid.locale }.distinct().sorted())

            updateDataPredicate()
        }
    }

    private val dataProperty = SimpleListProperty(mutableListOf<ContentManifest.Data>().toObservable())

    private val _dataProperty = FilteredList(dataProperty)
    private val usagesProperty = SimpleListProperty(mutableListOf<Int?>().toObservable())
    private val usageProperty: SimpleObjectProperty<Int?> = SimpleObjectProperty<Int?>().apply { onChange { updateDataPredicate() } }
    private val typesProperty = SimpleListProperty(mutableListOf<Int?>().toObservable())
    private val typeProperty: SimpleObjectProperty<Int?> = SimpleObjectProperty<Int?>().apply { onChange { updateDataPredicate() } }
    private val platformsProperty = SimpleListProperty(mutableListOf<Int?>().toObservable())
    private val platformProperty: SimpleObjectProperty<Int?> = SimpleObjectProperty<Int?>().apply { onChange { updateDataPredicate() } }
    private val regionsProperty = SimpleListProperty(mutableListOf<Int?>().toObservable())
    private val regionProperty: SimpleObjectProperty<Int?> = SimpleObjectProperty<Int?>().apply { onChange { updateDataPredicate() } }
    private val localesProperty = SimpleListProperty(mutableListOf<Int?>().toObservable())
    private val localeProperty: SimpleObjectProperty<Int?> = SimpleObjectProperty<Int?>().apply { onChange { updateDataPredicate() } }

    private val dataEntryProperty = SimpleObjectProperty<ContentManifest.Data>()

    override val root = vbox {
        // Properties
        vgrow = Priority.ALWAYS

        // Children
        hbox {
            button("Load") {
                action {
                    val path = pathProperty.value
                    storage = Casc(File(path, "data/casc/data"))
                    val buildInfo = TypedTable(File(path, ".build.info").readText())
                    val buildKey = checkNotNull(buildInfo[0]["Build Key"])
                    val buildConfig = Config(File(path, "data/casc/config/${buildKey.substring(0, 2)}/${buildKey.substring(2, 4)}/$buildKey").readText())
                    encoding = BlteInputStream(KeepAliveInputStream(checkNotNull(checkNotNull(storage[Key(checkNotNull(buildConfig["encoding"])[1])]).inputStream))).use { Encoding(it) }
                    manifestsProperty.setAll(BlteInputStream(KeepAliveInputStream(checkNotNull(checkNotNull(storage[checkNotNull(encoding.getEKeysOrNull(Key(checkNotNull(buildConfig["root"])[0])))[0]]).inputStream))).use { Table(it.readAllBytes().decodeToString()) }.entries.map { checkNotNull(it["FILENAME"]) to checkNotNull(it["MD5"]) }.filter { it.first.endsWith(".cmf") })
                    /*data = manifestsProperty.flatMap { BlteInputStream(KeepAliveInputStream(checkNotNull(checkNotNull(storage[checkNotNull(encoding.getEKeysOrNull(Key(checkNotNull(it!!.second))))[0]]).inputStream))).use { stream -> ContentManifest(checkNotNull(it.first).split('/').last(), stream) }.data }

                    usagesProperty.clear()
                    usagesProperty.add(null)
                    usagesProperty.addAll(data.map { it.guid.usage }.distinct().sorted())
                    typesProperty.clear()
                    typesProperty.add(null)
                    typesProperty.addAll(data.map { it.guid.type }.distinct().sorted())
                    platformsProperty.clear()
                    platformsProperty.add(null)
                    platformsProperty.addAll(data.map { it.guid.platform }.distinct().sorted())
                    regionsProperty.clear()
                    regionsProperty.add(null)
                    regionsProperty.addAll(data.map { it.guid.region }.distinct().sorted())
                    localesProperty.clear()
                    localesProperty.add(null)
                    localesProperty.addAll(data.map { it.guid.locale }.distinct().sorted())
                    refresh()*/
                }
            }
            textfield(pathProperty) { hgrow = Priority.ALWAYS }
            button("...") { action { chooseDirectory()?.let { pathProperty.value = it.absolutePath } } }
        }
        combobox(manifestProperty, values = manifestsProperty) {
            maxWidth = Double.MAX_VALUE

            cellFormat { text = it.first }
        }
        hbox {
            vgrow = Priority.ALWAYS

            vbox {
                tableview(_dataProperty) {
                    vgrow = Priority.ALWAYS
                    placeholder = Label("")

                    style { font = Font.font("monospaced", 8.0) }

                    column<ContentManifest.Data, String>("U") { String.format("%01X", it.value.guid.usage).toProperty() }.apply { fixedWidth(45.0) }
                    column<ContentManifest.Data, String>("T") { String.format("%03X", it.value.guid.type).toProperty() }.apply { fixedWidth(60.0) }
                    column<ContentManifest.Data, String>("P") { String.format("%01X", it.value.guid.platform).toProperty() }.apply { fixedWidth(45.0) }
                    column<ContentManifest.Data, String>("R") { String.format("%02X", it.value.guid.region).toProperty() }.apply { fixedWidth(45.0) }
                    column<ContentManifest.Data, String>("L") { String.format("%02X", it.value.guid.locale).toProperty() }.apply { fixedWidth(45.0) }
                    column<ContentManifest.Data, String>("I") { String.format("%08X", it.value.guid.id).toProperty() }.apply { fixedWidth(100.0) }
                    smartResize()

                    bindSelected(dataEntryProperty)
                }
                hbox {
                    combobox(usageProperty, values = usagesProperty) { cellFormat { text = String.format("%01X", it) } }
                    combobox(typeProperty, values = typesProperty) { cellFormat { text = String.format("%03X${typeNameById[it]?.let { " - $it" } ?: ""}", it) } }
                    combobox(platformProperty, values = platformsProperty) { cellFormat { text = String.format("%01X", it) } }
                    combobox(regionProperty, values = regionsProperty) { cellFormat { text = String.format("%02X", it) } }
                    combobox(localeProperty, values = localesProperty) { cellFormat { text = String.format("%02X", it) } }
                }
            }
            vbox {
                hgrow = Priority.ALWAYS

                dynamicContent(dataEntryProperty) {
                    it?.let {
                        encoding.getEKeysOrNull(it.cKey)?.let { eKey ->
                            BlteInputStream(KeepAliveInputStream(checkNotNull(checkNotNull(storage[eKey[0]]).inputStream)), keyring).use { stream ->
                                val bytes = stream.readAllBytes()
                                dataReaders[it.guid.type]?.read(bytes)?.show(this) ?: run {
                                    textarea(ByteBufUtil.prettyHexDump(Unpooled.wrappedBuffer(bytes))) {
                                        vgrow = Priority.ALWAYS

                                        style { font = Font.font("monospaced", 10.0) }
                                    }
                                    buttonbar { button("Save") { action { chooseFile(filters = arrayOf(FileChooser.ExtensionFilter("All Files", "*.*")), mode = FileChooserMode.Save).singleOrNull()?.writeBytes(bytes) } } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateDataPredicate() {
        val usage = usageProperty.value
        val type = typeProperty.value
        val platform = platformProperty.value
        val region = regionProperty.value
        val locale = localeProperty.value
        _dataProperty.setPredicate {
            val guid = it.guid
            usage?.let { guid.usage == it } ?: true && type?.let { guid.type == it } ?: true && platform?.let { guid.platform == it } ?: true && region?.let { guid.region == it } ?: true && locale?.let { guid.locale == it } ?: true
        }
    }

    companion object {
        private val typeNameById = mapOf(
            0x003 to "STUEntityDefinition",
            0x004 to "teTexture",
            0x008 to "teMaterial",
            0x015 to "STUAnimCategory",
            0x018 to "STUAnimBoneWeightMask",
            0x01A to "STUModelLook",
            0x01B to "STUStatescriptGraph",
            0x020 to "STUAnimBlendTree",
            0x021 to "STUAnimBlendTreeSet",
            0x02C to "STUSound",
            0x02D to "STUSoundSwitchGroup",
            0x02E to "STUSoundSwitch",
            0x02F to "STUSoundParameter",
            0x030 to "STUSoundStateGroup",
            0x031 to "STUSoundState",
            0x039 to "STUMapCatalog",
            0x03F to "WWise WEM",
            0x043 to "WWise BNk",
            0x04D to "teTexturePayload",
            0x051 to "STUFontFamily",
            0x055 to "STUSoundSpace",
            0x058 to "STUProgressionUnlocks",
            0x05A to "STUUXScreen",
            0x05E to "STUUXResourceDictionary",
            0x05F to "STUVoiceSet",
            0x062 to "STUStat",
            0x063 to "STUDCatalog",
            0x068 to "STUAchievement",
            0x070 to "STUVoiceLineSet",
            0x072 to "STULocaleSettings",
            0x075 to "STUHero",
            0x078 to "STUVoiceStimulus",
            0x079 to "STUVoiceCategory",
            0x090 to "STUResourceKey",
            0x097 to "STURaycastType",
            0x098 to "STURaycastReceiver",
            0x09E to "STULoadout",
            0x09F to "STUMapHeader",
            0x0A3 to "STUSoundIntegrity",
            0x0A5 to "STUUnlock",
            0x0A6 to "STUSkinTheme",
            0x0A8 to "STUEffectLook",
            0x0AA to "STUMapFont",
            0x0AC to "STUGamePadVibration",
            0x0AD to "STUHeroWeapon",
            0x0B2 to "WWise WEM",
            0x0B7 to "STUUXLogicalButton",
            0x0BB to "WWise WEM",
            0x0BF to "STULineupPose",
            0x0C0 to "STUGameRuleset",
            0x0C5 to "STUGameMode",
            0x0C6 to "STUGameRulesetSchema",
            0x0C8 to "STURankedSeason",
            0x0CE to "STUTeamColor",
            0x0CF to "STULootBox",
            0x0D0 to "STUVoiceConversation"
        )
        private val keyring = mapOf(Key("D7B2F07FE90A6E85") to "8A5F52A09DCB15B2EF76DA972885866C".asHexStringToByteArray())
        private val dataReaders = mapOf(
            0x004 to teTextureReader,
            0x03F to WWiseMediaReader,
            0x04D to teTexturePayloadReader,
            0x0B2 to WWiseMediaReader,
            0x0BB to WWiseMediaReader
        )
    }
}
