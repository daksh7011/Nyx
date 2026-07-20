package com.slothiesmooth.nyx.shared.data.id

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IdGeneratorTest {

    private val uuidV4Regex =
        Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

    @Test
    fun `generated ids are lowercase uuid version 4`() {
        val generator = Uuid4IdGenerator()
        repeat(50) {
            val id = generator.newId()
            assertTrue(uuidV4Regex.matches(id), "not a v4 uuid: $id")
        }
    }

    @Test
    fun `generated ids are unique across a large batch`() {
        val generator = Uuid4IdGenerator()
        val ids = HashSet<String>()
        repeat(1000) { ids.add(generator.newId()) }
        assertEquals(1000, ids.size)
    }

    @Test
    fun `stego image id wraps its raw string value`() {
        assertEquals("abc-123", StegoImageId("abc-123").value)
    }
}
