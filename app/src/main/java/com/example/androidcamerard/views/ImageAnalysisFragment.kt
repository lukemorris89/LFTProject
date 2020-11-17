package com.example.androidcamerard.views

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.androidcamerard.R
import com.example.androidcamerard.imagelabelling.ImageLabelsAdapter
import com.example.androidcamerard.utils.Utils
import com.example.androidcamerard.viewmodel.CameraViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.android.synthetic.main.fragment_image_analysis.*
import java.io.IOException

class ImageAnalysisFragment : Fragment(), View.OnClickListener {

    private lateinit var imageLabelsRecyclerView: RecyclerView
    private lateinit var bottomSheetTitleView: View
    private lateinit var actionButton: Button
    private lateinit var returnHomeButton: Button
    private lateinit var backButton: ImageView
    private lateinit var expandButton: ImageView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private var slidingSheetUpFromHiddenState: Boolean = false
    private var fromCapture : Boolean? = null

    private val labelData: MutableList<ImageLabel> = mutableListOf()
    private val viewModel: CameraViewModel by activityViewModels()

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
            fromCapture = requireArguments().get("SOURCE")!! == "ImageCapture"
        }
        expandButton = view.findViewById(R.id.expand_arrow)
        actionButton = view.findViewById<Button>(R.id.action_button).apply {
            text = if (fromCapture!!) getString(R.string.retake_photo) else getString(R.string.gallery)
            setOnClickListener(this@ImageAnalysisFragment)
        }
        returnHomeButton = view.findViewById<Button>(R.id.return_home_button).apply {
            setOnClickListener(this@ImageAnalysisFragment)
        }
        backButton = view.findViewById<ImageView>(R.id.back_button).apply {
            setOnClickListener(this@ImageAnalysisFragment)
        }

        view.findViewById<ImageView>(R.id.camera_output_imageview).apply {
            if (fromCapture!!) {
                setImageBitmap(viewModel.croppedBitmap.value)
            } else {
                Glide.with(this).load(viewModel.photoFilename.value).into(this)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }

        analyzeStaticImage()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpBottomSheet()

        imageLabelsRecyclerView = requireView().findViewById<RecyclerView>(R.id.image_labels_recycler_view)
            .apply {
                adapter = ImageLabelsAdapter(context, labelData)
                layoutManager = LinearLayoutManager(context)
            }
    }


    override fun onResume() {
        super.onResume()
        setUpBottomSheet()
    }

    private fun setUpBottomSheet() {
        bottomSheetTitleView = requireView().findViewById(R.id.transparent_expand_view)
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetTitleView.setOnClickListener {
            Log.d(TAG, "Title view clicked")
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
                    Log.d(TAG, "Bottom sheet new state: $newState")

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
    }

    override fun onClick(view: View) {
        val activity = activity as Activity
        when (view.id) {
            R.id.return_home_button ->
                findNavController().navigate(R.id.action_cameraOutputFragment_to_startFragment)
            R.id.action_button -> {
                if (fromCapture!!) findNavController().popBackStack()
                else Utils.openImagePicker(activity)
            }
            R.id.back_button ->
                findNavController().popBackStack()
        }
    }

    private fun analyzeStaticImage() {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(requireContext(), viewModel.photoFilename.value!!)

            val options = ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build()
            val imageLabeler =
                ImageLabeling.getClient(options)

            imageLabeler.process(image)
                .addOnSuccessListener { labels ->
                    labelData.clear()
                    for (label in labels) {
                        labelData.add(label)
                    }
                    imageLabelsRecyclerView.adapter?.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, e.message.toString())
                }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "CameraOutputFragment"
    }
}