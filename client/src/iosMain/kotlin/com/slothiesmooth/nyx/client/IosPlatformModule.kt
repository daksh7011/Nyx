package com.slothiesmooth.nyx.client

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.slothiesmooth.nyx.client.data.source.database.sqldelight.SqlDelightSource
import com.slothiesmooth.nyx.client.data.source.database.vault.VaultSqlSource
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import com.slothiesmooth.nyx.shared.data.source.AppInfo
import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.ClipboardWriter
import com.slothiesmooth.nyx.shared.data.source.ImagePicker
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.source.SettingsSource
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.filesDir
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val DATABASE_FILE_NAME = "nyx.db"
private const val SETTINGS_FILE_NAME = "nyx.preferences_pb"
private const val VAULT_DIR_NAME = "stego_vault"

/**
 * The iOS infrastructure the app graph layers under: a NativeSqliteDriver-backed vault database, a
 * DataStore-preferences settings store under the Documents directory, and FileKit-backed file/
 * camera/pick sources. Persistent vault, camera available.
 */
fun iosPlatformModule(): Module = module {
    single<SqlDriver> { NativeSqliteDriver(NyxDb.Schema.synchronous(), DATABASE_FILE_NAME) }
    single { SqlDelightSource(get(), get()) }
    single<VaultSource> { VaultSqlSource(get()) }
    single<VaultFileStore> { FileKitVaultFileStore(PlatformFile(FileKit.filesDir, VAULT_DIR_NAME)) }
    single<SettingsSource> { DataStoreSettingsSource(iosDataStore()) }
    single<ShareSource> { IosShareSource() }
    single<CameraSource> { FileKitCameraSource() }
    single<ImagePicker> { FileKitImagePicker() }
    single<ClipboardWriter> { IosClipboardWriter() }
    single<AppInfo> { IosAppInfo() }
    single { PlatformCapabilities(camera = true, persistentVault = true) }
}

@OptIn(ExperimentalForeignApi::class)
private fun iosDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    produceFile = {
        val documentsUrl = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        "${requireNotNull(documentsUrl?.path)}/$SETTINGS_FILE_NAME".toPath()
    },
)
