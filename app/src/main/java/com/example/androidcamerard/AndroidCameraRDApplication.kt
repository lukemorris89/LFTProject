package com.example.androidcamerard

import android.app.Application
import com.example.androidcamerard.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AndroidCameraRDApplication : Application() {

    companion object {
        lateinit var instance: AndroidCameraRDApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin {
            androidContext(this@AndroidCameraRDApplication)
            modules(appModule)
        }
    }
}