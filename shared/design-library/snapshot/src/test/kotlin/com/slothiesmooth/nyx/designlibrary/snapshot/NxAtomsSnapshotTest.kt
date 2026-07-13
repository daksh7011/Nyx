package com.slothiesmooth.nyx.designlibrary.snapshot

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import com.slothiesmooth.nyx.designlibrary.atoms.NxButtonSample
import com.slothiesmooth.nyx.designlibrary.atoms.NxChipSample
import com.slothiesmooth.nyx.designlibrary.atoms.NxFieldSample
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButtonSample
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconSetSample
import com.slothiesmooth.nyx.designlibrary.atoms.NxTextSample
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NxAtomsSnapshotTest(private val palette: NxPalette) {

    @get:Rule
    val paparazzi: Paparazzi = nxPaparazzi()

    @Test fun text() = snapshot { NxTextSample() }

    @Test fun icons() = snapshot { NxIconSetSample() }

    @Test fun iconButtons() = snapshot { NxIconButtonSample() }

    @Test fun buttons() = snapshot { NxButtonSample() }

    @Test fun fields() = snapshot { NxFieldSample() }

    @Test fun chips() = snapshot { NxChipSample() }

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
