package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.source.SettingsSource
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * [SettingsSource] backed by browser localStorage. localStorage has no same-tab change event, so a
 * MutableStateFlow of the keys written this session drives observeString; the initial emission and
 * fallback read come straight from localStorage.
 */
class LocalStorageSettingsSource : SettingsSource {

    private val writes = MutableStateFlow<Map<String, String>>(emptyMap())

    override suspend fun getString(key: String): String? = localStorage.getItem(storageKey(key))

    override suspend fun putString(key: String, value: String) {
        localStorage.setItem(storageKey(key), value)
        writes.update { snapshot -> snapshot + (key to value) }
    }

    override fun observeString(key: String): Flow<String?> =
        writes
            .map { snapshot -> snapshot[key] ?: localStorage.getItem(storageKey(key)) }
            .distinctUntilChanged()

    private fun storageKey(key: String): String = "$STORAGE_PREFIX$key"

    private companion object {
        const val STORAGE_PREFIX = "nyx."
    }
}
