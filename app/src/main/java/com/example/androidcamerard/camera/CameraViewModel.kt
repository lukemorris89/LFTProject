package com.example.androidcamerard.camera

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import java.io.File

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val placeholderImageUri: Uri = Uri.parse(
        "android.resource://" + application.packageName + "/drawable/media"
    )

    val capturedImageProxy = MutableLiveData<ImageProxy>()
    val capturedImageBitmap = MutableLiveData<Bitmap>()

    val photoFilename = MutableLiveData(placeholderImageUri)
    val graphicOverlay = MutableLiveData<GraphicOverlay>()
    var numPhotosCollected = MutableLiveData<Int>()
    val outputDirectory = MutableLiveData<File>()

    init {
        Log.i(TAG, "Photo ViewModel created.")
    }

    companion object {
        private val TAG = CameraViewModel::class.simpleName
    }
}