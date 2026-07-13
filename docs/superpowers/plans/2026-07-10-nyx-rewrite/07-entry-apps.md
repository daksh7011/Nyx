# Entry Apps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the three non-Android entry points — `:desktopApp` (runs on Linux via `./gradlew :desktopApp:run`), `:webApp` (serves via `./gradlew :webApp:wasmJsBrowserDevelopmentRun` with an export-only in-memory vault), and the committed iOS scaffold (framework link deferred to a macOS lane) — plus every platform Koin module (desktop/web/iOS) and the CI jobs that guard them.

**Architecture:** Each entry app is a thin, single-target module (`:desktopApp` = jvm-only Compose Desktop; `:webApp` = wasmJs-only Compose/Wasm) that (1) initializes its platform prerequisites, (2) calls `initKoin(<platformModule>())` from `:client`, and (3) hosts the shared `App()` composable. The platform Koin module is the seam: it provides `VaultSource`, `VaultFileStore`, `SettingsSource`, `ShareSource`, `CameraSource`, and `PlatformCapabilities` — the six per-platform bindings that `appModule` (in `:client` commonMain) deliberately does NOT register, because wasm has no SqlDelight driver. iOS lives inside `:client` iosMain (`MainViewController`, `iosPlatformModule`) and is exercised only on macOS.

**Tech Stack:** Kotlin 2.3.21, Compose Multiplatform 1.10.3 (desktop `application`/`Window`, wasm `ComposeViewport`, iOS `ComposeUIViewController`), Koin 4.2.1, SqlDelight 2.3.2 (`JdbcSqliteDriver` desktop / `NativeSqliteDriver` iOS / no driver on wasm), AndroidX DataStore 1.2.1 (jvm/ios), kotlinx-browser 0.5.0 (wasm localStorage), FileKit 0.13.0 (`filesDir`/`openFileSaver`/`download`/`openCameraPicker`/`init`), Kermit 2.1.0, GitHub Actions.

## Global Constraints

Copied verbatim from `00-INDEX.md` (the whole index applies implicitly; these are the load-bearing lines for this phase):

- Kotlin 2.3.21, AGP 9.2.0, Gradle 9.4.1, Compose Multiplatform 1.10.3, JVM target 21,
  compileSdk 36, targetSdk 36, minSdk 24.
- KMP targets on every KMP module: `androidTarget` (via `com.android.kotlin.multiplatform.library`,
  configured as `kotlin { android {} }`), `iosX64`, `iosArm64`, `iosSimulatorArm64`, `jvm`,
  `wasmJs`. `applyDefaultHierarchyTemplate()`. iOS compiles only on macOS — never gate Linux
  progress on iOS; `kotlin.native.ignoreDisabledTargets=true`.
- Namespace/package: `com.slothiesmooth.nyx.<area>` (full reverse-domain everywhere; Android
  `namespace` per module must be unique).
- US English in all identifiers/comments/docs/commits. No `@Suppress`-style gate-passers —
  the single documented exception: `NxColors.kt` may suppress MagicNumber (the one raw-ARGB file).
- kotlinx ImmutableCollections for ALL collections in state/domain surfaces (ImmutableList/Set/Map).
- Repository writes return `AppResult`, reads return `Flow`.
- Tests: kotlin.test + kotlinx-coroutines-test + hand-written fakes only. No mockk/kotest/turbine.
- `suspend` end-to-end for crypto (WebCrypto provider is suspend-only; `*Blocking` throws on wasm).
- No `println`; logging via Kermit.
- Commit after every green test cycle (conventional commits).

Phase-specific constraints (load-bearing for entry apps — read before writing any code):

- **Platform-module boundary (the seam this whole phase turns on):** the six bindings
  `VaultSource`, `VaultFileStore`, `SettingsSource`, `ShareSource`, `CameraSource`,
  `PlatformCapabilities` are ALWAYS provided by the platform Koin module passed to
  `initKoin(...)`. `appModule` (in `:client` commonMain, plan 05) NEVER registers them, because
  it is compiled for wasmJs and wasm has no SqlDelight driver. Engines (`NyxCrypto`,
  `Steganography`), `ImageCodec` (expect/actual in `:client`), `Clock`/`IdGenerator`/
  `DomainEventBus`, and every feature stay in `appModule`. Task 1 verifies this invariant and
  repairs `appModule` + `AndroidPlatformModule` if plan 05 wired it any other way.
- **No SqlDelight on wasm (v1 capability cut, spec §6):** the `:client` `VaultSqlSource` /
  SqlDelight `NyxDb` / any `SqlDriver` are referenced ONLY from the non-wasm platform modules
  (desktop jvm, iOS native, android). `WebPlatformModule` provides `InMemoryVaultSource` +
  `InMemoryVaultFileStore` — the wasm vault is session-only; export via `FileKit.download`
  still works. `PlatformCapabilities(persistentVault = false)` drives the UI.
- **FileKit source-set traps (00-INDEX FileKit facts):** `FileKit.filesDir` / `openFileSaver` /
  `init` = nonWeb (jvm+ios+android); `FileKit.download(bytes, fileName)` = web only;
  `FileKit.openCameraPicker(...)` = mobile only (android+ios). None are visible from a 6-target
  commonMain — they are touched ONLY from platform source sets / platform Koin modules. JVM
  additionally requires `FileKit.init(appId = "Nyx")` at `main()` start before any `filesDir`
  access.
- **Small-glue duplication decision (stated, pawdex pattern):** `FileKitVaultFileStore`,
  `DataStoreSettingsSource`, `FileKitCameraSource`, and the trivial `NoCameraSource` are small
  platform-glue classes. Plan 05 authored them for `androidApp`. Because `:desktopApp`,
  `:webApp`, and the iOS platform module (inside `:client` iosMain) are three separate
  compilation contexts and a Kotlin source set cannot span Gradle-module boundaries, we
  DUPLICATE these ~20-line classes per platform module rather than build a custom `nonWebMain`
  intermediate published surface in `:client`. Plan 05's versions are authoritative; the copies
  here must stay byte-for-byte identical in behavior (only the package header and the path/root
  helper differ per platform).
- **iOS is `[macOS-verify-later]`:** every iOS step is written so Linux never blocks on it.
  `kotlin.native.ignoreDisabledTargets=true` skips iOS compilation on Linux; the exact macOS
  commands are spelled out in Task 3 for the mac lane / CI to run.
- **PBKDF2 latency escalation (spec §11):** the web PBKDF2 measurement step never silently
  lowers iterations below the 600 000 OWASP floor set in `:crypto`. If in-browser encrypt is
  slow (≥ 8 s on the dev machine), flag it to the user — do not "fix" it by weakening crypto.

## Context you need before starting (facts baked in — the pawdex clone and research-*.txt referenced by the orchestrator may be absent; everything needed is inlined here)

- **Module paths & packages** (00-INDEX namespace table): `:desktopApp` →
  `com.slothiesmooth.nyx.desktop` (pure jvm, no Android namespace); `:webApp` →
  `com.slothiesmooth.nyx.web` (pure wasmJs); iOS entry lives in `:client` iosMain, package
  `com.slothiesmooth.nyx.client`; the Xcode scaffold at `client/iosApp/`.
- **`:client` surface consumed here** (00-INDEX `:client` contract): `fun initKoin(platformModule: Module): KoinApplication`,
  `fun appModule(platformModule: Module): Module`, `@Composable fun App()`, the generated
  SqlDelight database `NyxDb` in package `com.slothiesmooth.nyx.client.data.sqldelight`, the
  `SqlDelightSource(driver, scope)` wrapper in
  `com.slothiesmooth.nyx.client.data.source.database.sqldelight`, and
  `VaultSqlSource(source: SqlDelightSource) : VaultSource` in
  `com.slothiesmooth.nyx.client.data.source.database.vault` (both plan 03). These are `public` —
  plan 05's `androidApp` already constructs the vault source across the module boundary, so
  desktop can too.
- **`:shared:data` source interfaces consumed here** (00-INDEX `:shared:data` contract, package
  `com.slothiesmooth.nyx.shared.data`): `VaultSource`, `VaultFileStore`, `SettingsSource`,
  `ShareSource`, `CameraSource`, `PickedImage`, `PlatformCapabilities`, `StegoImageRecord`,
  `AppResult`/`AppError`. Their exact signatures are reproduced inline in each task where used.
- **CMP 1.10.3 entries (00-INDEX research-verified — do not re-derive):**
  - Desktop: `fun main() = application { Window(onCloseRequest = ::exitApplication, title = "Nyx") { App() } }`.
  - wasm: `@OptIn(ExperimentalComposeUiApi::class) fun main() { ComposeViewport { App() } }` —
    `CanvasBasedWindow` is `@Deprecated(level = ERROR)`. `index.html`: empty body plus
    `<script src="composeApp.js">` matching `wasmJs { outputModuleName = "composeApp" }`;
    `styles.css` MUST set `html, body { width:100%; height:100%; margin:0; overflow:hidden }`
    (ComposeViewport injects no CSS).
  - iOS: `fun MainViewController() = ComposeUIViewController { App() }`.
- **Drivers (00-INDEX):** `JdbcSqliteDriver` (desktop jvm, `app.cash.sqldelight:sqlite-driver`),
  `NativeSqliteDriver` (iOS, `app.cash.sqldelight:native-driver`), NONE on wasm.
  `generateAsync = true` globally → the generated `NyxDb.Schema` is async; synchronous drivers
  take `NyxDb.Schema.synchronous()`. **The exact import of `synchronous()` is whatever plan 05's
  `AndroidPlatformModule` uses to build `AndroidSqliteDriver` — copy it; it is the same adapter
  on all three sync drivers.**
- **DataStore (00-INDEX):** `datastore-preferences-core` publishes android/ios/jvm only (the wasm
  artifact is a `TODO()` stub that crashes at runtime) → jvm and iOS platform modules use
  `DataStoreSettingsSource`; wasm uses `LocalStorageSettingsSource`.
- **Version-catalog aliases used below** (pawdex-style naming; plan 01 owns
  `gradle/libs.versions.toml` — if plan 01 chose different alias names for the same Maven
  coordinates, use plan 01's names throughout): `libs.plugins.kotlin.multiplatform`,
  `libs.plugins.compose.compiler`, `libs.plugins.compose.multiplatform`, `libs.koin.core`,
  `libs.sqldelight.sqlite.driver` (`app.cash.sqldelight:sqlite-driver`),
  `libs.sqldelight.native.driver` (`app.cash.sqldelight:native-driver`),
  `libs.androidx.datastore.preferences.core` (`androidx.datastore:datastore-preferences-core`),
  `libs.filekit.core` (`io.github.vinceglb:filekit-core`),
  `libs.filekit.dialogs` (`io.github.vinceglb:filekit-dialogs`),
  `libs.kotlinx.browser` (`org.jetbrains.kotlinx:kotlinx-browser`),
  `libs.kotlinx.collections.immutable`, `libs.kotlinx.coroutines.core`,
  `libs.kotlinx.coroutines.swing` (`org.jetbrains.kotlinx:kotlinx-coroutines-swing`),
  `libs.kermit` (`co.touchlab:kermit`), `libs.kotlin.test`, `libs.kotlinx.coroutines.test`.

---

### Task 1: `:desktopApp` — Compose Desktop entry, DesktopPlatformModule, packaging

**Files:**
- Modify (verify/repair): `settings.gradle.kts` (`include(":desktopApp")`),
  `gradle/libs.versions.toml` (aliases above),
  `client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/app/AppConfig.kt` (the
  `appModule` file — repair only if it registers vault sources; see Step 2),
  `androidApp/.../AndroidPlatformModule.kt` (repair only if plan 05 left vault sources in
  `appModule`; see Step 2)
- Create/Overwrite: `desktopApp/build.gradle.kts`
- Create: `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/Main.kt`
- Create: `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/DesktopPlatformModule.kt`
- Create: `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/FileKitVaultFileStore.kt`
- Create: `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/DataStoreSettingsSource.kt`
- Create: `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/DesktopShareSource.kt`
- Create: `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/NoCameraSource.kt`
- Test (config-task verification): `./gradlew :desktopApp:compileKotlinJvm`,
  `./gradlew :desktopApp:run`, `./gradlew :desktopApp:createDistributable`

**Interfaces:**
- Consumes (from `:client`, plans 03/05): `fun initKoin(platformModule: Module): KoinApplication`,
  `@Composable fun App()`, `class NyxDb` (`com.slothiesmooth.nyx.client.data.sqldelight`),
  `class SqlDelightSource(driver: SqlDriver, scope: CoroutineScope)`
  (`com.slothiesmooth.nyx.client.data.source.database.sqldelight`),
  `class VaultSqlSource(source: SqlDelightSource) : VaultSource`
  (`com.slothiesmooth.nyx.client.data.source.database.vault`). From `:shared:data` (plan 03):
  `VaultSource`, `VaultFileStore`, `SettingsSource`, `ShareSource`, `CameraSource`, `PickedImage`,
  `PlatformCapabilities`, `AppResult`, `AppError`.
- Produces: `fun desktopPlatformModule(): org.koin.core.module.Module`, `fun main()` (compiles
  to `com.slothiesmooth.nyx.desktop.MainKt`), the gradle tasks `:desktopApp:run` and
  `:desktopApp:createDistributable` / `:desktopApp:packageDistributionForCurrentOS` (consumed by
  plan 08 README/CI). Establishes the platform-module boundary invariant that Tasks 2 and 3
  rely on.

**Steps:**

- [ ] **Step 1: Reconcile the version catalog.** Open `gradle/libs.versions.toml`. Plan 01 owns
  this file; its alias names win. Verify these coordinates are present (add any that are missing,
  reusing plan 01's alias names if they differ):

```toml
[libraries]
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
androidx-datastore-preferences-core = { module = "androidx.datastore:datastore-preferences-core", version.ref = "androidx-datastore" }
filekit-core = { module = "io.github.vinceglb:filekit-core", version.ref = "filekit" }
filekit-dialogs = { module = "io.github.vinceglb:filekit-dialogs", version.ref = "filekit" }
kotlinx-browser = { module = "org.jetbrains.kotlinx:kotlinx-browser", version.ref = "kotlinx-browser" }
kotlinx-coroutines-swing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinx-coroutines" }
kermit = { module = "co.touchlab:kermit", version.ref = "kermit" }
```

  Expected: after editing, `./gradlew help` ends `BUILD SUCCESSFUL` (catalog parses).

- [ ] **Step 2: Verify — and repair if needed — the platform-module boundary invariant.**
  This is the prerequisite for all three entry apps: `appModule` must NOT register `VaultSource`
  or `VaultFileStore` (they are platform-provided); if plan 05 registered them there, wasm
  cannot compile/run. Run:

```bash
grep -rn "VaultSource\|VaultFileStore\|VaultSqlSource\|SqlDelightSource\|SqlDriver\|NyxDb" \
  client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/app/
```

  Decision rule:
  - **Invariant already holds** (no `single<VaultSource>` / `single<VaultFileStore>` /
    `SqlDelightSource(...)` / `VaultSqlSource(...)` / `SqlDriver` construction inside `appModule`;
    the only hits are the expect/actual `ImageCodec`, the shared `single<CoroutineScope>`, or
    none) → nothing to repair; record "invariant holds" and go to Step 3.
  - **Invariant violated** (`appModule` builds `SqlDelightSource` / `VaultSqlSource`, a
    `SqlDriver`, or registers `single<VaultSource>` / `single<VaultFileStore>`) → move that wiring
    out of `appModule` into the platform modules. Concretely: (a) delete the vault `single { ... }`
    lines from `AppConfig.kt`'s `appModule` — the `NyxDb` / `SqlDelightSource` / `VaultSqlSource`
    and any `SqlDriver` registrations — but KEEP the `single<CoroutineScope>` there: it is the
    shared app scope `SqlDelightSource` depends on, and every platform (including wasm) needs it;
    (b) add the platform vault wiring into `androidApp`'s `AndroidPlatformModule` so Android keeps
    working — mirror exactly the desktop wiring written in Step 6 below
    (`single { SqlDelightSource(get(), get()) }` +
    `single<VaultSource> { VaultSqlSource(get()) }`), substituting `AndroidSqliteDriver(...)` for
    the JDBC driver; `AndroidPlatformModule` already registers the `SqlDriver` and
    `single<VaultFileStore>`, so only the two vault-source `single`s are added. Re-run
    `./gradlew :androidApp:assembleDebug` and confirm `BUILD SUCCESSFUL` before moving on.

  After this step `VaultSource` is registered exactly once per platform — SqlDelight-backed in
  each non-wasm platform module (android/desktop/ios), `InMemoryVaultSource` in `WebPlatformModule`
  (Task 2) — and NEVER in the common `appModule`.

  Expected: either a recorded "invariant holds", or a repaired `appModule` + `AndroidPlatformModule`
  with Android still assembling green.

- [ ] **Step 3: Verify the module is registered, then compile the empty skeleton (failing
  check).** Plan 01 created the `:desktopApp` skeleton. Run:

```bash
grep -n '":desktopApp"' settings.gradle.kts || echo "MISSING_INCLUDE"
./gradlew :desktopApp:compileKotlinJvm
```

  If `MISSING_INCLUDE`, add `include(":desktopApp")` to `settings.gradle.kts`. The compile is the
  "failing test": with only the plan-01 skeleton (no `Main.kt`/`DesktopPlatformModule.kt`) it
  either builds an empty module or fails on the missing entry — either way it does NOT yet
  produce a runnable app. Record the result; the next steps drive it to a real green.

- [ ] **Step 4: Write the desktop build file.** Overwrite `desktopApp/build.gradle.kts` with
  exactly (if plan 01 provides a KMP-compose convention plugin producing an equivalent jvm+compose
  configuration, applying it instead is fine — the effective config below is what matters):

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":client"))
            implementation(compose.desktop.currentOs)
            implementation(libs.koin.core)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            // Provides the JVM Main coroutine dispatcher (Swing EDT); BaseViewModel uses
            // Dispatchers.Main.immediate and throws "Module with Main dispatcher had failed to
            // initialize" without it.
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.slothiesmooth.nyx.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "Nyx"
            packageVersion = "1.0.0"
        }
    }
}
```

- [ ] **Step 5: Write the two duplicated glue classes (copied from plan 05, package rehomed).**
  These are byte-for-byte plan 05 behavior; if plan 05's `AndroidPlatformModule` uses different
  FileKit / DataStore method names, plan 05 is authoritative — match it.

  Create `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/FileKitVaultFileStore.kt`:

```kotlin
package com.slothiesmooth.nyx.desktop

import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.VaultFileStore
import io.github.vinceglb.filekit.PlatformFile

/**
 * Stores stego PNG bytes as `<vaultDir>/<id>.png` via FileKit's platform-agnostic file API.
 * Identical to plan 05's androidApp FileKitVaultFileStore; the only difference is the package.
 */
class FileKitVaultFileStore(private val vaultDir: PlatformFile) : VaultFileStore {

    override suspend fun write(id: String, bytes: ByteArray): AppResult<Unit> = guard {
        if (!vaultDir.exists()) vaultDir.createDirectories()
        pngFile(id).write(bytes)
    }

    override suspend fun read(id: String): AppResult<ByteArray> {
        val file = pngFile(id)
        if (!file.exists()) return AppResult.Err(AppError.NotFound)
        return runCatching { file.readBytes() }.fold(
            onSuccess = { bytes -> AppResult.Ok(bytes) },
            onFailure = { error -> AppResult.Err(AppError.Storage("vault read failed", error)) },
        )
    }

    override suspend fun delete(id: String): AppResult<Unit> = guard {
        val file = pngFile(id)
        if (file.exists()) file.delete()
    }

    override suspend fun deleteAll(): AppResult<Unit> = guard {
        if (vaultDir.exists()) vaultDir.list().forEach { child -> child.delete() }
    }

    private fun pngFile(id: String): PlatformFile = PlatformFile(vaultDir, "$id.png")

    private inline fun guard(block: () -> Unit): AppResult<Unit> = runCatching { block() }.fold(
        onSuccess = { AppResult.Ok(Unit) },
        onFailure = { error -> AppResult.Err(AppError.Storage("vault io failed", error)) },
    )
}
```

  Create `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/DataStoreSettingsSource.kt`:

```kotlin
package com.slothiesmooth.nyx.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.slothiesmooth.nyx.shared.data.SettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * SettingsSource backed by AndroidX DataStore preferences (jvm artifact).
 * Identical to plan 05's androidApp DataStoreSettingsSource; only the package differs.
 */
class DataStoreSettingsSource(
    private val dataStore: DataStore<Preferences>,
) : SettingsSource {

    override suspend fun getString(key: String): String? =
        dataStore.data.map { prefs -> prefs[stringPreferencesKey(key)] }.first()

    override suspend fun putString(key: String, value: String) {
        dataStore.edit { prefs -> prefs[stringPreferencesKey(key)] = value }
    }

    override fun observeString(key: String): Flow<String?> =
        dataStore.data.map { prefs -> prefs[stringPreferencesKey(key)] }
}
```

- [ ] **Step 6: Write the desktop-specific sources (share, no-camera) and the platform module.**

  Create `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/DesktopShareSource.kt`:

```kotlin
package com.slothiesmooth.nyx.desktop

import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.ShareSource
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

/** Desktop "share" == a native Save-As dialog writing the stego PNG to the chosen location. */
class DesktopShareSource : ShareSource {
    override suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit> {
        val baseName = fileName.substringBeforeLast('.')
        val destination = FileKit.openFileSaver(suggestedName = baseName, extension = "png")
            ?: return AppResult.Ok(Unit) // user cancelled the dialog — not an error
        return runCatching { destination.write(bytes) }.fold(
            onSuccess = { AppResult.Ok(Unit) },
            onFailure = { error -> AppResult.Err(AppError.Storage("save failed", error)) },
        )
    }
}
```

  Create `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/NoCameraSource.kt`:

```kotlin
package com.slothiesmooth.nyx.desktop

import com.slothiesmooth.nyx.shared.data.CameraSource
import com.slothiesmooth.nyx.shared.data.PickedImage

/** Desktop has no system camera; PlatformCapabilities(camera = false) hides the UI button. */
object NoCameraSource : CameraSource {
    override val isAvailable: Boolean = false
    override suspend fun capture(): PickedImage? = null
}
```

  Create `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/DesktopPlatformModule.kt`:

```kotlin
package com.slothiesmooth.nyx.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import com.slothiesmooth.nyx.shared.data.CameraSource
import com.slothiesmooth.nyx.shared.data.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.SettingsSource
import com.slothiesmooth.nyx.shared.data.ShareSource
import com.slothiesmooth.nyx.shared.data.VaultFileStore
import com.slothiesmooth.nyx.shared.data.VaultSource
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.filesDir
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
// One import is intentionally omitted: the SqlDelight `synchronous()` extension used below on
// `NyxDb.Schema` (NyxDb.Schema is async because generateAsync = true). Add the SAME
// `import ...synchronous` line that plan 05's AndroidPlatformModule uses for AndroidSqliteDriver —
// it is the identical async→sync schema adapter on all three synchronous drivers.

private const val DATABASE_FILE_NAME = "nyx.db"
private const val SETTINGS_FILE_NAME = "nyx.preferences_pb"
private const val VAULT_DIR_NAME = "stego_vault"

fun desktopPlatformModule(): Module = module {
    single<SqlDriver> { desktopSqlDriver() }
    // SqlDelightSource(driver, scope): the second get() resolves the shared app CoroutineScope that
    // appModule (plan 05) registers as single<CoroutineScope>. SqlDelightSource builds NyxDb
    // internally, so there is no separate single { NyxDb(get()) }. VaultSqlSource wraps the source.
    single { com.slothiesmooth.nyx.client.data.source.database.sqldelight.SqlDelightSource(get(), get()) }
    single<VaultSource> { com.slothiesmooth.nyx.client.data.source.database.vault.VaultSqlSource(get()) }
    single<VaultFileStore> { FileKitVaultFileStore(desktopVaultRoot()) }
    single<SettingsSource> { DataStoreSettingsSource(desktopDataStore()) }
    single<ShareSource> { DesktopShareSource() }
    single<CameraSource> { NoCameraSource }
    single { PlatformCapabilities(camera = false, persistentVault = true) }
}

private fun desktopSqlDriver(): SqlDriver {
    val databasePath = "${FileKit.filesDir.path}/$DATABASE_FILE_NAME"
    return JdbcSqliteDriver(
        url = "jdbc:sqlite:$databasePath",
        schema = NyxDb.Schema.synchronous(),
    )
}

private fun desktopVaultRoot(): PlatformFile = PlatformFile(FileKit.filesDir, VAULT_DIR_NAME)

private fun desktopDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    produceFile = { "${FileKit.filesDir.path}/$SETTINGS_FILE_NAME".toPath() },
)
```

  **One edit required as you paste this** (it cannot be resolved from Linux without plan 05's exact
  code — see Open Questions):
  1. Add the `synchronous()` import (deliberately omitted above) — copy the exact
     `import ...synchronous` line from plan 05's `AndroidPlatformModule` (the SqlDelight async→sync
     schema adapter on `NyxDb.Schema`; plan 03 Task 14 shows it as
     `import app.cash.sqldelight.async.coroutines.synchronous`, the `async-extensions` artifact).
     It is the identical extension used for `AndroidSqliteDriver`. Until it is added,
     `NyxDb.Schema.synchronous()` is an unresolved reference — that is the one intentional
     cross-plan blank, not a compile-forever placeholder.

  The vault wiring is already pinned to plan 03's delivered shapes and needs NO reconciliation:
  `SqlDelightSource(driver: SqlDriver, scope: CoroutineScope)` (package
  `com.slothiesmooth.nyx.client.data.source.database.sqldelight`, builds `NyxDb` internally) and
  `VaultSqlSource(source: SqlDelightSource) : VaultSource` (package
  `com.slothiesmooth.nyx.client.data.source.database.vault`). The `single<SqlDriver>` feeds
  `SqlDelightSource`'s first arg; its second arg resolves the `single<CoroutineScope>` that
  `appModule` (plan 05) registers — do NOT re-register a `CoroutineScope` in this platform module.

- [ ] **Step 7: Write the desktop `main()`.** Create
  `desktopApp/src/jvmMain/kotlin/com/slothiesmooth/nyx/desktop/Main.kt`:

```kotlin
package com.slothiesmooth.nyx.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.slothiesmooth.nyx.client.App
import com.slothiesmooth.nyx.client.initKoin
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.init

fun main() {
    // JVM FileKit needs an appId before FileKit.filesDir is read (00-INDEX FileKit fact).
    FileKit.init(appId = "Nyx")
    initKoin(desktopPlatformModule())
    application {
        Window(onCloseRequest = ::exitApplication, title = "Nyx") {
            App()
        }
    }
}
```

- [ ] **Step 8: Compile the desktop target (green).** Run:

```bash
./gradlew :desktopApp:compileKotlinJvm
```

  Expected: `BUILD SUCCESSFUL`. If it fails on an unresolved `synchronous`, `FileKit.init`,
  `write`, or `openFileSaver`, apply the Step 6 reconciliation against plan 05's actual signatures
  (the `synchronous()` import is the only remaining cross-plan unknown — the vault-source
  constructors are already pinned to plan 03's shapes; the rest is self-contained). Do not
  `@Suppress` — fix the reference.

- [ ] **Step 9: Run the desktop app (Linux smoke test).** On a machine with a display:

```bash
./gradlew :desktopApp:run
```

  Expected: a window titled "Nyx" opens showing the app's start destination (the vault, in its
  empty state — no stego images yet). Verify the bottom navigation renders (Vault / Encrypt /
  Decrypt / Settings) and switching tabs does not crash. Close the window; gradle returns
  `BUILD SUCCESSFUL`. (Headless CI does the compile + package instead — Task 4.)

- [ ] **Step 10: Build a distributable (packaging smoke test).** Run:

```bash
./gradlew :desktopApp:createDistributable
```

  Expected: `BUILD SUCCESSFUL`; an app image (bundled JRE + app) appears under
  `desktopApp/build/compose/binaries/main/app/Nyx/`. `createDistributable` needs no OS packaging
  tools, so it is the CI-safe smoke. The full installer task
  `:desktopApp:packageDistributionForCurrentOS` (→ `.deb` on Linux) additionally needs
  `dpkg`/`fakeroot`; run it locally only when you actually want an installer.

- [ ] **Step 11: Commit.**

```bash
git add settings.gradle.kts gradle/libs.versions.toml desktopApp/ \
  client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/app/ androidApp/
git commit -m "feat(desktop): add :desktopApp Compose Desktop entry and DesktopPlatformModule

JdbcSqliteDriver vault at FileKit.filesDir/nyx.db, DataStore settings, FileKit
save-as share, no camera (capabilities false/true). Enforces the platform-module
boundary: appModule no longer registers VaultSource/VaultFileStore."
```

  Expected: commit created. (If Step 2 recorded "invariant holds", the `client/` and
  `androidApp/` paths add nothing — that is fine.)

---

### Task 2: `:webApp` — Compose/Wasm entry, in-memory vault, localStorage settings, PBKDF2 latency check

**Files:**
- Modify (verify/repair): `settings.gradle.kts` (`include(":webApp")`)
- Create/Overwrite: `webApp/build.gradle.kts`
- Create: `webApp/src/wasmJsMain/resources/index.html`
- Create: `webApp/src/wasmJsMain/resources/styles.css`
- Create: `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/Main.kt`
- Create: `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/WebPlatformModule.kt`
- Create: `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/InMemoryVaultSource.kt`
- Create: `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/InMemoryVaultFileStore.kt`
- Create: `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/LocalStorageSettingsSource.kt`
- Create: `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/WebShareSource.kt`
- Create: `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/NoCameraSource.kt`
- Test: `webApp/src/wasmJsTest/kotlin/com/slothiesmooth/nyx/web/InMemoryVaultSourceTest.kt`
- Test (config-task verification): `./gradlew :webApp:wasmJsBrowserTest`,
  `./gradlew :webApp:wasmJsBrowserDevelopmentRun`, `./gradlew :webApp:wasmJsBrowserDistribution`

**Interfaces:**
- Consumes: `fun initKoin(platformModule: Module)` and `@Composable fun App()` from `:client`;
  from `:shared:data` — `VaultSource`, `VaultFileStore`, `SettingsSource`, `ShareSource`,
  `CameraSource`, `PickedImage`, `PlatformCapabilities`, `StegoImageRecord`, `AppResult`,
  `AppError`. Relies on the Task 1 Step 2 invariant (`appModule` provides no vault sources).
- Produces: `fun webPlatformModule(): org.koin.core.module.Module`, `fun main()`, and the tasks
  `:webApp:wasmJsBrowserDevelopmentRun` / `:webApp:wasmJsBrowserDistribution` (consumed by plan
  08). Classes `InMemoryVaultSource`, `InMemoryVaultFileStore`, `LocalStorageSettingsSource`,
  `WebShareSource` are wasm-only and not consumed by other plans.

**`StegoImageRecord` shape (00-INDEX, reproduced so this task is self-contained):**

```kotlin
data class StegoImageRecord(
    val id: String, val name: String, val createdAt: String, val updatedAt: String,
    val deletedAt: String?, val isArchived: Boolean,
)
```

**Steps:**

- [ ] **Step 1: Verify registration, then compile the skeleton (failing check).** Run:

```bash
grep -n '":webApp"' settings.gradle.kts || echo "MISSING_INCLUDE"
./gradlew :webApp:compileKotlinWasmJs
```

  If `MISSING_INCLUDE`, add `include(":webApp")`. The compile against the bare plan-01 skeleton
  is the failing baseline (no `main()`, no platform module) — record it.

- [ ] **Step 2: Write the web build file.** Overwrite `webApp/build.gradle.kts` with exactly:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":client"))
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.filekit.core) // FileKit.download (webMain)
        }
        wasmJsTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

- [ ] **Step 3: Write `index.html` and `styles.css`.** ComposeViewport injects no CSS, so the
  stylesheet is what makes the canvas fill the viewport (00-INDEX wasm facts).

  Create `webApp/src/wasmJsMain/resources/index.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nyx</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <script src="composeApp.js"></script>
</body>
</html>
```

  Create `webApp/src/wasmJsMain/resources/styles.css`:

```css
html, body {
    width: 100%;
    height: 100%;
    margin: 0;
    overflow: hidden;
}
```

- [ ] **Step 4: Write the failing test for `InMemoryVaultSource` (TDD red).** Create
  `webApp/src/wasmJsTest/kotlin/com/slothiesmooth/nyx/web/InMemoryVaultSourceTest.kt`:

```kotlin
package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.StegoImageRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryVaultSourceTest {

    private fun record(
        id: String,
        createdAt: String = "2026-01-01T00:00:00Z",
        isArchived: Boolean = false,
        deletedAt: String? = null,
    ) = StegoImageRecord(
        id = id, name = "img-$id", createdAt = createdAt, updatedAt = createdAt,
        deletedAt = deletedAt, isArchived = isArchived,
    )

    @Test
    fun `upsert then observeActive emits the record and countActive is 1`() = runTest {
        val source = InMemoryVaultSource()
        source.upsert(record("a"))
        assertEquals(listOf("a"), source.observeActive().first().map { it.id })
        assertEquals(1, source.countActive())
    }

    @Test
    fun `observeActive orders newest first by createdAt`() = runTest {
        val source = InMemoryVaultSource()
        source.upsert(record("old", createdAt = "2026-01-01T00:00:00Z"))
        source.upsert(record("new", createdAt = "2026-02-01T00:00:00Z"))
        assertEquals(listOf("new", "old"), source.observeActive().first().map { it.id })
    }

    @Test
    fun `setArchived moves a record from active to archived`() = runTest {
        val source = InMemoryVaultSource()
        source.upsert(record("a"))
        source.setArchived("a", archived = true, updatedAt = "2026-03-01T00:00:00Z")
        assertEquals(emptyList(), source.observeActive().first().map { it.id })
        assertEquals(listOf("a"), source.observeArchived().first().map { it.id })
        assertEquals(0, source.countActive())
    }

    @Test
    fun `softDelete hides a record from both lists`() = runTest {
        val source = InMemoryVaultSource()
        source.upsert(record("a"))
        source.softDelete("a", deletedAt = "2026-03-02T00:00:00Z")
        assertEquals(emptyList(), source.observeActive().first().map { it.id })
        assertEquals(emptyList(), source.observeArchived().first().map { it.id })
    }

    @Test
    fun `purgeAll empties the source`() = runTest {
        val source = InMemoryVaultSource()
        source.upsert(record("a"))
        source.upsert(record("b"))
        source.purgeAll()
        assertEquals(0, source.countActive())
        assertEquals(emptyList(), source.observeActive().first().map { it.id })
    }
}
```

  Run it (this needs headless Chrome — see Task 4 for CI; locally, Chrome/Chromium must be on
  PATH):

```bash
./gradlew :webApp:wasmJsBrowserTest
```

  Expected failure: compilation error `Unresolved reference: InMemoryVaultSource` (the class
  does not exist yet). That is the red state.

- [ ] **Step 5: Implement `InMemoryVaultSource` (TDD green).** Create
  `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/InMemoryVaultSource.kt`:

```kotlin
package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.StegoImageRecord
import com.slothiesmooth.nyx.shared.data.VaultSource
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Session-only VaultSource for wasm (spec §6: no persistent vault on web in v1).
 * Mirrors VaultSqlSource semantics: active = not archived and not tombstoned, newest first.
 */
class InMemoryVaultSource : VaultSource {

    private val records = MutableStateFlow<PersistentList<StegoImageRecord>>(persistentListOf())

    override fun observeActive(): Flow<List<StegoImageRecord>> =
        records.map { list -> list.filter(::isActive).sortedByDescending { it.createdAt } }

    override fun observeArchived(): Flow<List<StegoImageRecord>> =
        records.map { list -> list.filter(::isArchived).sortedByDescending { it.createdAt } }

    override suspend fun getById(id: String): StegoImageRecord? =
        records.value.firstOrNull { record -> record.id == id }

    override suspend fun upsert(record: StegoImageRecord) {
        records.update { list ->
            val index = list.indexOfFirst { existing -> existing.id == record.id }
            if (index >= 0) list.set(index, record) else list.add(record)
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean, updatedAt: String) {
        records.update { list ->
            val index = list.indexOfFirst { existing -> existing.id == id }
            if (index < 0) {
                list
            } else {
                list.set(index, list[index].copy(isArchived = archived, updatedAt = updatedAt))
            }
        }
    }

    override suspend fun softDelete(id: String, deletedAt: String) {
        records.update { list ->
            val index = list.indexOfFirst { existing -> existing.id == id }
            if (index < 0) list else list.set(index, list[index].copy(deletedAt = deletedAt))
        }
    }

    override suspend fun purgeAll() {
        records.value = persistentListOf()
    }

    override suspend fun countActive(): Int = records.value.count(::isActive)

    private fun isActive(record: StegoImageRecord): Boolean =
        !record.isArchived && record.deletedAt == null

    private fun isArchived(record: StegoImageRecord): Boolean =
        record.isArchived && record.deletedAt == null
}
```

  Run again:

```bash
./gradlew :webApp:wasmJsBrowserTest
```

  Expected: `BUILD SUCCESSFUL`, all five tests pass. If Chrome is not installed locally, this
  test is deferred to the Task 4 CI job — note the deferral and continue (do not weaken the
  test).

- [ ] **Step 6: Implement the remaining wasm sources.**

  Create `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/InMemoryVaultFileStore.kt`:

```kotlin
package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.VaultFileStore

/** Session-only PNG store for wasm (single-threaded, so a plain map is safe). */
class InMemoryVaultFileStore : VaultFileStore {

    private val files = mutableMapOf<String, ByteArray>()

    override suspend fun write(id: String, bytes: ByteArray): AppResult<Unit> {
        files[id] = bytes
        return AppResult.Ok(Unit)
    }

    override suspend fun read(id: String): AppResult<ByteArray> {
        val bytes = files[id] ?: return AppResult.Err(AppError.NotFound)
        return AppResult.Ok(bytes)
    }

    override suspend fun delete(id: String): AppResult<Unit> {
        files.remove(id)
        return AppResult.Ok(Unit)
    }

    override suspend fun deleteAll(): AppResult<Unit> {
        files.clear()
        return AppResult.Ok(Unit)
    }
}
```

  Create `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/LocalStorageSettingsSource.kt`:

```kotlin
package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.SettingsSource
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * SettingsSource backed by browser localStorage. localStorage has no same-tab change event, so a
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
```

  Create `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/WebShareSource.kt`:

```kotlin
package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.ShareSource
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download

/** Web "share" == a browser download of the stego PNG (FileKit.download is webMain-only). */
class WebShareSource : ShareSource {
    override suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit> {
        FileKit.download(bytes = bytes, fileName = fileName)
        return AppResult.Ok(Unit)
    }
}
```

  Create `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/NoCameraSource.kt`:

```kotlin
package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.CameraSource
import com.slothiesmooth.nyx.shared.data.PickedImage

/** No camera on the web build; PlatformCapabilities(camera = false) hides the UI button. */
object NoCameraSource : CameraSource {
    override val isAvailable: Boolean = false
    override suspend fun capture(): PickedImage? = null
}
```

- [ ] **Step 7: Write the web platform module.** Create
  `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/WebPlatformModule.kt`:

```kotlin
package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.CameraSource
import com.slothiesmooth.nyx.shared.data.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.SettingsSource
import com.slothiesmooth.nyx.shared.data.ShareSource
import com.slothiesmooth.nyx.shared.data.VaultFileStore
import com.slothiesmooth.nyx.shared.data.VaultSource
import org.koin.core.module.Module
import org.koin.dsl.module

fun webPlatformModule(): Module = module {
    single<VaultSource> { InMemoryVaultSource() }
    single<VaultFileStore> { InMemoryVaultFileStore() }
    single<SettingsSource> { LocalStorageSettingsSource() }
    single<ShareSource> { WebShareSource() }
    single<CameraSource> { NoCameraSource }
    single { PlatformCapabilities(camera = false, persistentVault = false) }
}
```

- [ ] **Step 8: Write the web `main()`.** Create
  `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/Main.kt`:

```kotlin
package com.slothiesmooth.nyx.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.slothiesmooth.nyx.client.App
import com.slothiesmooth.nyx.client.initKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin(webPlatformModule())
    ComposeViewport {
        App()
    }
}
```

- [ ] **Step 9: Compile and build the production bundle (green).** Run:

```bash
./gradlew :webApp:compileKotlinWasmJs
./gradlew :webApp:wasmJsBrowserDistribution
```

  Expected: both `BUILD SUCCESSFUL`. The distribution appears under
  `webApp/build/dist/wasmJs/productionExecutable/` and contains `composeApp.js` (name matching
  `outputModuleName`/`outputFileName`), `index.html`, and `styles.css`. If the bundle is missing
  `composeApp.js`, the `outputModuleName`/`outputFileName` pair is wrong — fix Step 2, not the
  html.

- [ ] **Step 10: Serve the dev app and smoke-test the export-only vault.** Run:

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

  Expected: a dev server starts (default `http://localhost:8080/`) and a browser tab opens
  showing "Nyx" with the canvas filling the viewport. Manually confirm: the app renders the vault
  empty state; the encrypt flow accepts a picked image + message + password and produces a
  downloadable PNG (WebShareSource → browser download); the vault list is session-only (a page
  reload clears it — expected, `persistentVault = false`). Stop the server (Ctrl-C).

- [ ] **Step 11: Measure PBKDF2-in-browser latency (spec §11 escalation gate).** This is a
  one-off measurement, not shipped code — you will revert it. Temporarily edit
  `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/Main.kt` to add a measurement before
  `initKoin`, and temporarily add `implementation(project(":crypto"))`, `implementation(libs.kermit)`,
  and `implementation(libs.kotlinx.coroutines.core)` to `webApp/build.gradle.kts`'s
  `wasmJsMain.dependencies` (coroutines-core may already be there):

```kotlin
// TEMPORARY — measurement only, remove before committing.
import co.touchlab.kermit.Logger
import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

private fun measurePbkdf2() {
    MainScope().launch {
        val crypto = DefaultNyxCrypto()
        val startedAt = TimeSource.Monotonic.markNow()
        crypto.encrypt(plaintext = "hello", password = "correct horse battery staple")
        Logger.i { "PBKDF2 encrypt('hello') took ${startedAt.elapsedNow().inWholeMilliseconds} ms" }
    }
}
```

  Call `measurePbkdf2()` as the first line of `main()`. Then:

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

  Open the browser dev console and read the logged millisecond value.
  - **< 8000 ms** (expected — WebCrypto PBKDF2 is hardware-accelerated; 600k SHA-256 is typically
    well under 1 s): record the number in the commit body, revert the temporary edits (Main.kt +
    build.gradle.kts), and continue.
  - **≥ 8000 ms**: STOP and flag to the user with the measured value. Do NOT lower
    `PBKDF2_ITERATIONS` below 600 000 (spec §11 / OWASP floor). Revert the temporary edits; the
    escalation is the deliverable of this step, not a code change.

  After recording, confirm the temporary edits are gone:

```bash
grep -n "measurePbkdf2\|DefaultNyxCrypto" webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/Main.kt || echo "MEASUREMENT_REVERTED"
grep -n ':crypto' webApp/build.gradle.kts || echo "CRYPTO_DEP_REVERTED"
```

  Expected: `MEASUREMENT_REVERTED` and `CRYPTO_DEP_REVERTED`.

- [ ] **Step 12: Commit.**

```bash
git add settings.gradle.kts webApp/
git commit -m "feat(web): add :webApp Compose/Wasm entry with export-only vault

InMemoryVaultSource/FileStore (session-only), LocalStorageSettingsSource,
WebShareSource (FileKit.download), no camera (capabilities false/false).
PBKDF2 in-browser encrypt measured at <RECORD ms> (< 8s acceptance)."
```

  Expected: commit created (substitute the measured latency).

---

### Task 3: iOS scaffold — MainViewController, IosPlatformModule, iosApp/ Xcode project `[macOS-verify-later]`

Every step here is `[macOS-verify-later]`: on Linux the code below does not compile
(`kotlin.native.ignoreDisabledTargets=true` skips the iOS targets), and that is expected. The
Linux-runnable proof for this task is only "the shared build still compiles for its non-iOS
targets and nothing references iOS symbols from common code" (Step 8). The real verification
commands run on the macOS lane and are listed in Step 9.

**Files:**
- Create: `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/MainViewController.kt`
- Create: `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/KoinInit.kt`
- Create: `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/IosPlatformModule.kt`
- Create: `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/IosShareSource.kt`
- Create: `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/FileKitCameraSource.kt`
- Create: `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/FileKitVaultFileStore.kt`
- Create: `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/DataStoreSettingsSource.kt`
- Create: `client/iosApp/project.yml` (XcodeGen spec)
- Create: `client/iosApp/iosApp/iOSApp.swift`
- Create: `client/iosApp/iosApp/ContentView.swift`
- Create: `client/iosApp/iosApp/Info.plist`
- Modify (verify/repair): `client/build.gradle.kts` (framework `baseName = "App"`, `isStatic`,
  `linkerOpts("-lsqlite3")`), and add iOS deps `filekit-dialogs` + `datastore-preferences-core`
  + `native-driver` to `iosMain`.

**Interfaces:**
- Consumes: `@Composable fun App()`, `fun initKoin(platformModule: Module)`, `class NyxDb`,
  `class SqlDelightSource(driver, scope)`, `class VaultSqlSource(source: SqlDelightSource) : VaultSource`
  from `:client`; the `:shared:data` source interfaces. FileKit `openCameraPicker` (mobileMain —
  reachable from iosMain), `FileKit.filesDir` (nonWeb).
- Produces: `fun MainViewController(): UIViewController`, `fun initKoinIos()`,
  `fun iosPlatformModule(): Module` (exported to Swift via the `App` framework). Consumed by the
  `client/iosApp/` Xcode project and by plan 08's README (iosApp path).

**Steps:**

- [ ] **Step 1: Verify the `:client` iOS framework config (repair if plan 01/03 left it
  incomplete).** Run:

```bash
grep -n 'baseName\|isStatic\|framework\|lsqlite3' client/build.gradle.kts
```

  The framework must be named `App`, static, and link system sqlite (NativeSqliteDriver needs
  `-lsqlite3`). If any is missing, ensure `client/build.gradle.kts`'s `kotlin {}` contains:

```kotlin
listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
        baseName = "App"
        isStatic = true
        linkerOpts("-lsqlite3")
    }
}
```

  (If plan 01 declared the iOS targets without the `framework {}` block, add it. If the
  `-lsqlite3` linker opt is instead applied via the root `subprojects` block for all native
  binaries per spec §3, that also satisfies the requirement — record which mechanism is in use.)
  Also add to `iosMain.dependencies` in `client/build.gradle.kts` (if not already present):
  `implementation(libs.sqldelight.native.driver)`, `implementation(libs.androidx.datastore.preferences.core)`,
  `implementation(libs.filekit.core)`, `implementation(libs.filekit.dialogs)`.

- [ ] **Step 2: Write the iOS entry point and koin initializer.**

  Create `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/MainViewController.kt`:

```kotlin
package com.slothiesmooth.nyx.client

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}
```

  Create `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/KoinInit.kt`:

```kotlin
package com.slothiesmooth.nyx.client

/** Called once from the Swift App.init() before the first MainViewController is shown. */
fun initKoinIos() {
    initKoin(iosPlatformModule())
}
```

- [ ] **Step 3: Write the two duplicated glue classes for iOS.** These are the same classes as
  Task 1 Step 5, rehomed to package `com.slothiesmooth.nyx.client` (they live in `:client`
  iosMain). Copy Task 1's `FileKitVaultFileStore` and `DataStoreSettingsSource` verbatim except
  for the package line.

  Create `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/FileKitVaultFileStore.kt` —
  identical body to Task 1 Step 5's `FileKitVaultFileStore`, with header
  `package com.slothiesmooth.nyx.client`.

  Create `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/DataStoreSettingsSource.kt` —
  identical body to Task 1 Step 5's `DataStoreSettingsSource`, with header
  `package com.slothiesmooth.nyx.client`.

- [ ] **Step 4: Write the iOS camera and share sources.**

  Create `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/FileKitCameraSource.kt`:

```kotlin
package com.slothiesmooth.nyx.client

import com.slothiesmooth.nyx.shared.data.CameraSource
import com.slothiesmooth.nyx.shared.data.PickedImage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openCameraPicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes

/**
 * System-camera capture via FileKit (00-INDEX camera delta: no hand-rolled AVFoundation).
 * Requires NSCameraUsageDescription in Info.plist. Identical to plan 05's androidApp
 * FileKitCameraSource; only the package differs.
 */
class FileKitCameraSource : CameraSource {
    override val isAvailable: Boolean = true
    override suspend fun capture(): PickedImage? {
        val file = FileKit.openCameraPicker() ?: return null
        return PickedImage(bytes = file.readBytes(), suggestedName = file.name)
    }
}
```

  Create `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/IosShareSource.kt`:

```kotlin
package com.slothiesmooth.nyx.client

import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.ShareSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/** iOS "share" == UIActivityViewController over a temp-file NSURL for the stego PNG. */
class IosShareSource : ShareSource {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit> {
        return try {
            val filePath = NSTemporaryDirectory() + fileName
            val nsData = bytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
            }
            nsData.writeToFile(filePath, atomically = true)
            val fileUrl = NSURL.fileURLWithPath(filePath)
            val controller = UIActivityViewController(
                activityItems = listOf(fileUrl),
                applicationActivities = null,
            )
            val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootController?.presentViewController(controller, animated = true, completion = null)
            AppResult.Ok(Unit)
        } catch (error: Throwable) {
            AppResult.Err(AppError.Storage(error.message ?: "share failed", error))
        }
    }
}
```

  (`keyWindow` is deprecated on iOS 13+ but works for this scaffold; the macOS lane may replace it
  with a connected-scene lookup — noted, not blocking.)

- [ ] **Step 5: Write the iOS platform module.** Create
  `client/src/iosMain/kotlin/com/slothiesmooth/nyx/client/IosPlatformModule.kt`:

```kotlin
package com.slothiesmooth.nyx.client

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import com.slothiesmooth.nyx.shared.data.CameraSource
import com.slothiesmooth.nyx.shared.data.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.SettingsSource
import com.slothiesmooth.nyx.shared.data.ShareSource
import com.slothiesmooth.nyx.shared.data.VaultFileStore
import com.slothiesmooth.nyx.shared.data.VaultSource
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
// One import intentionally omitted: the SqlDelight `synchronous()` extension used on
// `NyxDb.Schema` below. Add the same `import ...synchronous` line plan 05 uses (see Task 1 Step 6).

private const val DATABASE_FILE_NAME = "nyx.db"
private const val SETTINGS_FILE_NAME = "nyx.preferences_pb"
private const val VAULT_DIR_NAME = "stego_vault"

fun iosPlatformModule(): Module = module {
    single<SqlDriver> { NativeSqliteDriver(NyxDb.Schema.synchronous(), DATABASE_FILE_NAME) }
    // SqlDelightSource(driver, scope): the second get() resolves the shared app CoroutineScope that
    // appModule (plan 05) registers as single<CoroutineScope>. SqlDelightSource builds NyxDb
    // internally, so there is no separate single { NyxDb(get()) }. VaultSqlSource wraps the source.
    single { com.slothiesmooth.nyx.client.data.source.database.sqldelight.SqlDelightSource(get(), get()) }
    single<VaultSource> { com.slothiesmooth.nyx.client.data.source.database.vault.VaultSqlSource(get()) }
    single<VaultFileStore> { FileKitVaultFileStore(PlatformFile(FileKit.filesDir, VAULT_DIR_NAME)) }
    single<SettingsSource> { DataStoreSettingsSource(iosDataStore()) }
    single<ShareSource> { IosShareSource() }
    single<CameraSource> { FileKitCameraSource() }
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
```

  Apply the same single reconciliation as Task 1 Step 6: add plan 05's real `synchronous()` import
  (deliberately omitted above). The vault wiring already matches plan 03's delivered
  `SqlDelightSource(driver, scope)` / `VaultSqlSource(source)` shapes (fully qualified above);
  `SqlDelightSource`'s second arg resolves the `single<CoroutineScope>` that `appModule` (plan 05)
  registers — do NOT re-register a `CoroutineScope` here.

- [ ] **Step 6: Write the Xcode scaffold (Swift + Info.plist + XcodeGen spec).** The project is a
  standard KMP-iOS host that renders `MainViewController` and calls `initKoinIos()` on launch.

  Create `client/iosApp/iosApp/iOSApp.swift`:

```swift
import SwiftUI

@main
struct NyxApp: App {
    init() {
        KoinInitKt.initKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea()
        }
    }
}
```

  Create `client/iosApp/iosApp/ContentView.swift`:

```swift
import SwiftUI
import UIKit
import App

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

  Create `client/iosApp/iosApp/Info.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>en</string>
    <key>CFBundleExecutable</key>
    <string>$(EXECUTABLE_NAME)</string>
    <key>CFBundleIdentifier</key>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>Nyx</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSRequiresIPhoneOS</key>
    <true/>
    <key>UILaunchScreen</key>
    <dict/>
    <key>NSCameraUsageDescription</key>
    <string>Nyx uses the camera to capture a cover image for your hidden message.</string>
    <key>NSPhotoLibraryUsageDescription</key>
    <string>Nyx needs access to your photos to pick a cover image and to save stego images.</string>
    <key>UISupportedInterfaceOrientations</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
    </array>
</dict>
</plist>
```

  Create `client/iosApp/project.yml` (XcodeGen spec — regenerated into `iosApp.xcodeproj` on the
  macOS lane; committing the yaml rather than a hand-written `project.pbxproj` avoids a fragile,
  unreviewable binary blob):

```yaml
name: iosApp
options:
  bundleIdPrefix: com.slothiesmooth.nyx
  deploymentTarget:
    iOS: "15.0"
targets:
  iosApp:
    type: application
    platform: iOS
    sources:
      - path: iosApp
    info:
      path: iosApp/Info.plist
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.slothiesmooth.nyx
        ENABLE_USER_SCRIPT_SANDBOXING: NO
    preBuildScripts:
      - name: "Build App.framework (Kotlin/Native)"
        script: |
          cd "$SRCROOT/../.."
          ./gradlew :client:embedAndSignAppleFrameworkForXcode
    settings:
      base:
        FRAMEWORK_SEARCH_PATHS:
          - "$(SRCROOT)/../build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)"
        OTHER_LDFLAGS:
          - "-framework"
          - "App"
```

  (This scaffold is committed as-is; it is `[macOS-verify-later]`. On the macOS lane run
  `brew install xcodegen`, then `cd client/iosApp && xcodegen generate` to produce
  `iosApp.xcodeproj`. If plan 05/pawdex instead ships a checked-in `iosApp.xcodeproj`, prefer
  mirroring that on the mac lane — see Open Questions.)

- [ ] **Step 7: Add an iOS README-note file so the deferred build is discoverable.** Create
  `client/iosApp/README.md`:

```markdown
# Nyx — iOS host app

This is the Xcode host that renders the shared Compose UI. It is **built on macOS only**
(Kotlin/Native iOS targets do not compile on Linux; `kotlin.native.ignoreDisabledTargets=true`
skips them elsewhere).

## Build (macOS + Xcode required)

```bash
brew install xcodegen            # once
cd client/iosApp
xcodegen generate                # creates iosApp.xcodeproj from project.yml
open iosApp.xcodeproj            # build & run in Xcode, or:
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15' build
```

The Kotlin framework is produced by `./gradlew :client:embedAndSignAppleFrameworkForXcode`
(wired as a pre-build script in `project.yml`).
```

- [ ] **Step 8: Linux-runnable proof — the shared build is unaffected by the iOS additions.**
  On Linux the iOS source set does not compile (targets skipped), so verify instead that nothing
  in common code references the new iOS symbols and that the non-iOS world still builds:

```bash
./gradlew :client:compileKotlinJvm :client:compileKotlinWasmJs
grep -rn "MainViewController\|iosPlatformModule\|IosShareSource" \
  client/src/commonMain client/src/jvmMain client/src/wasmJsMain 2>/dev/null || echo "NO_IOS_LEAK"
```

  Expected: both compiles `BUILD SUCCESSFUL`, and `NO_IOS_LEAK` (the iOS entry symbols are
  referenced only from iosMain and the Swift host — never from common/jvm/wasm).

- [ ] **Step 9: Record the exact macOS-lane verification commands (documentation, not run on
  Linux).** These are what the mac lane / CI (Task 4) runs to actually prove iOS:

```bash
# 1. Kotlin/Native iOS compile + static framework link (proves IosPlatformModule et al compile):
./gradlew :client:compileKotlinIosSimulatorArm64
./gradlew :client:linkDebugFrameworkIosSimulatorArm64
# 2. Xcode host build (proves the Swift scaffold + framework embedding):
cd client/iosApp && xcodegen generate
xcodebuild -project client/iosApp/iosApp.xcodeproj -scheme iosApp \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15' build
```

  Do NOT run these on Linux (they will fail on the missing Apple toolchain — that is expected and
  is not a defect in this plan). They belong to the macOS workflow added in Task 4.

- [ ] **Step 10: Commit.**

```bash
git add client/src/iosMain client/iosApp client/build.gradle.kts
git commit -m "feat(ios): add iOS entry (MainViewController), IosPlatformModule, iosApp scaffold

NativeSqliteDriver vault, DataStore (NSDocumentDirectory), FileKit camera,
UIActivityViewController share, capabilities(true/true). XcodeGen project.yml +
Swift host + Info.plist (NSCameraUsageDescription). [macOS-verify-later]:
framework link and Xcode build run on the macOS lane."
```

  Expected: commit created. `./gradlew :client:compileKotlinJvm :client:compileKotlinWasmJs`
  still green (Step 8) confirms Linux is not blocked.

---

### Task 4: CI additions — desktop package job, wasm browser-test job, optional macOS iOS lane

**Files:**
- Modify: `.github/workflows/<ci-workflow>.yml` (plan 01 owns the filename — Step 1 finds it;
  spec §3 name is `ci.yml`)
- Create: `.github/workflows/ios.yml`
- Test: `actionlint` (best-effort) + local parity runs of the added gradle commands

**Interfaces:**
- Consumes: plan 01's CI workflow (its checkout + JDK-21 setup steps, its `runs-on`/cache
  pattern); the tasks produced by Tasks 1–3 (`:desktopApp:createDistributable`,
  `:webApp:wasmJsBrowserDistribution`, `:webApp:wasmJsBrowserTest`,
  `:client:linkDebugFrameworkIosSimulatorArm64`).
- Produces: the CI jobs that guard the three entry apps. Consumed by plan 08's final CI polish
  and README badges.

**Steps:**

- [ ] **Step 1: Locate the existing workflow and read its shape.** Run:

```bash
ls .github/workflows/
grep -n 'runs-on\|actions/checkout\|setup-java\|java-version\|jobs:' .github/workflows/*.yml
```

  Identify plan 01's main CI file (spec §3: `ci.yml`) and note the exact checkout + JDK-21 setup
  step block it uses — the new jobs below reuse that same setup. If the filename is not `ci.yml`,
  substitute the real name wherever `ci.yml` appears below.

- [ ] **Step 2: Add the desktop package job.** In the main workflow's `jobs:` map, add a job that
  builds the desktop distributable on Linux (compile + `createDistributable`; no window, so no
  xvfb needed — we do not run the app in CI). Reuse the workflow's existing checkout/JDK setup
  steps (shown here as the standard pattern; align with what Step 1 found):

```yaml
  desktop:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
      - uses: gradle/actions/setup-gradle@v4
      - name: Compile desktop + build distributable
        run: ./gradlew :desktopApp:compileKotlinJvm :desktopApp:createDistributable
```

  Verify locally that these commands are green:

```bash
./gradlew :desktopApp:compileKotlinJvm :desktopApp:createDistributable
```

  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Add the wasm browser-test job.** wasm tests run in a real browser via Karma;
  `useChromeHeadless()` (set in `webApp/build.gradle.kts`, Task 2 Step 2) picks up Chrome, which
  ubuntu-latest ships preinstalled. Add:

```yaml
  web:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
      - uses: gradle/actions/setup-gradle@v4
      - name: Wasm browser tests + production bundle
        run: ./gradlew :webApp:wasmJsBrowserTest :webApp:wasmJsBrowserDistribution
```

  Verify locally (needs Chrome/Chromium on PATH):

```bash
./gradlew :webApp:wasmJsBrowserTest :webApp:wasmJsBrowserDistribution
```

  Expected: `BUILD SUCCESSFUL`; the `InMemoryVaultSource` tests run under headless Chrome. If
  Chrome is genuinely unavailable locally, rely on the CI runner for this one and note the
  deferral — but the `wasmJsBrowserDistribution` half compiles the whole web app and must pass.

- [ ] **Step 4: Create the optional macOS iOS lane.** iOS compiles only on macOS and macOS
  runners are metered, so this is a separate, manually/PR-triggered workflow, not part of every
  push. Create `.github/workflows/ios.yml`:

```yaml
name: iOS

on:
  workflow_dispatch:
  pull_request:
    paths:
      - "client/src/iosMain/**"
      - "client/iosApp/**"
      - "client/build.gradle.kts"

jobs:
  ios-compile:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
      - uses: gradle/actions/setup-gradle@v4
      - name: Compile + link iOS framework
        run: |
          ./gradlew :client:compileKotlinIosSimulatorArm64
          ./gradlew :client:linkDebugFrameworkIosSimulatorArm64
```

  (This lane proves the Kotlin/Native iOS side — the `IosPlatformModule`, share, and camera code
  from Task 3. Full Xcode host build via `xcodegen` + `xcodebuild` is an optional follow-on step
  the mac lane may add once a checked-in or generated `.xcodeproj` is confirmed; it is not
  required for the phase deliverable "iOS scaffold committed".)

- [ ] **Step 5: Lint the workflows (best-effort) and commit.** Run:

```bash
command -v actionlint >/dev/null && actionlint .github/workflows/*.yml \
  || echo "ACTIONLINT_NOT_INSTALLED (skip — Steps 2-3 already ran the gradle commands)"
```

  Expected: no findings, or the documented skip. Do not install tooling just for this. Then:

```bash
git add .github/workflows/
git commit -m "ci: add desktop package, wasm browser-test, and optional macOS iOS jobs

desktop = compile + createDistributable (ubuntu); web = wasmJsBrowserTest +
wasmJsBrowserDistribution (headless Chrome); ios = macos-latest lane
(workflow_dispatch + iosMain PR paths) running compile + linkDebugFramework."
```

  Expected: commit created.

- [ ] **Step 6: Final phase verification.** Prove the whole non-iOS world still builds green with
  all three entry apps and their CI in place:

```bash
./gradlew :desktopApp:compileKotlinJvm :webApp:compileKotlinWasmJs :client:compileKotlinJvm
./gradlew build
```

  Expected: `BUILD SUCCESSFUL`. `build` exercises host tests (including the `InMemoryVaultSource`
  suite where the runner supports wasm). This is the entry-gate that plan 08 (teardown) checks
  before deleting the legacy tree. Report completion to the orchestrator; do not merge or open a
  PR unless asked (superpowers:finishing-a-development-branch).
