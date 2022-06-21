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

package com.valaphee.synergy.cv.extractor

import com.valaphee.foundry.math.Double2
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.imgproc.Imgproc

/**
 * @author Kevin Ludwig
 */
class FindContoursExtractor : Extractor() {
    override fun extract(image: Mat): List<Double2> {
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(image, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        return contours.map {
            var x = 0.0
            var y = 0.0
            val points = it.toArray()
            points.forEach {
                x += it.x
                y += it.y
            }
            Double2(x / points.size, y / points.size)
        }
    }
}
