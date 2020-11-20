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

package com.example.androidcamerard.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Size
import android.view.View
import com.example.androidcamerard.R.*
import com.example.androidcamerard.utils.getEnum
import com.example.androidcamerard.utils.px
import kotlin.math.min

/**
 * A view which renders a series of custom graphics to be overlayed on top of an associated preview
 * (i.e., the camera preview). The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.
 *
 *
 * Supports scaling and mirroring of the graphics relative the camera's preview properties. The
 * idea is that detection items are expressed in terms of an image size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.
 *
 *
 * Associated [Graphic] items should use the following methods to convert to view
 * coordinates for the graphics that are drawn:
 *
 *
 *  1. [Graphic.scale] adjusts the size of the supplied value from the image scale
 * to the view scale.
 *  1. [Graphic.translateX] and [Graphic.translateY] adjust the
 * coordinate from the image's coordinate system to the view coordinate system.
 *
 */
class GraphicOverlay@JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ScannerOverlay {

    private val transparentPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    private val strokePaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            strokeWidth = context.px(3f)
            style = Paint.Style.STROKE
        }
    }

    var drawBlueRect : Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var type: Type

    private val blueColor = resources.getColor(color.bond)

    init {
        setWillNotDraw(false)

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        val typedArray = context.obtainStyledAttributes(attrs, styleable.ScannerOverlayImpl, 0, 0)
        type = typedArray.getEnum(styleable.ScannerOverlayImpl_type, Type.LFT)
        typedArray.recycle()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#88000000"))

        val radius = context.px(16f)
        val rectF = scanRect
        canvas.drawRoundRect(rectF, radius, radius, transparentPaint)
        strokePaint.color = if(drawBlueRect) blueColor else Color.WHITE
        canvas.drawRoundRect(rectF, radius, radius, strokePaint)
    }

    override val size: Size
        get() = Size(width, height)

    // Position, shape and size of overlay window
    override val scanRect: RectF
        get() = when (type) {
            Type.LFT -> {
                    val size = min(width * 0.7f, MAX_WIDTH_PORTRAIT)
                    val l = (width - size) / 1.05f
                    val r = width - l
                    val t = height * 0.1f
                    val b = (t + size) * 1.6f
                    RectF(l, t, r, b)
            }
        }


    enum class Type {
        LFT
    }

    companion object {
        const val MAX_WIDTH_PORTRAIT = 1200f
    }
}