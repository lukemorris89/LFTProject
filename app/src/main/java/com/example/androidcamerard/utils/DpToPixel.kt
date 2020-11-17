package com.example.androidcamerard.utils

import android.content.Context
import android.util.DisplayMetrics

fun Context.px(densityPixel : Float) = calc(this, densityPixel)

private fun calc(context : Context, dp : Float) : Float{
    return dp * (context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
}