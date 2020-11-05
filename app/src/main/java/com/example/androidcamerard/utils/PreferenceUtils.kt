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
import androidx.annotation.StringRes
import com.example.androidcamerard.R
import com.example.androidcamerard.camera.CameraSizePair
import com.google.android.gms.common.images.Size
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase.DetectorMode
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

/** Utility class to retrieve shared preferences.  */
object PreferenceUtils {

    fun saveStringPreference(context: Context, @StringRes prefKeyId: Int, value: String?) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(context.getString(prefKeyId), value)
            .apply()
    }

    fun getUserSpecifiedPreviewSize(context: Context): CameraSizePair? {
        return try {
            val previewSizePrefKey = context.getString(R.string.pref_key_rear_camera_preview_size)
            val pictureSizePrefKey = context.getString(R.string.pref_key_rear_camera_picture_size)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            CameraSizePair(
                Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)),
                Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null))
            )
        } catch (e: Exception) {
            null
        }
    }

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
        return getObjectDetectorOptions(
            context!!,
            R.string.pref_key_live_preview_object_detector_enable_multiple_objects,
            R.string.pref_key_live_preview_object_detector_enable_classification,
            ObjectDetectorOptions.STREAM_MODE
        )
    }

    private fun getObjectDetectorOptions(
        context: Context,
        @StringRes prefKeyForMultipleObjects: Int,
        @StringRes prefKeyForClassification: Int,
        @DetectorMode mode: Int
    ): ObjectDetectorOptions? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val enableMultipleObjects =
            sharedPreferences.getBoolean(context.getString(prefKeyForMultipleObjects), false)
        val enableClassification =
            sharedPreferences.getBoolean(context.getString(prefKeyForClassification), true)
        val builder = ObjectDetectorOptions.Builder().setDetectorMode(mode)
        if (enableMultipleObjects) {
            builder.enableMultipleObjects()
        }
        if (enableClassification) {
            builder.enableClassification()
        }
        return builder.build()
    }

}