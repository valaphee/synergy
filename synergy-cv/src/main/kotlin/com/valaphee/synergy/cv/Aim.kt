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

package com.valaphee.synergy.cv

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.math.IntMath
import com.valaphee.foundry.math.Double2
import com.valaphee.foundry.math.Int2
import com.valaphee.foundry.math.Int4
import com.valaphee.synergy.cv.extractor.Extractor
import com.valaphee.synergy.cv.processsor.Processor
import com.valaphee.synergy.mouse.HidMouse
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import nu.pattern.OpenCV
import org.opencv.core.CvType
import org.opencv.core.Mat
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

/**
 * @author Kevin Ludwig
 */
class Aim(
    id: UUID,
    scripts: List<URL>,
    sensitivity: Float,
    epsilon: Int,
    @get:JsonProperty("view") val view: Int4,
    @get:JsonProperty("processors") val processors: List<Processor>,
    @get:JsonProperty("extractor") val extractor: Extractor
) : HidMouse(id, scripts, sensitivity, epsilon) {
    private var running = false

    suspend fun start() {
        require(!running)

        coroutineScope {
            launch {
                val image = Mat(view.w, view.z, CvType.CV_8UC3)
                val centerFloat = Double2(view.z / 2.0, view.w / 2.0)
                val centerInt = centerFloat.toInt2()

                running = true
                while (running) {
                    val _image = robot.createScreenCapture(Rectangle(view.x, view.y, view.z, view.w))
                    image.put(0, 0, (BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(_image.width, _image.height), false, null).apply {
                        createGraphics().apply {
                            drawImage(_image, 0, 0, null)
                            dispose()
                        }
                    }.raster.dataBuffer as DataBufferByte).data)

                    var processedImage = image
                    processors.forEach { processor -> processedImage = processor.process(processedImage) }
                    val extractedPoints = extractor.extract(processedImage)
                    extractedPoints.minByOrNull { it.distance2(centerFloat) }?.let {
                        val target = it.toInt2()
                        if (IntMath.pow(target.x - centerInt.x, 2) + IntMath.pow(target.y - centerInt.y, 2) > epsilon) mouseMoveRaw(Int2(target.x - centerInt.x, target.y - centerInt.y))
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
