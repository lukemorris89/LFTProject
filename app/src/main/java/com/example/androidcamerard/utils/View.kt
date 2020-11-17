package com.example.androidcamerard.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.TypedArray

inline fun <reified T : Enum<T>> TypedArray.getEnum(index: Int, default: T) =
        getInt(index, -1).let { if (it >= 0) enumValues<T>()[it] else default }


fun Context.isPortrait() : Boolean {
   val orientation = this.resources.configuration.orientation
   return orientation == Configuration.ORIENTATION_PORTRAIT
}
