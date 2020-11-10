package com.example.androidcamerard.views

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.androidcamerard.R
import com.example.androidcamerard.camera.GraphicOverlay
import com.example.androidcamerard.imagelabelling.ImageLabellingProcessor
import com.example.androidcamerard.processor.VisionImageProcessor
import com.example.androidcamerard.viewmodel.CameraViewModel
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ImageLabellingLiveFragment : Fragment(), View.OnClickListener {

    private lateinit var previewView: PreviewView
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var flashButton: View
    private lateinit var closeButton: View
    private lateinit var photoCaptureButton: View
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false

    private var camera: Camera? = null

    //Use cases
    private var previewUseCase: Preview? = null
    private var imageCaptureUseCase: ImageCapture? = null
    private var analysisUseCase: ImageAnalysis? = null

    private var outputDirectory: File? = null
    private lateinit var cameraExecutor: ExecutorService

    private val viewModel: CameraViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image_labelling_live, container, false)

        // Request camera permissions
        if (allPermissionsGranted()) startCamera()
        else requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        previewView = view.findViewById(R.id.preview_view)
        graphicOverlay = view.findViewById(R.id.graphic_overlay)
        flashButton = view.findViewById<View>(R.id.flash_button).apply {
            setOnClickListener(this@ImageLabellingLiveFragment)
        }
        photoCaptureButton = view.findViewById<View>(R.id.photo_capture_button).apply {
            setOnClickListener(this@ImageLabellingLiveFragment)
            isEnabled = false
        }
        closeButton = view.findViewById<View>(R.id.close_button).apply {
            setOnClickListener(this@ImageLabellingLiveFragment)
        }
        return view
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider? = cameraProviderFuture.get()

            // Preview
            previewUseCase = Preview.Builder().build()

            // Image Capture
            imageCaptureUseCase = ImageCapture.Builder().build()

            // Image Analysis
            val options = ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build()
            imageProcessor = ImageLabellingProcessor(requireContext(), options, requireView(), viewModel)

//            Replace ImageLabellingProcessor (above) with below when using custom models
//            IMAGE_LABELING_CUSTOM -> {
//            Log.i(
//                TAG,
//                "Using Custom Image Label (Bird) Detector Processor"
//            )
//            val localClassifier = LocalModel.Builder()
//                .setAssetFilePath("custom_models/bird_classifier.tflite")
//                .build()
//            val customImageLabelerOptions =
//                CustomImageLabelerOptions.Builder(localClassifier).build()
//            ImageLabellingProcessor(
//                this, customImageLabelerOptions
//            )
//        }

            analysisUseCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            needUpdateGraphicOverlayImageSourceInfo = true
            analysisUseCase!!.setAnalyzer(
                // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                // thus we can just runs the analyzer itself on main thread.
                ContextCompat.getMainExecutor(requireContext()), { imageProxy: ImageProxy ->
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        val isImageFlipped = false
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        if (rotationDegrees == 0 || rotationDegrees == 180)
                            graphicOverlay.setImageSourceInfo(
                            imageProxy.width, imageProxy.height, isImageFlipped)
                        else graphicOverlay.setImageSourceInfo(
                                imageProxy.height, imageProxy.width, isImageFlipped)
                        needUpdateGraphicOverlayImageSourceInfo = false
                    }
                    try {
                        imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
                    } catch (e: MlKitException) {
                        Log.e(
                            TAG,
                            "Failed to process image. Error: " + e.localizedMessage
                        )
                    }
                }
            )

            // Select back camera as a default
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

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
                    analysisUseCase)
            } catch(exc: Exception) {
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
        if (camera!!.cameraInfo.hasFlashUnit()) camera!!.cameraControl.enableTorch(!flashMode)
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCaptureUseCase ?: return

        // Create a time-stamped output file to hold the image
        val photoFile = File(outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.ENGLISH)
                .format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    viewModel.photoFilename.value = savedUri
                    findNavController()
                        .navigate(R.id.action_imageLabellingLiveFragment_to_cameraOutputFragment)
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

    private fun getOutputDirectory(): File? {
        val mediaDir = context?.externalMediaDirs?.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }

        val defaultFilesDir: File? = activity?.filesDir

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else defaultFilesDir
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) startCamera()
            else {
                Toast.makeText(activity,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}