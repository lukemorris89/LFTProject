package com.example.androidcamerard.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.androidcamerard.R
import com.example.androidcamerard.databinding.FragmentCameraOutputBinding
import com.example.androidcamerard.viewmodel.PhotoViewModel

class CameraOutputFragment : Fragment() {

    private lateinit var binding: FragmentCameraOutputBinding

    private val viewModel: PhotoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_camera_output,
            container,
            false
        )

        binding.retakePhotoButton.setOnClickListener {
            findNavController().navigate(R.id.action_cameraOutputFragment_to_labelDetectionLiveFragment)
        }

        binding.returnHomeButton.setOnClickListener {
            findNavController().navigate(R.id.action_cameraOutputFragment_to_startFragment)
        }

        viewModel.photoFilename.observe(viewLifecycleOwner, {
            Glide.with(requireContext()).load(viewModel.photoFilename.value).centerCrop()
                .into(binding.imageView)
        })


        return binding.root
    }
}