package com.slothiesmooth.nyx.designlibrary.snapshot

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.slothiesmooth.nyx.designlibrary.tokens.LocalNxType
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxTypeFor
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext

// Real JetBrains Mono for the goldens. The production path — NxTheme's compose-resources
// Font(Res.font.*) — is async and loses the race to Paparazzi's single layoutlib frame (Roboto).
// androidx Font(resId) is FontLoadingStrategy.Blocking, so layoutlib resolves the Typeface in that
// one frame, and Paparazzi renders res/font faces (its ResourcesCompat.loadFont hook keys off the
// "res/" path). All five weights are registered so bold/semibold use the real masters.
private val jetBrainsMono = FontFamily(
    Font(R.font.jetbrainsmono_regular, FontWeight.Normal),
    Font(R.font.jetbrainsmono_medium, FontWeight.Medium),
    Font(R.font.jetbrainsmono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrainsmono_bold, FontWeight.Bold),
    Font(R.font.jetbrainsmono_italic, FontWeight.Normal, FontStyle.Italic),
)

/**
 * Shared Paparazzi factory: renders on a Pixel 5 and SHRINKs the frame to each sample's own bounds.
 * Recorded on Linux (dev machine) to match the ubuntu CI runner's font rendering.
 */
fun nxPaparazzi(): Paparazzi = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_5,
    renderingMode = SessionParams.RenderingMode.SHRINK,
    showSystemUi = false,
)

/**
 * Renders a sample through [NxTheme] on its palette-appropriate background, overriding only the type
 * seam with the blocking JetBrains Mono family. The override nests inside [NxTheme] because NxTheme
 * re-provides LocalNxType (with the async production font) and still owns colors, dimensions, etc.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun NxSnapshot(palette: NxPalette, content: @Composable () -> Unit) {
    // Paparazzi runs neither compose-resources' AndroidContextProvider (a ContentProvider) nor sets
    // LocalInspectionMode, so PreviewContextConfigurationEffect no-ops. Set the reader's context
    // directly from LocalContext (Paparazzi's) so stringResource(Res.string.*) resolves.
    setResourceReaderAndroidContext(LocalContext.current)
    NxTheme(palette = palette) {
        CompositionLocalProvider(LocalNxType provides nxTypeFor(jetBrainsMono)) {
            Surface(color = MaterialTheme.colorScheme.background) { content() }
        }
    }
}
