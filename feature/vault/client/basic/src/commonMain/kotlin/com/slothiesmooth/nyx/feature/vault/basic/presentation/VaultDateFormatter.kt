package com.slothiesmooth.nyx.feature.vault.basic.presentation

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val MONTH_ABBREVIATION_LENGTH = 3

/**
 * Renders a stego image's creation [Instant] into a short, human-readable label such as
 * `Jul 13, 2026`, projected into [zone]. Pure and side-effect-free so the render-ready mapping stays
 * out of the composables and is unit-testable.
 */
fun formatVaultDate(instant: Instant, zone: TimeZone): String {
    val dateTime = instant.toLocalDateTime(zone)
    val month = dateTime.month.name.lowercase()
        .replaceFirstChar { it.uppercase() }
        .take(MONTH_ABBREVIATION_LENGTH)
    return "$month ${dateTime.day}, ${dateTime.year}"
}
