package com.example.androidcamerard.views

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.androidcamerard.R
import com.example.androidcamerard.databinding.FragmentCameraOutputBinding
import com.example.androidcamerard.imagelabelling.ImageLabelsAdapter
import com.example.androidcamerard.viewmodel.CameraViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.fragment_camera_output.*
import kotlinx.android.synthetic.main.image_labelling_bottom_sheet.*
import kotlinx.android.synthetic.main.image_labelling_bottom_sheet.bottom_sheet

class CameraOutputFragment : Fragment(), View.OnClickListener {

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var imageLabelsRecyclerView: RecyclerView? = null
    private var bottomSheetTitleView: TextView? = null
    private var slidingSheetUpFromHiddenState: Boolean = false
    private lateinit var retakePhotoButton: Button
    private lateinit var returnHomeButton: Button

    private val viewModel: CameraViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(
            R.layout.fragment_camera_output,
            container,
            false
        )

        view.findViewById<ImageView>(R.id.camera_output_imageview).apply {
            Glide.with(this@CameraOutputFragment).load(viewModel.photoFilename.value).into(this)
        }

        retakePhotoButton = view.findViewById<Button>(R.id.retake_photo_button).apply {
            setOnClickListener(this@CameraOutputFragment)
        }
        returnHomeButton = view.findViewById<Button>(R.id.return_home_button).apply {
            setOnClickListener(this@CameraOutputFragment)
        }

        return view
    }


    override fun onResume() {
        super.onResume()
        setUpBottomSheet()
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun setUpBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior?.setBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Log.d(TAG, "Bottom sheet new state: $newState")

                    when (newState) {
                        BottomSheetBehavior.STATE_COLLAPSED,
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> slidingSheetUpFromHiddenState = false
                        BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    return
                }
            })

        bottomSheetTitleView = requireView().findViewById(R.id.bottom_sheet_title)
        imageLabelsRecyclerView = requireView().findViewById<RecyclerView>(R.id.image_labels_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ImageLabelsAdapter(viewModel.imageLabels.value)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.return_home_button ->
                findNavController().navigate(R.id.action_cameraOutputFragment_to_startFragment)
            R.id.retake_photo_button -> {
                findNavController().popBackStack()
            }
        }
    }

    companion object {
        private const val TAG = "CameraOutputFragment"
    }
}