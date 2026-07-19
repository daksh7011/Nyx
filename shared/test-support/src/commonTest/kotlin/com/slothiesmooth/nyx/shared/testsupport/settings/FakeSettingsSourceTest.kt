package com.slothiesmooth.nyx.shared.testsupport.settings

import com.slothiesmooth.nyx.shared.testsupport.FakeSettingsSource
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeSettingsSourceTest {

    @Test
    fun getString_returnsNullWhenKeyNotSet() = runTest {
        val source = FakeSettingsSource()
        val result = source.getString("nonexistent")
        assertNull(result)
    }

    @Test
    fun getString_returnsInitialValue() = runTest {
        val source = FakeSettingsSource(mapOf("key1" to "value1"))
        val result = source.getString("key1")
        assertEquals("value1", result)
    }

    @Test
    fun putString_storesValue() = runTest {
        val source = FakeSettingsSource()
        source.putString("key1", "value1")
        val result = source.getString("key1")
        assertEquals("value1", result)
    }

    @Test
    fun putString_overwritesExistingValue() = runTest {
        val source = FakeSettingsSource(mapOf("key1" to "oldValue"))
        source.putString("key1", "newValue")
        val result = source.getString("key1")
        assertEquals("newValue", result)
    }

    @Test
    fun observeString_emitsNullWhenKeyNotSet() = runTest {
        val source = FakeSettingsSource()
        val result = source.observeString("nonexistent").firstOrNull()
        assertNull(result)
    }

    @Test
    fun observeString_emitsInitialValue() = runTest {
        val source = FakeSettingsSource(mapOf("key1" to "value1"))
        val result = source.observeString("key1").firstOrNull()
        assertEquals("value1", result)
    }

    @Test
    fun observeString_emitsLiveChanges() = runTest {
        val source = FakeSettingsSource()

        // Put first value
        source.putString("key1", "value1")
        val firstValue = source.observeString("key1").firstOrNull()
        assertEquals("value1", firstValue)

        // Put second value — should emit the change
        source.putString("key1", "value2")
        val secondValue = source.observeString("key1").firstOrNull()
        assertEquals("value2", secondValue)
    }

    @Test
    fun observeString_emitsBothInitialAndChanges() = runTest {
        val source = FakeSettingsSource(mapOf("key1" to "initial"))

        // Verify initial value is observable
        val initialValue = source.observeString("key1").firstOrNull()
        assertEquals("initial", initialValue)

        // Change the value
        source.putString("key1", "changed")

        // Verify the new value is observable
        val newValue = source.observeString("key1").firstOrNull()
        assertEquals("changed", newValue)
    }

    @Test
    fun observeString_distinctUntilChanged() = runTest {
        val source = FakeSettingsSource(mapOf("key1" to "value1"))

        // Observe initial value
        val initialValue = source.observeString("key1").firstOrNull()
        assertEquals("value1", initialValue)

        // Put same value — should not change observable state
        source.putString("key1", "value1")
        val unchangedValue = source.observeString("key1").firstOrNull()
        assertEquals("value1", unchangedValue)

        // Put different value — should update observable state
        source.putString("key1", "value2")
        val changedValue = source.observeString("key1").firstOrNull()
        assertEquals("value2", changedValue)
    }

    @Test
    fun multipleKeys_storeIndependently() = runTest {
        val source = FakeSettingsSource()
        source.putString("key1", "value1")
        source.putString("key2", "value2")

        assertEquals("value1", source.getString("key1"))
        assertEquals("value2", source.getString("key2"))
    }
}
