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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer


/** Utils functions for bitmap conversions.  */
object BitmapUtils {

    /**
     * Convert Image Proxy to Bitmap
     */
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var rotationMatrix: Matrix

    @SuppressLint("UnsafeExperimentalUsageError")
    fun liveImageProxyToBitmap(context: Context, imageProxy: ImageProxy): Bitmap? {
        val yuvToRgbConverter = YuvToRgbConverter(context)
        val image = imageProxy.image ?: return null


        // Initialise Buffer
        if (!::bitmapBuffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            rotationMatrix = Matrix()
            rotationMatrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
            )
        }

        // Pass image to an image analyser
        yuvToRgbConverter.yuvToRgb(image, bitmapBuffer)

        // Create the Bitmap in the correct orientation
        return Bitmap.createBitmap(
            bitmapBuffer,
            0,
            0,
            bitmapBuffer.width,
            bitmapBuffer.height,
            rotationMatrix,
            false
        )
    }

    fun capturedImageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val matrix = Matrix()

        matrix.postRotate(90f)


        return Bitmap.createBitmap(
            rawBitmap,
            0,
            0,
            rawBitmap.width,
            rawBitmap.height,
            matrix,
            true
        )
    }

    fun cropBitmapToTest(bitmap: Bitmap): Bitmap {

        val width = bitmap.width
        val height = bitmap.height
        val size = width * 0.7f
        val l = (width - size) / 1.05f
        val r = width - l
        val t = height * 0.1f
        val b = (t + size) * 1.6f
        val cropWidth = r - l
        val cropHeight = b - t

        return Bitmap.createBitmap(
            bitmap,
            l.toInt(),
            t.toInt(),
            cropWidth.toInt(),
            cropHeight.toInt()
        )
    }
}