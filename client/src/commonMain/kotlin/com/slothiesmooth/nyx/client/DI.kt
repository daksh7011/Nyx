package com.slothiesmooth.nyx.client

import com.slothiesmooth.nyx.client.app.appModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

private var started: KoinApplication? = null

/** Starts the global Koin graph once, layering the platform module under the app graph. */
fun initKoin(platformModule: org.koin.core.module.Module): KoinApplication {
    started?.let { return it }
    return startKoin { modules(appModule(platformModule)) }.also { started = it }
}
