package com.example.androidcamerard.views

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidcamerard.R
import com.example.androidcamerard.utils.Utils

class StartFragment : Fragment() {

    enum class DetectionMode(val titleResId: Int, val subtitleResId: Int) {
        ILC_LIVE(R.string.mode_ilc_live_title, R.string.mode_ilc_live__subtitle),
        ILC_STATIC(R.string.mode_ilc_static_title, R.string.mode_ilc_static_subtitle),
        DC_LIVE(R.string.mode_dc_title, R.string.mode_dc_subtitle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_start, container, false)
        view.findViewById<RecyclerView>(R.id.select_mode_recyclerview).apply {
            setHasFixedSize(true)
            adapter = ModeItemAdapter(DetectionMode.values())
            layoutManager = LinearLayoutManager(context)
        }
        return view
    }

    private inner class ModeItemAdapter(private val detectionModes: Array<DetectionMode>) :
        RecyclerView.Adapter<ModeItemAdapter.ModeItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModeItemViewHolder {
            return ModeItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.fragment_start_mode_recyclerview_item, parent, false
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
                        DetectionMode.ILC_LIVE -> findNavController()
                            .navigate(R.id.action_startFragment_to_imageLabellingLiveFragment)
                        DetectionMode.DC_LIVE -> findNavController()
                            .navigate(R.id.action_startFragment_to_dataCollectionFragment)
                        else -> Utils.openImagePicker(activity)
                    }
                }
            }
        }
    }
}
