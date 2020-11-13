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

package com.example.androidcamerard.processor

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.os.Build.VERSION_CODES
import android.os.SystemClock
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.example.androidcamerard.ScopedExecutor
import com.example.androidcamerard.camera.FrameMetadata
import com.example.androidcamerard.camera.GraphicOverlay
import com.example.androidcamerard.utils.BitmapUtils
import com.example.androidcamerard.utils.YuvNV21Util
import com.example.androidcamerard.views.ImageLabellingLiveFragment
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer
import java.util.Timer
import java.util.TimerTask
import kotlin.math.max
import kotlin.math.min

/**
 * Abstract base class for ML Kit frame processors. Subclasses need to implement {@link
 * #onSuccess(T, FrameMetadata, GraphicOverlay)} to define what they want to with the detection
 * results and {@link #detectInImage(VisionImage)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
abstract class VisionProcessorBase<T>(context: Context, private val graphicOverlay: GraphicOverlay) : VisionImageProcessor {

    companion object {
        const val MANUAL_TESTING_LOG = "LogTagForTest"
        private const val TAG = "VisionProcessorBase"
    }

    private var activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val fpsTimer = Timer()
    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    // Whether this processor is already shut down
    private var isShutdown = false

    // Used to calculate latency, running in the same thread, no sync needed.
    private var numRuns = 0
    private var totalRunMs: Long = 0
    private var maxRunMs: Long = 0
    private var minRunMs = Long.MAX_VALUE

    // Frame count that have been processed so far in an one second interval to calculate FPS.
    private var frameProcessedInOneSecondInterval = 0
    private var framesPerSecond = 0

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private var latestImage: ByteBuffer? = null
    @GuardedBy("this")
    private var latestImageMetaData: FrameMetadata? = null
    // To keep the images and metadata in process.
    @GuardedBy("this")
    private var processingImage: ByteBuffer? = null
    @GuardedBy("this")
    private var processingMetaData: FrameMetadata? = null

    init {
        fpsTimer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    framesPerSecond = frameProcessedInOneSecondInterval
                    frameProcessedInOneSecondInterval = 0
                }
            },
            0,
            1000
        )
    }

    @RequiresApi(VERSION_CODES.KITKAT)
    @ExperimentalGetImage
    override fun processImageProxy(imageProxy: ImageProxy, graphicOverlay: GraphicOverlay?) {
        if (isShutdown) return

        val rotation = imageProxy.imageInfo.rotationDegrees
        val scannerRect = getScannerRectToPreviewViewRelation(Size(imageProxy.width, imageProxy.height), rotation)

        val image = imageProxy.image!!
        val cropRect = image.getCropRectAccordingToRotation(scannerRect, rotation)
        image.cropRect = cropRect

        val byteArray = YuvNV21Util.yuv420toNV21(image)
        val bitmap = BitmapUtils.getBitmap(byteArray, FrameMetadata(cropRect.width(), cropRect.height(), rotation))

        if (graphicOverlay != null) requestDetectInImage(
            InputImage.fromMediaImage(image, rotation),
            graphicOverlay, /* originalCameraImage= */
            bitmap
        )
            // When the image is from CameraX analysis use case, must call image.close()
            // on received images when finished using them. Otherwise, new images may not be
            // received or the camera may stall.
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun requestDetectInImage(
        image: InputImage,
        graphicOverlay: GraphicOverlay,
        originalCameraImage: Bitmap?,
    ): Task<T> {
        val startMs = SystemClock.elapsedRealtime()
        val inputImage: InputImage = InputImage.fromBitmap(originalCameraImage!!, 0)
        return detectInImage(inputImage).addOnSuccessListener(executor) { results: T ->
            val currentLatencyMs = SystemClock.elapsedRealtime() - startMs
            numRuns++
            frameProcessedInOneSecondInterval++
            totalRunMs += currentLatencyMs
            maxRunMs = max(currentLatencyMs, maxRunMs)
            minRunMs = min(currentLatencyMs, minRunMs)
            // Only log inference info once per second. When frameProcessedInOneSecondInterval is
            // equal to 1, it means this is the first frame processed during the current second.
            if (frameProcessedInOneSecondInterval == 1) {
                Log.d(TAG, "Max latency is: $maxRunMs")
                Log.d(TAG, "Min latency is: $minRunMs")
                Log.d(
                    TAG,
                    "Num of Runs: " + numRuns + ", Avg latency is: " + totalRunMs / numRuns
                )
                val mi = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(mi)
                val availableMegs = mi.availMem / 0x100000L
                Log.d(
                    TAG,
                    "Memory available in system: $availableMegs MB"
                )
            }

            this@VisionProcessorBase.onSuccess(results, graphicOverlay)
            graphicOverlay.postInvalidate()
        }
            .addOnFailureListener(executor) { e: Exception ->
                e.printStackTrace()
                this@VisionProcessorBase.onFailure(e)
            }
    }

    private fun getScannerRectToPreviewViewRelation(proxySize : Size, rotation : Int): ImageLabellingLiveFragment.ScannerRectToPreviewViewRelation {
        return when(rotation) {
            0, 180 -> {
                val size = graphicOverlay.size
                val width = size.width
                val height = size.height
                val previewHeight = width / (proxySize.width.toFloat() / proxySize.height)
                val heightDeltaTop = (previewHeight - height) / 2

                val scannerRect = graphicOverlay.scanRect
                val rectStartX = scannerRect.left
                val rectStartY = heightDeltaTop + scannerRect.top

                ImageLabellingLiveFragment.ScannerRectToPreviewViewRelation(
                    rectStartX / width,
                    rectStartY / previewHeight,
                    scannerRect.width() / width,
                    scannerRect.height() / previewHeight
                )
            }
            90, 270 -> {
                val size = graphicOverlay.size
                val width = size.width
                val height = size.height
                val previewWidth = height / (proxySize.width.toFloat() / proxySize.height)
                val widthDeltaLeft = (previewWidth - width) / 2

                val scannerRect = graphicOverlay.scanRect
                val rectStartX = widthDeltaLeft + scannerRect.left
                val rectStartY = scannerRect.top

                ImageLabellingLiveFragment.ScannerRectToPreviewViewRelation(
                    rectStartX / previewWidth,
                    rectStartY / height,
                    scannerRect.width() / previewWidth,
                    scannerRect.height() / height
                )
            }
            else -> throw IllegalArgumentException("Rotation degree ($rotation) not supported!")
        }
    }

    private fun Image.getCropRectAccordingToRotation(scannerRect: ImageLabellingLiveFragment.ScannerRectToPreviewViewRelation, rotation: Int) : Rect {
        return when(rotation) {
            0 -> {
                val startX = (scannerRect.relativePosX * this.width).toInt()
                val numberPixelW = (scannerRect.relativeWidth * this.width).toInt()
                val startY = (scannerRect.relativePosY * this.height).toInt()
                val numberPixelH = (scannerRect.relativeHeight * this.height).toInt()
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            90 -> {
                val startX = (scannerRect.relativePosY * this.width).toInt()
                val numberPixelW = (scannerRect.relativeHeight * this.width).toInt()
                val numberPixelH = (scannerRect.relativeWidth * this.height).toInt()
                val startY = height - (scannerRect.relativePosX * this.height).toInt() - numberPixelH
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            180 -> {
                val numberPixelW = (scannerRect.relativeWidth * this.width).toInt()
                val startX = (this.width - scannerRect.relativePosX * this.width - numberPixelW).toInt()
                val numberPixelH = (scannerRect.relativeHeight * this.height).toInt()
                val startY = (height - scannerRect.relativePosY * this.height - numberPixelH).toInt()
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            270 -> {
                val numberPixelW = (scannerRect.relativeHeight * this.width).toInt()
                val numberPixelH = (scannerRect.relativeWidth * this.height).toInt()
                val startX = (this.width - scannerRect.relativePosY * this.width - numberPixelW).toInt()
                val startY = (scannerRect.relativePosX * this.height).toInt()
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            else -> throw IllegalArgumentException("Rotation degree ($rotation) not supported!")
        }
    }

    override fun stop() {
        executor.shutdown()
        isShutdown = true
        numRuns = 0
        totalRunMs = 0
        fpsTimer.cancel()
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    protected abstract fun onSuccess(results: T, graphicOverlay: GraphicOverlay)

    protected abstract fun onFailure(e: Exception)
}
