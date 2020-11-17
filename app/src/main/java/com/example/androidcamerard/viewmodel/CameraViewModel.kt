package com.example.androidcamerard.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.androidcamerard.camera.GraphicOverlay
import com.google.mlkit.vision.label.ImageLabel
import java.io.File

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val placeholderImageUri: Uri = Uri.parse(
        "android.resource://" + application.packageName + "/drawable/media"
    )

    val imageLabels = MutableLiveData<List<ImageLabel>>()
    val photoFilename = MutableLiveData(placeholderImageUri)
    val croppedBitmap = MutableLiveData<Bitmap>()
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