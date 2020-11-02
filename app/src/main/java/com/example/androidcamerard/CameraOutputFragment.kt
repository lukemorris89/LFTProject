package com.example.androidcamerard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.example.androidcamerard.databinding.FragmentCameraOutputBinding


class CameraOutputFragment : Fragment() {

    private lateinit var binding: FragmentCameraOutputBinding

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
            findNavController().navigate(R.id.action_cameraOutputFragment_to_cameraFragment)
        }

        binding.returnHomeButton.setOnClickListener {
            findNavController().navigate(R.id.action_cameraOutputFragment_to_startFragment)
        }

        return binding.root
    }
}