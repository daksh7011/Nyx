package com.slothiesmooth.nyx.androidapp

import android.app.Application
import com.slothiesmooth.nyx.client.initKoin

/** Starts the Koin graph with the Android platform module before any Activity or ViewModel resolves. */
class NyxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidPlatformModule(this))
    }
}
