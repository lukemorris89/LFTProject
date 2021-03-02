package com.example.androidcamerard.viewModels

import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.androidcamerard.recognition.Recognition
import java.io.File

class CameraViewModel : ViewModel() {

    // For storing captured raw imageproxy and cropped bitmap
    val capturedImageProxy = MutableLiveData<ImageProxy>()
    val capturedImageBitmap = MutableLiveData<Bitmap>()

    // For storing Uri of photo form gallery
    val photoFilename = MutableLiveData<Uri>()

    //For storing photo capture as file
    var outputDirectory: File? = null

    // For logging photos collected so far in util class
    var numPhotosCollected = MutableLiveData<Int>()

    // This is a LiveData field. Choosing this structure because the whole list tends to be updated
    // at once in ML and not individual elements. Updating this once for the entire list makes
    // sense.
    private val _recognitionList = MutableLiveData<List<Recognition>>()
    val recognitionList: LiveData<List<Recognition>> = _recognitionList

    fun updateData(recognitions: List<Recognition>) {
        _recognitionList.postValue(recognitions)
    }
}