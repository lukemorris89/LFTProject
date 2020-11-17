/*
* Copyright 2020 Google LLC. All rights reserved.
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

package com.example.androidcamerard.utils

import android.graphics.*
import android.media.Image
import android.view.View
import com.example.androidcamerard.camera.GraphicOverlay
import java.io.ByteArrayOutputStream


/** Utils functions for bitmap conversions.  */
object BitmapUtils {
    fun cropImage(bitmap: Bitmap, frame: View, graphicOverlay: GraphicOverlay): ByteArray? {
        val matrix = Matrix()
        matrix.postRotate(90f)

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        val reference = graphicOverlay.scanRect
        val heightOriginal: Int = frame.height
        val widthOriginal: Int = frame.width

        val heightFrame: Int = reference.height().toInt()
        val widthFrame: Int = reference.width().toInt()

        val leftFrame: Int = reference.left.toInt()
        val topFrame: Int = reference.top.toInt()

        val heightReal = bitmap.height
        val widthReal = bitmap.width

        val widthFinal = (widthFrame * widthReal / heightOriginal) - 10
        val heightFinal = (heightFrame * heightReal / widthOriginal) - 1000
        val leftFinal = (leftFrame * widthReal / widthOriginal) - 100
        val topFinal = (topFrame * heightReal / heightOriginal) +  100

        val bitmapFinal = Bitmap.createBitmap(
            rotatedBitmap,
            leftFinal, topFinal, widthFinal, heightFinal
        )
        val stream = ByteArrayOutputStream()
        bitmapFinal.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            stream
        )
        return stream.toByteArray()
    }

}