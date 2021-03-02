package com.example.androidcamerard.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.androidcamerard.R
import com.example.androidcamerard.databinding.FragmentDataCollectionBinding
import com.example.androidcamerard.utils.Utils
import com.example.androidcamerard.viewModels.CameraViewModel
import kotlinx.android.synthetic.main.fragment_data_collection.*
import kotlinx.android.synthetic.main.top_action_bar_live_camera.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Fragment designed for developer only - to collect large amounts of data in a short period of time.
 * Taking a photo in this fragment does not trigger any navigation component - user stays on same
 * fragment and is allowed to continue taking more photos.
 * Photos are named "Data Collect_" plus date to distinguish these from photos taken in main app.
 */

class DataCollectionFragment : Fragment(), View.OnClickListener {

    // Data binding
    private lateinit var binding: FragmentDataCollectionBinding

    // CameraX
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService

    // Use Cases
    private var previewUseCase: Preview? = null
    private var imageCaptureUseCase: ImageCapture? = null

    private val viewModel: CameraViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_data_collection,
                container,
                false
            )

        // Request camera permissions
        if (allPermissionsGranted()) startCamera()
        else requestPermissions(
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialise view with number of photos taken so far
        viewModel.outputDirectory = Utils.getOutputDirectory(requireContext())

        getNumPhotosCollected()
        viewModel.numPhotosCollected.observe(viewLifecycleOwner, {
            binding.numPhotosCollected = it.toString()
        })

        photo_capture_button.setOnClickListener(this@DataCollectionFragment)
        flash_button.setOnClickListener(this@DataCollectionFragment)
        close_button.setOnClickListener(this@DataCollectionFragment)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.cameraViewModel = viewModel

        return binding.root
    }

    private fun getNumPhotosCollected() {
        var numPhotos = 0
        if (viewModel.numPhotosCollected.value == null) {
            viewModel.numPhotosCollected.value = 0
        }
        val outputDirectory = viewModel.outputDirectory

        if (outputDirectory?.listFiles()!!.isNotEmpty()) {
            for (photo in outputDirectory.listFiles()!!) {
                if (photo.name.startsWith("DataCollect")) {
                    numPhotos += 1
                }
            }
        }
        viewModel.numPhotosCollected.value = numPhotos
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

            // Sets camera to focus on centre of viewfinder once a minute
            setupAutoFocus()

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                previewUseCase?.setSurfaceProvider(preview_view.surfaceProvider)
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
            R.id.flash_button -> updateFlashMode(flash_button.isSelected)
        }
    }

    private fun updateFlashMode(flashMode: Boolean) {
        flash_button.isSelected = !flashMode
        if (camera!!.cameraInfo.hasFlashUnit()) {
            camera!!.cameraControl.enableTorch(!flashMode)
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCaptureUseCase ?: return
        // Create a time-stamped output file to hold the image
        val photoFile = File(
            viewModel.outputDirectory,
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
                    getNumPhotosCollected()
                }
            }
        )
    }

    private fun setupAutoFocus() {
        preview_view.afterMeasured {
            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                preview_view.width.toFloat(), preview_view.height.toFloat()
            )
            val centerWidth = preview_view.width.toFloat() / 2
            val centerHeight = preview_view.height.toFloat() / 2
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