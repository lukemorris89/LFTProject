package com.example.androidcamerard.imagelabelling

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.example.androidcamerard.R
import com.example.androidcamerard.camera.GraphicOverlay
import com.example.androidcamerard.processor.VisionProcessorBase
import com.example.androidcamerard.viewmodel.CameraViewModel
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabelerOptionsBase
import com.google.mlkit.vision.label.ImageLabeling
import java.io.IOException

/** Custom InputImage Classifier Demo.  */
class ImageLabellingProcessor(private val context: Context, options: ImageLabelerOptionsBase, view: View, private val viewModel: CameraViewModel) :
    VisionProcessorBase<List<ImageLabel>>(context) {

    private val imageLabeler: ImageLabeler = ImageLabeling.getClient(options)
    private val resultsText: TextView = view.findViewById(R.id.overlay_results_textview)
    private val cameraImageButton: ImageButton = view.findViewById(R.id.photo_capture_button)

    override fun stop() {
        super.stop()
        try {
            imageLabeler.close()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Exception thrown while trying to close ImageLabelerClient: $e"
            )
        }
    }

    override fun detectInImage(image: InputImage): Task<List<ImageLabel>> {
        return imageLabeler.process(image)
    }

    override fun onSuccess(results: List<ImageLabel>, graphicOverlay: GraphicOverlay) {
        if (results.isEmpty()) {
            resultsText.text = context.resources.getString(R.string.point_your_camera_at_the_test)
            cameraImageButton.isEnabled = false
            cameraImageButton.setImageResource(R.drawable.ic_photo_camera_disabled_v24)
        }
        else {
            if (results.isNotEmpty()) {
                viewModel.imageLabels.value = results
                resultsText.text = context.resources.getString(R.string.image_labelling_results, results[0].text,  "%.2f".format(results[0].confidence * 100))
                if (results[0].text == "Hand") {
                    cameraImageButton.isEnabled = true
                    cameraImageButton.setImageResource(R.drawable.ic_photo_camera_24)
                }
                else {
                    cameraImageButton.isEnabled = false
                    cameraImageButton.setImageResource(R.drawable.ic_photo_camera_disabled_v24)
                }
            }
        }
        logExtrasForTesting(results)
    }


    override fun onFailure(e: Exception) {
        Log.w(TAG, "Label detection failed.$e")
    }


    companion object {
        private const val TAG = "LabelDetectorProcessor"

        private fun logExtrasForTesting(labels: List<ImageLabel>?) {
            if (labels == null) {
                Log.v(MANUAL_TESTING_LOG, "No labels detected")
            } else {
                for (label in labels) {
                    Log.v(
                        MANUAL_TESTING_LOG,
                        String.format("Label %s, confidence %f", label.text, label.confidence)
                    )
                }
            }
        }
    }
}
