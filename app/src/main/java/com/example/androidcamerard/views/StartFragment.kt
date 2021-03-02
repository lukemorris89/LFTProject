package com.example.androidcamerard.views

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidcamerard.R
import com.example.androidcamerard.databinding.FragmentStartBinding
import com.example.androidcamerard.utils.DetectionMode
import com.example.androidcamerard.utils.Utils

class StartFragment : Fragment() {

    // Data binding
    private lateinit var binding: FragmentStartBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_start,
                container,
                false
            )
        binding.selectModeRecyclerview.apply {
            setHasFixedSize(true)
            adapter = ModeItemAdapter(DetectionMode.values())
            layoutManager = LinearLayoutManager(context)
        }
        return binding.root
    }

    private inner class ModeItemAdapter(private val detectionModes: Array<DetectionMode>) :
        RecyclerView.Adapter<ModeItemAdapter.ModeItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModeItemViewHolder {
            return ModeItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.start_mode_recyclerview_item, parent, false
                    )
            )
        }

        override fun onBindViewHolder(modeItemViewHolder: ModeItemViewHolder, position: Int) =
            modeItemViewHolder.bindDetectionMode(detectionModes[position])

        override fun getItemCount(): Int = detectionModes.size

        private inner class ModeItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            private val titleView: TextView = view.findViewById(R.id.mode_title)
            private val subtitleView: TextView = view.findViewById(R.id.mode_subtitle)

            fun bindDetectionMode(detectionMode: DetectionMode) {
                titleView.setText(detectionMode.titleResId)
                subtitleView.setText(detectionMode.subtitleResId)
                itemView.setOnClickListener {
                    val activity = activity as Activity
                    when (detectionMode) {
                        DetectionMode.ILCTF_LIVE -> {
                            val action =
                                StartFragmentDirections.actionStartFragmentToImageLabellingLiveFragment()
                            findNavController().navigate(action)
                        }
                        DetectionMode.ILCFB_LIVE -> {
                            val action =
                                StartFragmentDirections.actionStartFragmentToImageLabellingLiveFirebaseFragment()
                            findNavController().navigate(action)
                        }
                        DetectionMode.DC_LIVE -> {
                            val action =
                                StartFragmentDirections.actionStartFragmentToDataCollectionFragment()
                            findNavController().navigate(action)
                        }
                        else -> Utils.openImagePicker(activity)
                    }
                }
            }
        }
    }
}
