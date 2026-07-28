package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.source.SettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [SettingsSource] whose [observeString] emits live changes. Test doubles only. */
class FakeSettingsSource(initial: Map<String, String> = emptyMap()) : SettingsSource {

    private val store = MutableStateFlow(initial.toMap())

    override suspend fun getString(key: String): String? = store.value[key]

    override suspend fun putString(key: String, value: String) {
        store.update { current -> current + (key to value) }
    }

    override fun observeString(key: String): Flow<String?> =
        store.map { current -> current[key] }.distinctUntilChanged()
}
