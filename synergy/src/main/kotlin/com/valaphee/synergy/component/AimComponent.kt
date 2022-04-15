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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.valaphee.foundry.math.Double2
import com.valaphee.foundry.math.Int4
import com.valaphee.synergy.component.cv.Extractor
import com.valaphee.synergy.component.cv.Processor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import nu.pattern.OpenCV
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.highgui.HighGui
import java.awt.Color
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer
import java.awt.image.DataBufferByte
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * @author Kevin Ludwig
 */
class AimComponent(
    id: UUID,
    _controller: List<URL>,
    sensitivity: Float,
    @get:JsonProperty("area") val area: Int4,
    @get:JsonProperty("processors") val processors: List<Processor>,
    @get:JsonProperty("extractor") val extractor: Extractor
) : MouseComponent(id, _controller, sensitivity, 0), CoroutineScope {
    @get:JsonIgnore override val coroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher() + SupervisorJob()

    var active = false

    private var running = false
    private var processorPreview: List<JLabel>? = null
    private var extractorPreview: JLabel? = null

    fun enablePreview() {
        check(processorPreview == null)

        val jFrame = JFrame().apply { isVisible = true }
        val jPanel = JPanel().apply { jFrame.add(this) }
        processorPreview = processors.map { JLabel().apply { jPanel.add(this) } }
        extractorPreview = JLabel().apply { jPanel.add(this) }
    }

    fun start() {
        launch {
            val image = Mat(area.w, area.z, CvType.CV_8UC3)
            val centerFloat = Double2(area.z / 2.0, area.w / 2.0)
            val centerInt = centerFloat.toInt2()
            /*val values = EvictingQueue.create<Int2>(1)*/

            running = true
            while (running) {
                val _image = robot.createScreenCapture(Rectangle(area.x, area.y, area.z, area.w))
                image.put(0, 0, (BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(_image.width, _image.height), false, null).apply {
                    createGraphics().apply {
                        drawImage(_image, 0, 0, null)
                        dispose()
                    }
                }.raster.dataBuffer as DataBufferByte).data)

                var processedImage = image
                processors.forEachIndexed { i, processor ->
                    processedImage = processor.process(processedImage)
                    processorPreview?.let { it[i].icon = ImageIcon(HighGui.toBufferedImage(processedImage)) }
                }
                val extractedPoints = extractor.extract(processedImage)
                extractorPreview?.let {
                    it.icon = ImageIcon((HighGui.toBufferedImage(image) as BufferedImage).apply {
                        createGraphics().apply {
                            extractedPoints.forEach {
                                color = Color.GREEN
                                drawRect(it.x.toInt() - 1, it.y.toInt() - 1, 3, 3)
                            }
                        }
                    })
                }
                extractedPoints.minByOrNull { it.distance2(centerFloat) }?.let {
                    /*values += it.toInt2()*/
                    if (active) {
                        /*var x = 0
                        var y = 0
                        values.forEach {
                            x += it.x
                            y += it.y
                        }*/
                        mouseMoveRaw(/*Int2(x / values.size, y / values.size)*/it.toInt2() - centerInt)
                    }
                }
            }
        }
    }

    fun stop() {
        running = false
    }

    companion object {
        private val robot = Robot()
        private val colorModel = ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), intArrayOf(8, 8, 8), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE)

        init {
            OpenCV.loadLocally()
        }
    }
}
