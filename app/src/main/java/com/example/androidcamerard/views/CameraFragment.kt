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
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.androidcamerard.GraphicOverlay
import com.example.androidcamerard.R
import com.example.androidcamerard.VisionImageProcessor
import com.example.androidcamerard.labeldetector.LabelDetectorProcessor
import com.example.androidcamerard.viewmodel.PhotoViewModel
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerLocalModel
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerOptions
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class CameraFragment : Fragment() {

    private var previewView: PreviewView? = null
    private lateinit var graphicOverlay: GraphicOverlay
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false

    private var camera: Camera? = null

    //Use cases
    private var previewUseCase: Preview? = null
    private var imageCaptureUseCase: ImageCapture? = null
    private var analysisUseCase: ImageAnalysis? = null

    private var outputDirectory: File? = null
    private lateinit var cameraExecutor: ExecutorService

    private val viewModel: PhotoViewModel by activityViewModels()

    private var luminosity: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listener for take photo button
//        view.camera_capture_button.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        previewView = view.findViewById(R.id.cameraPreview)
        graphicOverlay = view.findViewById(R.id.graphic_overlay)

        return view
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            var cameraProvider: ProcessCameraProvider? = cameraProviderFuture.get()

            // Preview
            previewUseCase = Preview.Builder()
                .build()

//            imageCaptureUseCase = ImageCapture.Builder()
//                .build()

            imageProcessor = LabelDetectorProcessor(requireContext(), ImageLabelerOptions.DEFAULT_OPTIONS)

//            Replace LabelDetectorProcessor (above) with below when using custom models
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
//            LabelDetectorProcessor(
//                this, customImageLabelerOptions
//            )
//        }
//            AUTOML_LABELING -> {
//            Log.i(
//                TAG,
//                "Using AutoML Image Label Detector Processor"
//            )
//            val autoMLLocalModel = AutoMLImageLabelerLocalModel.Builder()
//                .setAssetFilePath("automl/manifest.json")
//                .build()
//            val autoMLOptions = AutoMLImageLabelerOptions
//                .Builder(autoMLLocalModel)
//                .setConfidenceThreshold(0f)
//                .build()
//            LabelDetectorProcessor(
//                this, autoMLOptions
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
//                            lensFacing == CameraSelector.LENS_FACING_FRONT
                        val rotationDegrees =
                            imageProxy.imageInfo.rotationDegrees
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            graphicOverlay.setImageSourceInfo(
                                imageProxy.width, imageProxy.height, isImageFlipped
                            )
                        } else {
                            graphicOverlay.setImageSourceInfo(
                                imageProxy.height, imageProxy.width, isImageFlipped
                            )
                        }
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
            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                previewUseCase?.setSurfaceProvider(cameraPreview.surfaceProvider)
                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, previewUseCase, analysisUseCase)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupAutoFocus() {
        cameraPreview.afterMeasured {
            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                cameraPreview.width.toFloat(), cameraPreview.height.toFloat()
            )
            val centerWidth = cameraPreview.width.toFloat() / 2
            val centerHeight = cameraPreview.height.toFloat() / 2
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
        })
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
                    viewModel.photoLuminosity.value = luminosity

                    findNavController().navigate(R.id.action_cameraFragment_to_cameraOutputFragment)
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
            if (allPermissionsGranted()) {
                startCamera()
            } else {
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

    private class ImageAnalyzer : ImageAnalysis.Analyzer {

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Pass image to an ML Kit Vision API

                // To use default options:
                val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

                // Or, to set the minimum confidence required:
                // val options = ImageLabelerOptions.Builder()
                //     .setConfidenceThreshold(0.7f)
                //     .build()
                // val labeler = ImageLabeling.getClient(options)

                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        // Task completed successfully
                        for (label in labels) {
                            val text = label.text
                            val confidence = label.confidence
                            val index = label.index
                            Log.i(TAG, "Text: $text, Confidence: $confidence")
                        }
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        Log.e(TAG, "Failed to process image. Error: ${e.localizedMessage}")
                    }
            }
        }
    }
}