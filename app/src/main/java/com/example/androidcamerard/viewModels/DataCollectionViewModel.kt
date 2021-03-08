package com.example.androidcamerard.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class DataCollectionViewModel : ViewModel() {

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

    init {
        _torchOn.value = false
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