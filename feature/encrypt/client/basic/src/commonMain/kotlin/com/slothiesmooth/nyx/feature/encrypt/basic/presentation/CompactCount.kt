package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import kotlin.math.roundToInt

private const val THOUSAND = 1_000
private const val MILLION = 1_000_000
private const val BILLION = 1_000_000_000
private const val COMPACT_DECIMAL_CUTOFF = 10
private const val DECIMAL_SCALE = 10

/**
 * Formats a non-negative count as a compact, human-readable string: the exact number below 1000, then
 * k / M / B with one decimal below ten (1.8k) and none from ten up (12k, 1M). Keeps the capacity hint
 * short so a large image reports "about 1.8M characters fit" instead of a wall of digits.
 */
fun formatCompactCount(count: Int): String {
    if (count < THOUSAND) return count.toString()
    val (value, suffix) = when {
        count < MILLION -> count.toDouble() / THOUSAND to "k"
        count < BILLION -> count.toDouble() / MILLION to "M"
        else -> count.toDouble() / BILLION to "B"
    }
    val text = if (value < COMPACT_DECIMAL_CUTOFF) withOneDecimal(value) else value.roundToInt().toString()
    return text + suffix
}

private fun withOneDecimal(value: Double): String {
    val scaled = (value * DECIMAL_SCALE).roundToInt()
    val whole = scaled / DECIMAL_SCALE
    val fraction = scaled % DECIMAL_SCALE
    return if (fraction == 0) whole.toString() else "$whole.$fraction"
}
