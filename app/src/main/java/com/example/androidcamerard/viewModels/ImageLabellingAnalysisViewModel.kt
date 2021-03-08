package com.example.androidcamerard.viewModels

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.androidcamerard.recognition.Recognition

class ImageLabellingAnalysisViewModel : ViewModel() {

    // For storing captured raw imageproxy and cropped bitmap
    val capturedImageProxy = MutableLiveData<ImageProxy>()
    val capturedImageBitmap = MutableLiveData<Bitmap>()

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

    fun toggleTorch() {
        _torchOn.value = !torchOn.value!!
    }
}