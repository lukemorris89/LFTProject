package com.example.androidcamerard.views

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.androidcamerard.R
import com.example.androidcamerard.databinding.FragmentCameraOutputBinding
import com.example.androidcamerard.databinding.FragmentStaticImageAnalysisBinding
import com.example.androidcamerard.viewmodel.PhotoViewModel

class StaticImageAnalysisFragment : Fragment() {
    private lateinit var binding: FragmentStaticImageAnalysisBinding

    private val viewModel: PhotoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_static_image_analysis,
            container,
            false
        )

        binding.returnHomeButton.setOnClickListener {
            findNavController().navigate(R.id.action_staticImageAnalysisFragment_to_startFragment)
        }

        viewModel.photoFilename.observe(viewLifecycleOwner, {
            Glide.with(requireContext()).load(viewModel.photoFilename.value).centerCrop()
                .into(binding.imageView)
        })


        return binding.root
    }
}