package com.agydroid

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

import javax.inject.Inject

@HiltAndroidApp
class AgyDroidApp : Application() {

    @Inject
    lateinit var internalEngineManager: com.agydroid.engine.InternalEngineManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        internalEngineManager.startInternalServer()
    }
}
