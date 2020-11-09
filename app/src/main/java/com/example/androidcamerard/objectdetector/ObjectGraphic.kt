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

package com.example.androidcamerard.objectdetector

import android.graphics.*
import androidx.core.content.ContextCompat
import com.example.androidcamerard.R
import com.example.androidcamerard.camera.GraphicOverlay
import kotlin.math.max
import kotlin.math.min

import com.google.mlkit.vision.objects.DetectedObject

/** Draw the detected object info in preview.  */
class ObjectGraphic constructor(
    overlay: GraphicOverlay,
    private val detectedObject: DetectedObject
) : GraphicOverlay.Graphic(overlay) {

    private val boxColor: Int  = context.resources.getColor(R.color.bounding_box_solid_color)
    private val scrimPaint: Paint = Paint().apply {
        color = context.resources.getColor(R.color.object_detection_scrim_color)
    }

    private val boxPaint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = context
            .resources
            .getDimensionPixelOffset(
                R.dimen.bounding_box_stroke_width
            ).toFloat()
        color = boxColor
    }

    private val boxCornerRadius: Int = context.resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius)

    override fun draw(canvas: Canvas?) {
        // Draws the bounding box.
        val rect = overlay.translateRect(detectedObject.boundingBox)
        scrimPaint.shader = LinearGradient(
            0f,
            0f,
            overlay.width.toFloat(),
            overlay.height.toFloat(),
            ContextCompat.getColor(context, R.color.object_detected_bg_gradient_start),
            ContextCompat.getColor(context, R.color.object_detected_bg_gradient_end),
            Shader.TileMode.CLAMP)

        val x0 = translateX(rect.left)
        val x1 = translateX(rect.right)
        rect.left = min(x0, x1)
        rect.right = max(x0, x1)
        rect.top = translateY(rect.top)
        rect.bottom = translateY(rect.bottom)
        canvas?.drawRect(
            0f,
            0f,
            canvas.width.toFloat(),
            canvas.height.toFloat(),
            scrimPaint)

        boxPaint.shader = LinearGradient(
            rect.left,
            rect.top,
            rect.left,
            rect.bottom,
            boxColor,
            boxColor,
            Shader.TileMode.CLAMP)
        canvas?.drawRoundRect(rect, boxCornerRadius.toFloat(), boxCornerRadius.toFloat(), boxPaint)
    }
}
