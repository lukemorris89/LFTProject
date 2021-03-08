package com.example.androidcamerard.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.androidcamerard.R
import com.example.androidcamerard.databinding.FragmentImageLabellingLiveBinding
import com.example.androidcamerard.ml.Model
import com.example.androidcamerard.utils.BitmapUtils.liveImageProxyToBitmap
import com.example.androidcamerard.viewModels.ImageLabellingAnalysisViewModel
import com.example.androidcamerard.recognition.Recognition
import com.example.androidcamerard.utils.BitmapUtils.cropBitmapToTest
import com.example.androidcamerard.utils.BitmapUtils.capturedImageProxyToBitmap
import com.example.androidcamerard.utils.SOURCE_IMAGE_CAPTURE
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.TensorImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.text.*

// Listener for the result of the ImageAnalyzer
typealias RecognitionListener = (recognition: List<Recognition>) -> Unit

/**
 * Main source of live image analysis within the app
 */
class ImageLabellingLiveFragment : Fragment(), View.OnClickListener {

    // Data binding
    private lateinit var binding: FragmentImageLabellingLiveBinding

    // CameraX variables
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalyzer: ImageAnalysis
    private lateinit var camera: Camera
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // ViewModel variables
    private val viewModel: ImageLabellingAnalysisViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_image_labelling_live,
                container,
                false
            )

        setUpUI()

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun setUpUI() {
        binding.overlayText = getString(R.string.align_the_test_device_inside_the_box)
        viewModel.recognitionList.observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                if (it[0].label == "lateral_flow_test" && it[0].confidence >= 0.8f) {
                    binding.graphicOverlay.drawBlueRect = true
                    binding.photoCaptureButton.apply {
                        isEnabled = true
                        setImageResource(R.drawable.ic_photo_camera_24)
                    }
                    binding.overlayText =
                        String.format("Lateral Flow Test: %.1f", it[0].confidence * 100.0f)

                } else {
                    binding.graphicOverlay.drawBlueRect = false
                    binding.photoCaptureButton.apply {
                        isEnabled = false
                        setImageResource(R.drawable.ic_photo_camera_disabled_v24)
                    }
                    binding.overlayText = getString(R.string.align_the_test_device_inside_the_box)
                }
            }
        })
        binding.closeButton.setOnClickListener(this@ImageLabellingLiveFragment)
        binding.photoCaptureButton.apply {
            setOnClickListener(this@ImageLabellingLiveFragment)
            // Begin session with capture button disabled - should only be enabled when valid object detected
            isEnabled = false
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            // Image Capture
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            // Image Analysis
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                // How the Image Analyser should pipe in input, 1. every frame but drop no frame, or
                // 2. go to the latest frame and may drop some frame. The default is 2.
                // STRATEGY_KEEP_ONLY_LATEST. The following line is optional, kept here for clarity
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysisUseCase: ImageAnalysis ->
                    analysisUseCase.setAnalyzer(
                        cameraExecutor,
                        ImageAnalyzer(requireContext()) { items ->
                            // updating the list of recognised objects
                            viewModel.updateData(items)
                        })
                }

            // Select camera, back is the default. If it is not available, choose front camera
            val cameraSelector =
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
                    CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            // Set viewport as equal to preview view size to allow for WYSIWYG-style analysis
            // (prevents imageProxy being cropped to 720 x 720)
            val viewport = binding.previewView.viewPort

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera - try to bind everything at once and CameraX will find
                // the best combination. Use case group used as this allows viewport to be set
                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageCapture)
                    .addUseCase(imageAnalyzer)
                    .setViewPort(viewport!!)
                    .build()

                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    useCaseGroup
                )

                // Attach the preview to preview view, aka View Finder
                preview.setSurfaceProvider(binding.previewView.surfaceProvider)

                viewModel.torchOn.observe(viewLifecycleOwner, {
                    updateTorchMode(it)
                })
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
            setupAutoFocus()
        }, ContextCompat.getMainExecutor(context))
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.photo_capture_button -> takePhoto()
            R.id.close_button -> findNavController().popBackStack()
        }
    }

    private fun updateTorchMode(torchOn: Boolean) {
        binding.torchButton.isSelected = torchOn
        if (camera.cameraInfo.hasFlashUnit()) {
            camera.cameraControl.enableTorch(torchOn)
        }
    }

    private fun takePhoto() {
        // Disable the photo capture button to prevent errors when the camera closes
        binding.photoCaptureButton.isClickable = false
        // Get a stable reference of the modifiable image capture use case
        // Create a time-stamped output file to hold the image

        imageCapture.takePicture(cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }

                @SuppressLint("UnsafeExperimentalUsageError")
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val imageBitmap = capturedImageProxyToBitmap(imageProxy)
                    val croppedBitmap = cropBitmapToTest(imageBitmap)

                    viewModel.capturedImageProxy.postValue(imageProxy)
                    viewModel.capturedImageBitmap.postValue(croppedBitmap)

                    // Inform analysis fragment of source to determine which UI to show
                    val source = SOURCE_IMAGE_CAPTURE
                    val action =
                        ImageLabellingLiveFragmentDirections.actionImageLabellingLiveFragmentToImageAnalysisFragment(
                            source
                        )
                    findNavController().navigate(action)

                    imageProxy.close()
                }
            }
        )
    }

    private fun setupAutoFocus() {
        binding.previewView.afterMeasured {
            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                binding.previewView.width.toFloat(), binding.previewView.height.toFloat()
            )
            val centerWidth = binding.previewView.width.toFloat() / 2
            val centerHeight = binding.previewView.height.toFloat() / 2
            //create a point on the center of the view
            val autoFocusPoint = factory.createPoint(centerWidth, centerHeight)
            try {
                camera.cameraControl.startFocusAndMetering(
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

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        activity?.baseContext?.let { it1 ->
            ContextCompat.checkSelfPermission(
                it1, it
            )
        } == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
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

    class ImageAnalyzer(private val context: Context, private val listener: RecognitionListener) :
        ImageAnalysis.Analyzer {

        // Add class variable TensorFlow Lite Model
        // Initializing the LFT Model by lazy so that it runs in the same thread when the process
        // method is called.
        private val model: Model by lazy {

            // Optional GPU acceleration
            val compatList = CompatibilityList()

            val options = if (compatList.isDelegateSupportedOnThisDevice) {
                Log.d(TAG, "This device is GPU Compatible ")
                org.tensorflow.lite.support.model.Model.Options.Builder()
                    .setDevice(org.tensorflow.lite.support.model.Model.Device.GPU).build()
            } else {
                Log.d(TAG, "This device is GPU Incompatible ")
                org.tensorflow.lite.support.model.Model.Options.Builder().setNumThreads(4).build()
            }

            // Initialize the LFT Model
            Model.newInstance(context, options)
        }

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val analysisTimeStart = System.currentTimeMillis()
            val items = mutableListOf<Recognition>()

            // Crop imageProxy to shape of test
            val bitmapImage = liveImageProxyToBitmap(context, imageProxy)
            val croppedBitmap = cropBitmapToTest(bitmapImage!!)

            // Convert Image to Bitmap then to TensorImage
            val tfImage = TensorImage.fromBitmap(croppedBitmap)

            // Process the image using the trained model, sort and pick out the top results
            val outputs = model.process(tfImage)
                .probabilityAsCategoryList.apply {
                    sortByDescending { it.score } // Sort with highest confidence first
                }.take(MAX_RESULT_DISPLAY) // take the top results

            // Converting the top probability items into a list of recognitions
            for (output in outputs) {
                items.add(Recognition(output.label, output.score))
            }

            // Return the result
            listener(items.toList())

            //Log for testing speed of analysis
            val analysisTime = System.currentTimeMillis() - analysisTimeStart
            Log.d(TAG, "Total analysis time: $analysisTime ms")

            // Close the image,this tells CameraX to feed the next image to the analyzer
            imageProxy.close()
        }
    }


    companion object {
        // Constants
        private const val TAG = "ImageLabellingLive"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val MAX_RESULT_DISPLAY = 1 // Maximum number of results displayed
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)


    }
}