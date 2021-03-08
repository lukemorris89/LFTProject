package com.example.androidcamerard.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.androidcamerard.R
import com.example.androidcamerard.utils.PHOTO_FILENAME_KEY
import com.example.androidcamerard.utils.Utils

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = findNavController(R.id.main_nav_host_fragment)
        navController.navigate(R.id.startFragment)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Utils.REQUEST_CODE_PHOTO_LIBRARY &&
            resultCode == Activity.RESULT_OK &&
            data != null
        ) {
            val args = bundleOf(
                "SOURCE" to SOURCE,
                PHOTO_FILENAME_KEY to data.data.toString()
            )
            navController.navigate(R.id.imageAnalysisFragment, args)
        }
    }

    companion object {
        private const val SOURCE = "ImagePicker"
    }
}