package com.example.androidcamerard.views

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.androidcamerard.R
import com.example.androidcamerard.databinding.FragmentImageAnalysisBinding
import com.example.androidcamerard.viewModels.ImageLabellingAnalysisViewModel
import com.example.androidcamerard.ml.Model
import com.example.androidcamerard.recognition.Recognition
import com.example.androidcamerard.recognition.RecognitionAdapter
import com.example.androidcamerard.utils.PHOTO_FILENAME_KEY
import com.example.androidcamerard.utils.SOURCE_IMAGE_CAPTURE
import com.example.androidcamerard.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.fragment_image_analysis.*
import kotlinx.android.synthetic.main.image_analysis_bottom_sheet.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.TensorImage


class ImageAnalysisFragment : Fragment(), View.OnClickListener {

    // Data binding
    private lateinit var binding: FragmentImageAnalysisBinding

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private var slidingSheetUpFromHiddenState: Boolean = false
    private var fromCapture: Boolean? = null

    private val args: ImageAnalysisFragmentArgs by navArgs()

    private val viewModel: ImageLabellingAnalysisViewModel by sharedViewModel()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_image_analysis,
                container,
                false
            )
        if (arguments != null) {
            fromCapture = args.source == SOURCE_IMAGE_CAPTURE
        }

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        setUpUI()
        analyzeStaticImage()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpBottomSheet()
    }

    private fun setUpUI() {
        binding.imageAnalysisBottomSheetInclude.returnHomeButton.setOnClickListener(this@ImageAnalysisFragment)
        binding.imageAnalysisBottomSheetInclude.actionButton.setOnClickListener(this@ImageAnalysisFragment)
        binding.topActionBarImageAnalysisInclude.backButton.setOnClickListener(this@ImageAnalysisFragment)

        if (fromCapture!!) {
            binding.imageAnalysisBottomSheetInclude.actionButton.apply {
                text = getString(R.string.retake_photo)
            }
            binding.cameraOutputImageview.apply {
                setImageBitmap(viewModel.capturedImageBitmap.value)
            }
        } else {
            binding.imageAnalysisBottomSheetInclude.actionButton.apply {
                text = getString(R.string.gallery)
            }
            binding.cameraOutputImageview.apply {
                Glide.with(this).load(requireArguments().getString(PHOTO_FILENAME_KEY)).into(this)
            }
        }
    }

    private fun setUpBottomSheet() {
        bottomSheetBehavior =
            BottomSheetBehavior.from(binding.imageAnalysisBottomSheetInclude.bottomSheet)
        binding.imageAnalysisBottomSheetInclude.bottomSheetTitleView.setOnClickListener {
            when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_HIDDEN,
                BottomSheetBehavior.STATE_COLLAPSED -> bottomSheetBehavior.state =
                    BottomSheetBehavior.STATE_HALF_EXPANDED
                else -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        bottomSheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN,
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            slidingSheetUpFromHiddenState = false
                            binding.imageAnalysisBottomSheetInclude.expandArrow.setImageResource(R.drawable.expand_up_24)
                        }
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                            slidingSheetUpFromHiddenState = false
                            binding.imageAnalysisBottomSheetInclude.expandArrow.setImageResource(R.drawable.expand_down_24)
                        }
                        BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    return
                }
            })

        binding.imageAnalysisBottomSheetInclude.imageLabelsRecyclerView.apply {
            val viewAdapter = RecognitionAdapter(requireContext())
            adapter = viewAdapter

            viewModel.recognitionList.observe(viewLifecycleOwner, {
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
            viewModel.capturedImageBitmap.value!!
        } else {
            MediaStore.Images.Media.getBitmap(
                requireContext().contentResolver,
                Uri.parse(requireArguments().getString(PHOTO_FILENAME_KEY))
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
        viewModel.updateData(items)
    }

    companion object {
        private const val TAG = "CameraOutputFragment"
        private const val MAX_RESULT_DISPLAY = 2 // Maximum number of results displayed

    }
}