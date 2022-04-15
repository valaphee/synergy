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

package com.valaphee.synergy.component.aim

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.EvictingQueue
import com.valaphee.foundry.math.Double2
import com.valaphee.foundry.math.Int2
import com.valaphee.foundry.math.Int4
import com.valaphee.synergy.component.MouseComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
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
import java.util.concurrent.Executors

/**
 * @author Kevin Ludwig
 */
class AimComponent(
    id: UUID,
    _controller: List<URL>,
    sensitivity: Float,
    @get:JsonProperty("area") val area: Int4,
    @get:JsonProperty("processor") val processor: Processor
) : MouseComponent(id, _controller, sensitivity, 0), CoroutineScope {
    @get:JsonIgnore override val coroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher() + SupervisorJob()

    private var running = false

    fun start() {
        launch {
            val image = Mat(area.w, area.z, CvType.CV_8UC3)
            val centerD = Double2(area.z / 2.0, area.w / 2.0)
            val centerI = centerD.toInt2()
            val values = EvictingQueue.create<Int2>(1)

            running = true
            while (running) {
                val _image = robot.createScreenCapture(Rectangle(area.x, area.y, area.z, area.w))
                image.put(0, 0, (BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(_image.width, _image.height), false, null).apply {
                    createGraphics().apply {
                        drawImage(_image, 0, 0, null)
                        dispose()
                    }
                }.raster.dataBuffer as DataBufferByte).data)

                processor.process(image).minByOrNull { it.distance2(centerD) }?.let {
                    values += it.toInt2()
                    var x = 0
                    var y = 0
                    values.forEach {
                        x += it.x
                        y += it.y
                    }
                    mouseMoveRaw(Int2(x / values.size, y / values.size) - centerI)
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
