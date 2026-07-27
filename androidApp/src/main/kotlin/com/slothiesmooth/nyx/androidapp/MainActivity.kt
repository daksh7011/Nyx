package com.slothiesmooth.nyx.androidapp

import android.os.Bundle
import android.view.WindowManager
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

        // Vault app: block screenshots, screen recording, casting to non-secure displays, and blank the
        // Recents thumbnail. Every Nyx screen can show plaintext (encrypt input, decrypt reveal, vault),
        // so FLAG_SECURE is applied to the whole window. Note: this does NOT cover Compose Dialog/Popup
        // windows — if plaintext is ever rendered in one, set DialogProperties(securePolicy = SecureOn).
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

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
