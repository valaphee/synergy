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

package com.valaphee.synergy.cv.processsor

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.valaphee.foundry.math.Int3
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

/**
 * @author Kevin Ludwig
 */
class InRangeProcessor(
    @get:JsonProperty("lower_bound") val lowerBound: Int3,
    @get:JsonProperty("upper_bound") val upperBound: Int3
) : Processor {
    @JsonIgnore private val _lowerBound = Scalar(lowerBound.x.toDouble(), lowerBound.y.toDouble(), lowerBound.z.toDouble())
    @JsonIgnore private val _upperBound = Scalar(upperBound.x.toDouble(), upperBound.y.toDouble(), upperBound.z.toDouble())

    override fun process(image: Mat): Mat {
        val processedImage = Mat(image.width(), image.height(), CvType.CV_8UC1)
        Core.inRange(image, _lowerBound, _upperBound, processedImage)
        //Imgproc.blur(processedImage, processedImage, Size(3.0, 3.0))
        return processedImage
    }
}
