package com.example.androidcamerard.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.androidcamerard.R
import com.example.androidcamerard.utils.Utils
import com.example.androidcamerard.viewmodel.CameraViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.myNavHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        navController.navigate(R.id.startFragment)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Utils.REQUEST_CODE_PHOTO_LIBRARY &&
            resultCode == Activity.RESULT_OK &&
            data != null
        ) {
            viewModel.photoFilename.value = data.data
            val args = bundleOf("SOURCE" to SOURCE)
            navController.navigate(R.id.imageAnalysisFragment, args)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val SOURCE = "ImagePicker"
    }
}