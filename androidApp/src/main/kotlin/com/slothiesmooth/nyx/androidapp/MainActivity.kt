package com.slothiesmooth.nyx.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.slothiesmooth.nyx.client.app.presentation.App
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

/** Single-activity host. Keeps the OS splash on-screen until the shared [App] draws its first frame. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Registers the ActivityResultRegistry FileKit's suspend pickers (image + camera) need.
        FileKit.init(this)

        var firstFrameDrawn = false
        splashScreen.setKeepOnScreenCondition { !firstFrameDrawn }

        enableEdgeToEdge()
        setContent {
            App()
            LaunchedEffect(Unit) { firstFrameDrawn = true }
        }
    }
}
