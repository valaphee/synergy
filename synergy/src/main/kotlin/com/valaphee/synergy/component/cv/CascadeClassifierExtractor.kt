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

package com.valaphee.synergy.component.cv

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.valaphee.foundry.math.Double2
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Size
import org.opencv.objdetect.CascadeClassifier
import org.opencv.objdetect.Objdetect
import java.io.File
import kotlin.math.round

/**
 * @author Kevin Ludwig
 */
class CascadeClassifierExtractor(
    @get:JsonProperty("file") val file: File
) : Extractor {
    @JsonIgnore private val cascadeClassifier = CascadeClassifier(file.toString())

    override fun extract(image: Mat): List<Double2> {
        val objects = MatOfRect()
        cascadeClassifier.detectMultiScale(image, objects, 1.05, 10, Objdetect.CASCADE_SCALE_IMAGE, Size(round(image.rows() * 0.05f).toDouble(), round(image.rows() * 0.05f).toDouble()), Size())
        return objects.toList().map { Double2(it.x + (it.width / 2.0), it.y + (it.height / 2.0)) }
    }
}
