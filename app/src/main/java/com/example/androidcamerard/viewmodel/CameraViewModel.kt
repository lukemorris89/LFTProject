package com.example.androidcamerard.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.label.ImageLabel
import java.util.concurrent.ExecutionException

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val imageLabels = MutableLiveData<List<ImageLabel>>()

    private var cameraProviderLiveData: MutableLiveData<ProcessCameraProvider>? = null
    private val placeholderImageUri: Uri = Uri.parse(
        "android.resource://" + application.packageName + "/drawable/media"
    )
    val photoFilename = MutableLiveData<Uri>(placeholderImageUri)

    init {
        Log.i(TAG, "Photo ViewModel created.")
    }

    // Handle any errors (including cancellation) here.
    val processCameraProvider: LiveData<ProcessCameraProvider>
        get() {
            if (cameraProviderLiveData == null) {
                cameraProviderLiveData = MutableLiveData()
                val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
                cameraProviderFuture.addListener(
                    {
                        try {
                            cameraProviderLiveData!!.setValue(cameraProviderFuture.get())
                        } catch (e: ExecutionException) {
                            // Handle any errors (including cancellation) here.
                            Log.e(
                                TAG,
                                "Unhandled exception",
                                e
                            )
                        } catch (e: InterruptedException) {
                            Log.e(
                                TAG,
                                "Unhandled exception",
                                e
                            )
                        }
                    },
                    ContextCompat.getMainExecutor(getApplication())
                )
            }
            return cameraProviderLiveData as MutableLiveData<ProcessCameraProvider>
        }

    companion object {
        private val TAG = CameraViewModel::class.simpleName
    }
}