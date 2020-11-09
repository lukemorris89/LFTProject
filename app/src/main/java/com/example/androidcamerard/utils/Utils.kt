package com.example.androidcamerard.utils

import android.app.Activity
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult

object Utils {

    internal const val REQUEST_CODE_PHOTO_LIBRARY = 1

    private const val TAG = "Utils"

    internal fun openImagePicker(activity: Activity) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(activity, intent, REQUEST_CODE_PHOTO_LIBRARY, null)
    }
}