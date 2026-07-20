package com.slothiesmooth.nyx.shared.data.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class AppResultTest {

    @Test
    fun `map transforms the ok value`() {
        val result: AppResult<Int> = AppResult.Ok(21)
        assertEquals(AppResult.Ok(42), result.map { it * 2 })
    }

    @Test
    fun `map passes an error through unchanged`() {
        val error = AppResult.Err(AppError.NotFound)
        val mapped = error.map { "never" }
        assertSame(error, mapped)
    }

    @Test
    fun `getOrNull returns the value for ok`() {
        assertEquals("secret", AppResult.Ok("secret").getOrNull())
    }

    @Test
    fun `getOrNull returns null for err`() {
        assertNull(AppResult.Err(AppError.Permission).getOrNull())
    }

    @Test
    fun `errors carry their payloads`() {
        assertEquals("bad name", (AppError.Validation("bad name") as AppError.Validation).message)
        val cause = IllegalStateException("disk full")
        val storage = AppError.Storage("write failed", cause)
        assertEquals("write failed", storage.message)
        assertSame(cause, storage.cause)
    }
}
