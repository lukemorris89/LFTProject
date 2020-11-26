package com.example.androidcamerard.camera

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import java.io.File

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    // For storing captured raw imageproxy and cropped bitmap
    val capturedImageProxy = MutableLiveData<ImageProxy>()
    val capturedImageBitmap = MutableLiveData<Bitmap>()

    // For storing Uri of photo form gallery
    val photoFilename = MutableLiveData<Uri>()

    //For storing photo capture as file
    val outputDirectory = MutableLiveData<File>()

    // For logging photos collected so far in util class
    var numPhotosCollected = MutableLiveData<Int>()
}