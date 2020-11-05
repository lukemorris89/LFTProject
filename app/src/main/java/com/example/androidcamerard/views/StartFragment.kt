package com.example.androidcamerard.views

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidcamerard.R
import com.example.androidcamerard.viewmodel.CameraViewModel

class StartFragment : Fragment() {

    enum class DetectionMode(val titleResId: Int, val subtitleResId: Int) {
//        CUSTOM_MODEL_LIVE(R.string.custom_model_live_title, R.string.custom_model_live_subtitle),
        ODT_LIVE(R.string.mode_odt_live_title, R.string.mode_odt_live__subtitle),
//        ODT_STATIC(R.string.mode_odt_static_title, R.string.mode_odt_static_subtitle)
    }

    private val viewModel: CameraViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_start, container, false)
        view.findViewById<RecyclerView>(R.id.select_mode_recyclerview).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = ModeItemAdapter(DetectionMode.values())
        }

        return view
    }



    private inner class ModeItemAdapter internal constructor(private val detectionModes: Array<DetectionMode>) :
        RecyclerView.Adapter<ModeItemAdapter.ModeItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModeItemViewHolder {
            return ModeItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.mode_item, parent, false
                    )
            )
        }

        override fun onBindViewHolder(modeItemViewHolder: ModeItemViewHolder, position: Int) =
            modeItemViewHolder.bindDetectionMode(detectionModes[position])

        override fun getItemCount(): Int = detectionModes.size

        private inner class ModeItemViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {

            private val titleView: TextView = view.findViewById(R.id.mode_title)
            private val subtitleView: TextView = view.findViewById(R.id.mode_subtitle)

            internal fun bindDetectionMode(detectionMode: DetectionMode) {
                titleView.setText(detectionMode.titleResId)
                subtitleView.setText(detectionMode.subtitleResId)
                itemView.setOnClickListener {
                    val activity = activity as Activity
                    when (detectionMode) {
//                        DetectionMode.CUSTOM_MODEL_LIVE ->
//                            findNavController().navigate(R.id.action_startFragment_to_labelDetectionLiveFragment)
//                        DetectionMode.ODT_STATIC -> Utils.openImagePicker(activity)
                        DetectionMode.ODT_LIVE ->
                            findNavController().navigate(R.id.action_startFragment_to_objectDetectionLiveFragment)
                    }
                }
            }
        }
    }
}