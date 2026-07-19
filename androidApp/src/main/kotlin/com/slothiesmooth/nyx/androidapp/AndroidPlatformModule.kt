package com.slothiesmooth.nyx.androidapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import com.slothiesmooth.nyx.shared.data.source.AppInfo
import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.source.SettingsSource
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.filesDir
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

private const val DATABASE_NAME = "nyx.db"
private const val PREFERENCES_FILE = "nyx.preferences_pb"
private const val VAULT_DIR = "stego_vault"

/** The Android infrastructure the app graph layers under: driver, settings, vault store, share, camera. */
fun androidPlatformModule(context: Context): Module = module {
    single<SqlDriver> { AndroidSqliteDriver(NyxDb.Schema.synchronous(), context, DATABASE_NAME) }
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath {
            context.filesDir.resolve(PREFERENCES_FILE).absolutePath.toPath()
        }
    }
    single<SettingsSource> { DataStoreSettingsSource(get()) }
    single<VaultFileStore> { FileKitVaultFileStore(PlatformFile(FileKit.filesDir, VAULT_DIR)) }
    single<ShareSource> { AndroidShareSource(context) }
    single<CameraSource> { FileKitCameraSource() }
    single { PlatformCapabilities(camera = true, persistentVault = true) }
    single<AppInfo> { AndroidAppInfo(context) }
}
