package com.slothiesmooth.nyx.client

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.feature.vault.basic.data.VaultRepositoryImpl
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ArchiveImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.DeleteImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.GetImageBytesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveArchivedImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveVaultImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.RestoreImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ShareVaultImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.presentation.VaultImageUseCases
import com.slothiesmooth.nyx.feature.vault.basic.presentation.detail.VaultDetailContent
import com.slothiesmooth.nyx.feature.vault.basic.presentation.detail.VaultDetailViewModel
import com.slothiesmooth.nyx.feature.vault.basic.presentation.list.VaultContent
import com.slothiesmooth.nyx.feature.vault.basic.presentation.list.VaultViewModel
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.testsupport.FakeShareSource
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import com.slothiesmooth.nyx.shared.testsupport.id.DeterministicIdGenerator
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import kotlin.time.Instant

/**
 * Drives the real vault journey end to end on device: the actual [VaultContent] grid and
 * [VaultDetailContent] over real [VaultViewModel] / [VaultDetailViewModel] backed by the real
 * [VaultRepositoryImpl] (fake source + file store), seeded through the real [SaveToVaultUseCase] as
 * the encrypt path would. Proves: the stored image lists -> opening it -> archive moves it out of the
 * active view and into the archived view -> restore brings it back -> delete removes it everywhere;
 * and that an archived image only surfaces once the archived toggle is on.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class VaultFlowDeviceTest {

    private val vaultSource = FakeVaultSource()
    private val fileStore = FakeVaultFileStore()
    private val eventBus = DefaultDomainEventBus()
    private val clock = FakeClock(Instant.parse(FIXED_INSTANT))

    private lateinit var listViewModel: VaultViewModel
    private lateinit var detailViewModel: VaultDetailViewModel

    @Before
    fun setup() = runBlocking {
        val repository = VaultRepositoryImpl(vaultSource, fileStore, clock, eventBus)
        val useCases = VaultImageUseCases(
            observeActive = ObserveVaultImagesUseCase(repository),
            observeArchived = ObserveArchivedImagesUseCase(repository),
            getImageBytes = GetImageBytesUseCase(repository),
            archive = ArchiveImageUseCase(repository),
            restore = RestoreImageUseCase(repository),
            delete = DeleteImageUseCase(repository),
        )
        val save = SaveToVaultUseCase(vaultSource, fileStore, DeterministicIdGenerator(), clock, eventBus)
        save(generateCoverPng(), IMAGE_NAME)
        listViewModel = VaultViewModel(useCases, clock)
        detailViewModel = VaultDetailViewModel(
            useCases,
            ShareVaultImageUseCase(repository, FakeShareSource()),
            clock,
        )
    }

    @Test
    fun opensArchivesRestoresAndDeletes() = runComposeUiTest {
        renderVault()

        waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            !listViewModel.state.isLoading && listViewModel.state.active.size == 1
        }
        onNodeWithContentDescription(IMAGE_NAME).assertIsDisplayed()

        onNodeWithContentDescription(IMAGE_NAME).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { !detailViewModel.state.isLoading }
        onNodeWithText(ARCHIVE).assertIsDisplayed()

        onNodeWithText(ARCHIVE).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { detailViewModel.state.isArchived }
        assertEquals("active view empties on archive", 0, listViewModel.state.active.size)
        assertEquals("archived view gains the image", 1, listViewModel.state.archived.size)

        onNodeWithText(RESTORE).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { !detailViewModel.state.isArchived }
        assertEquals("active view regains the image on restore", 1, listViewModel.state.active.size)

        onNodeWithText(DELETE).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            listViewModel.state.active.isEmpty() && listViewModel.state.archived.isEmpty()
        }
        assertEquals("delete purges the active view", 0, runBlocking { vaultSource.countActive() })
    }

    @Test
    fun archivedImageOnlySurfacesUnderTheArchivedToggle() = runComposeUiTest {
        renderVault()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { listViewModel.state.active.size == 1 }

        listViewModel.archive(StegoImageId(SEEDED_ID))
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            listViewModel.state.active.isEmpty() && listViewModel.state.archived.size == 1
        }

        onNodeWithContentDescription(TOGGLE_ARCHIVED).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { listViewModel.state.showArchived }

        onNodeWithText(ARCHIVED_SECTION).assertIsDisplayed()
        onNodeWithContentDescription(IMAGE_NAME).assertIsDisplayed()
    }

    private fun ComposeUiTest.renderVault() = setContent {
        NxTheme(NxPalette.Nardo) {
            listViewModel.bind()
            var showDetail by remember { mutableStateOf(false) }
            if (showDetail) {
                VaultDetailContent(
                    state = detailViewModel.state,
                    onArchive = detailViewModel::archive,
                    onRestore = detailViewModel::restore,
                    onDelete = detailViewModel::delete,
                )
            } else {
                VaultContent(
                    state = listViewModel.state,
                    onOpenDetail = { id ->
                        detailViewModel.load(id)
                        showDetail = true
                    },
                    onToggleArchived = listViewModel::toggleArchived,
                )
            }
        }
    }

    private fun generateCoverPng(): ByteArray {
        val bitmap = Bitmap.createBitmap(COVER_SIZE, COVER_SIZE, Bitmap.Config.ARGB_8888)
        for (y in 0 until COVER_SIZE) {
            for (x in 0 until COVER_SIZE) {
                bitmap.setPixel(x, y, Color.rgb(x, y, x + y))
            }
        }
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)
            stream.toByteArray()
        }
    }

    private companion object {
        const val FIXED_INSTANT = "2026-07-13T12:00:00Z"
        const val IMAGE_NAME = "nyx-0001.png"
        const val SEEDED_ID = "id-0"
        const val ARCHIVE = "Archive"
        const val RESTORE = "Restore"
        const val DELETE = "Delete"
        const val TOGGLE_ARCHIVED = "Toggle archived"
        const val ARCHIVED_SECTION = "Archived"
        const val COVER_SIZE = 128
        const val PNG_QUALITY = 100
        const val TIMEOUT_MILLIS = 5_000L
    }
}
