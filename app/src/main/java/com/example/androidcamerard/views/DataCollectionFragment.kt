package com.example.androidcamerard.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.androidcamerard.R
import com.example.androidcamerard.utils.Utils
import com.example.androidcamerard.viewmodel.CameraViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class DataCollectionFragment : Fragment(), View.OnClickListener {

    private lateinit var previewView: PreviewView
    private lateinit var flashButton: View
    private lateinit var closeButton: View
    private lateinit var photoCaptureButton: View

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    //Use cases
    private var previewUseCase: Preview? = null
    private var imageCaptureUseCase: ImageCapture? = null

    private var outputDirectory: File? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var tvNumPhotosCollected: TextView
    private var numPhotosCollected: Int? = 0

    private val viewModel: CameraViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_collection, container, false)

        // Request camera permissions
        if (allPermissionsGranted()) startCamera()
        else requestPermissions(
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )

        numPhotosCollected = getNumPhotosCollected()
        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = viewModel.outputDirectory.value

        tvNumPhotosCollected = view.findViewById<TextView>(R.id.num_photos_collected).apply {
            text = getNumPhotosCollected().toString()
        }

        previewView = view.findViewById(R.id.preview_view)
        flashButton = view.findViewById<View>(R.id.flash_button).apply {
            setOnClickListener(this@DataCollectionFragment)
        }
        photoCaptureButton = view.findViewById<View>(R.id.photo_capture_button).apply {
            setOnClickListener(this@DataCollectionFragment)
        }
        closeButton = view.findViewById<View>(R.id.close_button).apply {
            setOnClickListener(this@DataCollectionFragment)
        }

        return view
    }

    private fun getNumPhotosCollected(): Int {
        if (viewModel.numPhotosCollected.value == null) {
            val outputDirectory = Utils.getOutputDirectory(requireContext())
            viewModel.outputDirectory.value = outputDirectory
            var numPhotos = 0
            if (outputDirectory!!.listFiles()!!.isNotEmpty()) {
                for (photo in outputDirectory.listFiles()!!) {
                    if (photo.name.startsWith("DataCollect")) {
                        numPhotos += 1
                    }
                }
            }
            return numPhotos
        }
        return 0
    }

    override fun onResume() {
        super.onResume()
        tvNumPhotosCollected.text = getNumPhotosCollected().toString()
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            previewUseCase = Preview.Builder().build()

            // Image Capture
            imageCaptureUseCase = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            setupAutoFocus()

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                previewUseCase?.setSurfaceProvider(previewView.surfaceProvider)
                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    previewUseCase,
                    imageCaptureUseCase,
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.photo_capture_button -> takePhoto()
            R.id.close_button -> findNavController().popBackStack()
            R.id.flash_button -> updateFlashMode(flashButton.isSelected)
        }
    }

    private fun updateFlashMode(flashMode: Boolean) {
        flashButton.isSelected = !flashMode
        if (camera!!.cameraInfo.hasFlashUnit()) {
            camera!!.cameraControl.enableTorch(!flashMode)
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCaptureUseCase ?: return
        // Create a time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            "DataCollect_" + SimpleDateFormat(FILENAME_FORMAT, Locale.ENGLISH)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    numPhotosCollected = numPhotosCollected?.plus(1)
                    viewModel.numPhotosCollected.value = numPhotosCollected
                    tvNumPhotosCollected.text = numPhotosCollected.toString()
                }
            }
        )
    }

    private fun setupAutoFocus() {
        previewView.afterMeasured {
            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                previewView.width.toFloat(), previewView.height.toFloat()
            )
            val centerWidth = previewView.width.toFloat() / 2
            val centerHeight = previewView.height.toFloat() / 2
            //create a point on the center of the view
            val autoFocusPoint = factory.createPoint(centerWidth, centerHeight)
            try {
                camera?.cameraControl?.startFocusAndMetering(
                    FocusMeteringAction.Builder(
                        autoFocusPoint,
                        FocusMeteringAction.FLAG_AF
                    ).apply {
                        //auto-focus every 1 seconds
                        setAutoCancelDuration(1, TimeUnit.SECONDS)
                    }.build()
                )
            } catch (e: CameraInfoUnavailableException) {
            }
        }
    }

    private inline fun View.afterMeasured(crossinline block: () -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    block()
                }
            }
        }
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        activity?.baseContext?.let { it1 ->
            ContextCompat.checkSelfPermission(
                it1, it
            )
        } == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) startCamera()
            else {
                Toast.makeText(
                    activity,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "DataCollectionFrag"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    }
}