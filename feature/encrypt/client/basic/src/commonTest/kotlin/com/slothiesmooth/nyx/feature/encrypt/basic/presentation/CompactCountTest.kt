package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class CompactCountTest {

    @Test
    fun `below one thousand shows the exact count`() {
        assertEquals("0", formatCompactCount(0))
        assertEquals("512", formatCompactCount(512))
        assertEquals("999", formatCompactCount(999))
    }

    @Test
    fun `thousands use k with one decimal below ten and none above`() {
        assertEquals("1k", formatCompactCount(1_000))
        assertEquals("1.8k", formatCompactCount(1_820))
        assertEquals("12k", formatCompactCount(12_000))
        assertEquals("100k", formatCompactCount(100_000))
    }

    @Test
    fun `millions use M`() {
        assertEquals("1M", formatCompactCount(1_000_000))
        assertEquals("1.5M", formatCompactCount(1_500_000))
        assertEquals("10M", formatCompactCount(10_000_000))
    }

    @Test
    fun `billions use B`() {
        assertEquals("1B", formatCompactCount(1_000_000_000))
    }
}
