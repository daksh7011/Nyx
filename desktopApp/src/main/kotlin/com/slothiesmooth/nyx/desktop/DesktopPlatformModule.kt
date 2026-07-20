package com.slothiesmooth.nyx.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
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
import io.github.vinceglb.filekit.path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

private const val DATABASE_FILE_NAME = "nyx.db"
private const val SETTINGS_FILE_NAME = "nyx.preferences_pb"
private const val VAULT_DIR_NAME = "stego_vault"

/**
 * The desktop (JVM) infrastructure the app graph layers under: a file-backed JDBC vault database,
 * a DataStore-preferences settings store, and FileKit-backed file/share/pick sources. The vault is
 * persistent (`persistentVault = true`); there is no camera on desktop.
 */
fun desktopPlatformModule(): Module = module {
    single<SqlDriver> { desktopSqlDriver() }
    single { SqlDelightSource(get(), get()) }
    single<VaultSource> { VaultSqlSource(get()) }
    single<VaultFileStore> { FileKitVaultFileStore(desktopVaultRoot()) }
    single<SettingsSource> { DataStoreSettingsSource(desktopDataStore()) }
    single<ShareSource> { DesktopShareSource() }
    single<CameraSource> { NoCameraSource }
    single<ImagePicker> { DesktopImagePicker() }
    single<ClipboardWriter> { DesktopClipboardWriter() }
    single<AppInfo> { DesktopAppInfo() }
    single { PlatformCapabilities(camera = false, persistentVault = true) }
}

private fun desktopSqlDriver(): SqlDriver = JdbcSqliteDriver(
    url = "jdbc:sqlite:${FileKit.filesDir.path}/$DATABASE_FILE_NAME",
    schema = NyxDb.Schema.synchronous(),
)

private fun desktopVaultRoot(): PlatformFile = PlatformFile(FileKit.filesDir, VAULT_DIR_NAME)

private fun desktopDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    produceFile = { "${FileKit.filesDir.path}/$SETTINGS_FILE_NAME".toPath() },
)
