package com.example.androidcamerard.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult
import java.io.*

object Utils {

    internal const val REQUEST_CODE_PHOTO_LIBRARY = 1

    internal fun openImagePicker(activity: Activity) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(activity, intent, REQUEST_CODE_PHOTO_LIBRARY, null)
    }

    internal fun getOutputDirectory(context: Context): File? {
        val mediaDir = context.externalMediaDirs?.firstOrNull()?.let {
            File(it, "Android Camera R&D").apply { mkdirs() } }

        val defaultFilesDir: File? = context.filesDir

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else defaultFilesDir
    }
}