/*
* Copyright 2020 Google LLC. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.androidcamerard.utils

import android.content.Context
import android.preference.PreferenceManager
import com.example.androidcamerard.R
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

/** Utility class to retrieve shared preferences.  */
object PreferenceUtils {

    fun isCameraLiveViewportEnabled(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(R.string.pref_key_camera_live_viewport)
        return sharedPreferences.getBoolean(prefKey, false)
    }

    fun getCameraXTargetResolution(context: Context): android.util.Size? {
        val prefKey = context.getString(R.string.pref_key_camerax_target_resolution)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return try {
            android.util.Size.parseSize(sharedPreferences.getString(prefKey, null))
        } catch (e: java.lang.Exception) {
            null
        }
    }

    fun getObjectDetectorOptionsForLivePreview(context: Context?): ObjectDetectorOptions? {
        return R.string.pref_key_live_preview_object_detector_enable_classification.getObjectDetectorOptions(
            context!!,
            ObjectDetectorOptions.STREAM_MODE
        )
    }

    private fun Int.getObjectDetectorOptions(
        context: Context,
        mode: Int
    ): ObjectDetectorOptions? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val enableClassification =
            sharedPreferences.getBoolean(context.getString(this), true)
        val builder = ObjectDetectorOptions.Builder().setDetectorMode(mode)
        if (enableClassification) {
            builder.enableClassification()
        }
        return builder.build()
    }

}