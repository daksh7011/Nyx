package com.slothiesmooth.nyx.designlibrary.snapshot

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import com.slothiesmooth.nyx.designlibrary.molecules.NxCardSample
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyStateSample
import com.slothiesmooth.nyx.designlibrary.molecules.NxFabSample
import com.slothiesmooth.nyx.designlibrary.molecules.NxImageTileSample
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlaySample
import com.slothiesmooth.nyx.designlibrary.molecules.NxSectionHeaderSample
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBarSample
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NxMoleculesSnapshotTest(private val palette: NxPalette) {

    @get:Rule
    val paparazzi: Paparazzi = nxPaparazzi()

    @Test fun card() = snapshot { NxCardSample() }

    @Test fun topBar() = snapshot { NxTopBarSample() }

    @Test fun fab() = snapshot { NxFabSample() }

    @Test fun imageTile() = snapshot { NxImageTileSample() }

    @Test fun sectionHeader() = snapshot { NxSectionHeaderSample() }

    @Test fun emptyState() = snapshot { NxEmptyStateSample() }

    @Test fun progressOverlay() = snapshot { NxProgressOverlaySample() }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot {
            NxTheme(palette = palette) {
                Surface(color = MaterialTheme.colorScheme.background) { content() }
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun palettes(): List<NxPalette> = NxPalette.entries
    }
}
