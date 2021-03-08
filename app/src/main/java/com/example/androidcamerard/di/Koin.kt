package com.example.androidcamerard.di

import android.content.Context
import com.example.androidcamerard.viewModels.DataCollectionViewModel
import com.example.androidcamerard.viewModels.ImageLabellingAnalysisViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        androidApplication().getSharedPreferences(
            "cameraX_shared_prefs",
            Context.MODE_PRIVATE
        )
    }

    viewModel { ImageLabellingAnalysisViewModel() }
    viewModel { DataCollectionViewModel() }
}