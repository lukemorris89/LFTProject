package com.example.androidcamerard.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class PhotoViewModel(
    application: Application
) : AndroidViewModel(application) {


    private val placeholderImageUri: Uri = Uri.parse(
        "android.resource://" + application.packageName + "/drawable/media"
    )

    private val placeholderPhotoLuminosity: Double = 0.0

    val photoFilename =
        MutableLiveData<Uri>(
            placeholderImageUri
        )

    init {
        Log.i(TAG,"Photo ViewModel created.")
    }

    companion object {
        private val TAG = PhotoViewModel::class.simpleName
    }
}