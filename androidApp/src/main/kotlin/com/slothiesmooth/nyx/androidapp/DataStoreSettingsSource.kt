package com.slothiesmooth.nyx.androidapp

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.slothiesmooth.nyx.shared.data.source.SettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** DataStore-preferences-backed [SettingsSource]. Reads/writes and observes String keys. */
class DataStoreSettingsSource(private val dataStore: DataStore<Preferences>) : SettingsSource {

    override suspend fun getString(key: String): String? =
        dataStore.data.map { prefs -> prefs[stringPreferencesKey(key)] }.first()

    override suspend fun putString(key: String, value: String) {
        dataStore.edit { prefs -> prefs[stringPreferencesKey(key)] = value }
    }

    override fun observeString(key: String): Flow<String?> =
        dataStore.data.map { prefs -> prefs[stringPreferencesKey(key)] }.distinctUntilChanged()
}
