/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidcamerard.imagelabelling

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.example.androidcamerard.R
import com.google.mlkit.vision.label.ImageLabel


/** Presents the list of product items from cloud product search.  */
class ImageLabelsAdapter(private val context: Context, private val labelList: List<ImageLabel>?) : Adapter<ImageLabelsAdapter.LabelViewHolder>() {

    class LabelViewHolder private constructor(view: View) : RecyclerView.ViewHolder(view) {

        private val labelView: TextView = view.findViewById(R.id.image_label)
        private val labelConfidenceView: TextView = view.findViewById(R.id.image_label_confidence)

        fun bindLabel(label: ImageLabel, context: Context) {
            labelView.text = label.text
            labelConfidenceView.text = context.resources.getString(R.string.image_labelling_results_confidence, "%.2f".format(label.confidence * 100))
        }

        companion object {
            fun create(parent: ViewGroup) =
                LabelViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.bottom_sheet_recyclerview_item, parent, false))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder =
        LabelViewHolder.create(parent)

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        holder.bindLabel(labelList!![position], context)
    }

    override fun getItemCount(): Int = labelList!!.size
}
