package com.slothiesmooth.nyx.feature.vault.basic.presentation.list

import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.feature.vault.basic.domain.model.VaultImage
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.GetImageBytesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.presentation.formatVaultDate
import com.slothiesmooth.nyx.shared.data.result.AppResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone

/**
 * Pure render-ready mapper from domain [images] to [VaultImageUi] rows. Fetching bytes and formatting
 * dates happen here (off the composables); [decode] is injected so unit tests exercise the mapping
 * without touching the platform image codec.
 */
suspend fun buildVaultRows(
    images: ImmutableList<VaultImage>,
    getImageBytes: GetImageBytesUseCase,
    zone: TimeZone,
    decode: (ByteArray) -> ImageBitmap?,
): ImmutableList<VaultImageUi> = images.map { image ->
    val thumbnail = when (val bytes = getImageBytes(image.id)) {
        is AppResult.Ok -> decode(bytes.value)
        is AppResult.Err -> null
    }
    VaultImageUi(
        id = image.id,
        name = image.name,
        createdLabel = formatVaultDate(image.createdAt, zone),
        thumbnail = thumbnail,
    )
}.toImmutableList()
