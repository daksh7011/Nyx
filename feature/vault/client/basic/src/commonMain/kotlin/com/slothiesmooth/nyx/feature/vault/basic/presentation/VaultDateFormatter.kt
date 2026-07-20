package com.slothiesmooth.nyx.feature.vault.basic.presentation

import com.slothiesmooth.nyx.feature.vault.basic.resources.Res
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_date_format
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_1
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_10
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_11
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_12
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_2
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_3
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_4
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_5
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_6
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_7
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_8
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_month_9
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val monthAbbreviations = persistentListOf(
    Res.string.vault_month_1,
    Res.string.vault_month_2,
    Res.string.vault_month_3,
    Res.string.vault_month_4,
    Res.string.vault_month_5,
    Res.string.vault_month_6,
    Res.string.vault_month_7,
    Res.string.vault_month_8,
    Res.string.vault_month_9,
    Res.string.vault_month_10,
    Res.string.vault_month_11,
    Res.string.vault_month_12,
)

/**
 * Renders a stego image's creation [Instant] into a localized [UiText] such as `Jul 13, 2026`,
 * projected into [zone]. The month abbreviation and the date pattern are string resources, resolved
 * when the composable draws the label, so it is translatable. Pure and side-effect-free for testing.
 */
fun formatVaultDate(instant: Instant, zone: TimeZone): UiText {
    val dateTime = instant.toLocalDateTime(zone)
    val month = monthAbbreviations[dateTime.month.ordinal]
    return UiText.res(Res.string.vault_date_format, month, dateTime.day, dateTime.year)
}
