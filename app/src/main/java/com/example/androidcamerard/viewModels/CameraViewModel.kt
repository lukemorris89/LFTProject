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

    // For storing Uri of photo from gallery
    val photoFilename = MutableLiveData<Uri>()

    //For storing photo capture as file
    var outputDirectory: File? = null

    // For logging photos collected so far in util class
    private val _numPhotosCollectedLD = MutableLiveData<Int>()
    val numPhotosCollectedLD: LiveData<Int>
        get() = _numPhotosCollectedLD

    // For enabling device torch
    private val _torchOn = MutableLiveData<Boolean>()
    val torchOn: LiveData<Boolean>
        get() = _torchOn

    // This is a LiveData field. Choosing this structure because the whole list tends to be updated
    // at once in ML and not individual elements. Updating this once for the entire list makes
    // sense.
    private val _recognitionList = MutableLiveData<List<Recognition>>()
    val recognitionList: LiveData<List<Recognition>>
        get() = _recognitionList

    init {
        _torchOn.value = false
    }

    fun updateData(recognitions: List<Recognition>) {
        _recognitionList.postValue(recognitions)
    }

    fun getNumPhotos() {
        var numPhotos = 0
        if (_numPhotosCollectedLD.value == null) {
            _numPhotosCollectedLD.value = 0
        }

        if (outputDirectory?.listFiles()!!.isNotEmpty()) {
            for (photo in outputDirectory!!.listFiles()!!) {
                if (photo.name.startsWith("DataCollect")) {
                    numPhotos += 1
                }
            }
        }
        _numPhotosCollectedLD.value = numPhotos
    }

    fun incrementNumPhotos() {
        _numPhotosCollectedLD.value = _numPhotosCollectedLD.value?.plus(1)
    }

    fun toggleTorch() {
        _torchOn.value = !torchOn.value!!
    }
}