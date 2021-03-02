package com.example.androidcamerard.views

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.androidcamerard.R
import com.example.androidcamerard.viewModels.CameraViewModel
import com.example.androidcamerard.ml.Model
import com.example.androidcamerard.recognition.Recognition
import com.example.androidcamerard.recognition.RecognitionAdapter
import com.example.androidcamerard.utils.SOURCE_IMAGE_CAPTURE
import com.example.androidcamerard.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.fragment_image_analysis.*
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.TensorImage


class ImageAnalysisFragment : Fragment(), View.OnClickListener {

    private lateinit var imageOutputView: ImageView
    private lateinit var imageLabelsRecyclerView: RecyclerView
    private lateinit var bottomSheetTitleView: View
    private lateinit var actionButton: Button
    private lateinit var returnHomeButton: Button
    private lateinit var backButton: ImageView
    private lateinit var expandButton: ImageView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private var slidingSheetUpFromHiddenState: Boolean = false
    private var fromCapture: Boolean? = null

    private val args: ImageAnalysisFragmentArgs by navArgs()

    private val cameraViewModel: CameraViewModel by activityViewModels()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_image_analysis,
            container,
            false
        )
        if (arguments != null) {
            fromCapture = args.source == SOURCE_IMAGE_CAPTURE
        }

        setUpUI(view)

        analyzeStaticImage()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpBottomSheet()
    }

    private fun setUpUI(view: View) {
        expandButton = view.findViewById(R.id.expand_arrow)

        actionButton = view.findViewById<Button>(R.id.action_button).apply {
            text = if (fromCapture!!) {
                getString(R.string.retake_photo)
            } else {
                getString(R.string.gallery)
            }
            setOnClickListener(this@ImageAnalysisFragment)
        }

        returnHomeButton = view.findViewById<Button>(R.id.return_home_button).apply {
            setOnClickListener(this@ImageAnalysisFragment)
        }
        backButton = view.findViewById<ImageView>(R.id.back_button).apply {
            setOnClickListener(this@ImageAnalysisFragment)
        }

        imageOutputView = view.findViewById<ImageView>(R.id.camera_output_imageview).apply {
            if (fromCapture!!) {
                setImageBitmap(cameraViewModel.capturedImageBitmap.value)
            } else {
                Glide.with(this).load(cameraViewModel.photoFilename.value).into(this)
            }
        }
    }

    private fun setUpBottomSheet() {
        bottomSheetTitleView = requireView().findViewById(R.id.transparent_expand_view)
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetTitleView.setOnClickListener {
            when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_HIDDEN,
                BottomSheetBehavior.STATE_COLLAPSED -> bottomSheetBehavior.state =
                    BottomSheetBehavior.STATE_HALF_EXPANDED
                else -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        bottomSheetBehavior.setBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN,
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            slidingSheetUpFromHiddenState = false
                            expandButton.setImageResource(R.drawable.expand_up_24)
                        }
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                            slidingSheetUpFromHiddenState = false
                            expandButton.setImageResource(R.drawable.expand_down_24)
                        }
                        BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    return
                }
            })

        imageLabelsRecyclerView =
            requireView().findViewById<RecyclerView>(R.id.image_labels_recycler_view).apply {
                val viewAdapter = RecognitionAdapter(requireContext())
                adapter = viewAdapter

                cameraViewModel.recognitionList.observe(viewLifecycleOwner, {
                    viewAdapter.submitList(it)
                })
            }
    }

    override fun onClick(view: View) {
        val activity = activity as Activity
        when (view.id) {
            R.id.return_home_button -> {
                val action =
                    ImageAnalysisFragmentDirections.actionCameraOutputFragmentToStartFragment()
                findNavController().navigate(action)
            }
            R.id.action_button -> {
                if (fromCapture!!) findNavController().popBackStack()
                else Utils.openImagePicker(activity)
            }
            R.id.back_button ->
                findNavController().popBackStack()
        }
    }

    private fun analyzeStaticImage() {
        val model: Model by lazy {

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
            Model.newInstance(requireContext(), options)
        }

        val items = mutableListOf<Recognition>()

        val bitmapImage: Bitmap = if (fromCapture!!) {
            cameraViewModel.capturedImageBitmap.value!!
        } else {
            MediaStore.Images.Media.getBitmap(
                requireContext().contentResolver,
                cameraViewModel.photoFilename.value
            )
        }
        val tfImage = TensorImage.fromBitmap(bitmapImage)

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
        cameraViewModel.updateData(items)
    }

    companion object {
        private const val TAG = "CameraOutputFragment"
        private const val MAX_RESULT_DISPLAY = 2 // Maximum number of results displayed

    }
}