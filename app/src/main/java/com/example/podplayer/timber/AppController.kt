package com.example.podplayer.timber

import android.app.Application
import timber.log.Timber
import com.example.podplayer.BuildConfig

class AppController : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

}