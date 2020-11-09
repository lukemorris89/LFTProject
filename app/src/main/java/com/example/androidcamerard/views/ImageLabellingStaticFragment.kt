package com.example.androidcamerard.views

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.android.synthetic.main.fragment_image_labelling_static.*
import java.io.IOException

class ImageLabellingStaticFragment : Fragment(), View.OnClickListener {

    private lateinit var retakePhotoButton: Button
    private lateinit var returnHomeButton: Button
    private lateinit var expandButton: ImageView
    private lateinit var imageLabelsRecyclerView: RecyclerView
    private lateinit var bottomSheetTitleView: TextView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var slidingSheetUpFromHiddenState: Boolean = false

    private val viewModel: CameraViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_image_labelling_static,
            container,
            false
        )

        expandButton = view.findViewById(R.id.expand_arrow)
        retakePhotoButton = view.findViewById<Button>(R.id.choose_photo_button).apply {
            setOnClickListener(this@ImageLabellingStaticFragment)
        }
        returnHomeButton = view.findViewById<Button>(R.id.return_home_button).apply {
            setOnClickListener(this@ImageLabellingStaticFragment)
        }
        imageLabelsRecyclerView = view.findViewById(R.id.image_labels_recycler_view)
        view.findViewById<ImageView>(R.id.image_labelling_static_imageview).apply {
            Glide.with(this@ImageLabellingStaticFragment).load(viewModel.photoFilename.value).into(this)
        }
        analyzeStaticImage()
        return view
    }


    override fun onResume() {
        super.onResume()
        setUpBottomSheet()
    }

    private fun setUpBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.setBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Log.d(TAG, "Bottom sheet new state: $newState")

                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN,
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            slidingSheetUpFromHiddenState = false
                            expandButton.setImageResource(R.drawable.expand_up_24)
                            expandButton.setOnClickListener {
                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                            }
                        }
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                            slidingSheetUpFromHiddenState = false
                            expandButton.setImageResource(R.drawable.expand_down_24)
                            expandButton.setOnClickListener {
                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                            }
                        }
                        BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    return
                }
            })

        bottomSheetTitleView = requireView().findViewById(R.id.bottom_sheet_title)
    }

    override fun onClick(view: View) {
        val activity = activity as Activity
        when (view.id) {
            R.id.return_home_button ->
                findNavController().popBackStack()
            else -> Utils.openImagePicker(activity)
        }
    }

    private fun analyzeStaticImage() {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(requireContext(), viewModel.photoFilename.value!!)

            val options = ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build()
            val imageLabeler = ImageLabeling.getClient(options)

            imageLabeler.process(image)
                .addOnSuccessListener { labels ->
                    imageLabelsRecyclerView = requireView()
                        .findViewById<RecyclerView>(R.id.image_labels_recycler_view).apply {
                            setHasFixedSize(true)
                            layoutManager = LinearLayoutManager(context)
                            adapter = ImageLabelsAdapter(context, labels)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, e.message.toString())
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "ImageLabelStaticFrag"
    }
}