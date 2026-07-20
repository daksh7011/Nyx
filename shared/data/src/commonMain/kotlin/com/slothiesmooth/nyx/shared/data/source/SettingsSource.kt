package com.slothiesmooth.nyx.shared.data.source

import kotlinx.coroutines.flow.Flow

/**
 * Key/value settings persistence (DataStore on android/ios/desktop; localStorage on web).
 */
interface SettingsSource {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
    fun observeString(key: String): Flow<String?>
}
