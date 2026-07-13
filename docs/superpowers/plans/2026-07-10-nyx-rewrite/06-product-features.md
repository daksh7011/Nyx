# Product Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the four product features — vault, encrypt, decrypt, settings — end-to-end so the app fully works on Android: encrypt wizard → vault grid → detail → decrypt reveal → settings wipe, with use-case/VM/repository tests green and Compose UI tests for the two critical flows.

**Architecture:** Each feature is a pawdex-style `api` (routes + `Feature` interface) + `basic` (domain → data → presentation → `BasicXProvider`) pair. `basic` modules consume ONLY shared sources (`:shared:data` interfaces), the pure engines (`:crypto`, `:steganography`), and their own domain; no feature depends on another feature's `basic`. Cross-feature navigation targets are pushed through the `FeatureContext` using the target feature's `api`-module route (routes-only dependency). ViewModels own all logic and expose render-ready state; composables are dumb.

**Tech Stack:** Kotlin 2.3.21, Compose Multiplatform 1.10.3, Koin 4.2.1 (isolated feature contexts), JetBrains navigation-compose 2.9.2, kotlinx-coroutines 1.10.2, kotlinx-datetime 0.7.1 (`kotlin.time.Instant`), kotlinx-collections-immutable 0.4.0, FileKit 0.13.0 (image picker), skiko (bytes→ImageBitmap on non-Android), cryptography-kotlin 0.6.0 + `:crypto`, `:steganography`, `:shared:design-library` Nx components.

## Global Constraints

Copied verbatim from `00-INDEX.md` "Global constraints" (all of 00-INDEX applies; these are the lines this phase exercises):

- Kotlin 2.3.21, AGP 9.2.0, Gradle 9.4.1, Compose Multiplatform 1.10.3, JVM target 21, compileSdk 36, targetSdk 36, minSdk 24.
- KMP targets on every KMP module: `androidTarget` (via `com.android.kotlin.multiplatform.library`, configured as `kotlin { android {} }`), `iosX64`, `iosArm64`, `iosSimulatorArm64`, `jvm`, `wasmJs`. `applyDefaultHierarchyTemplate()`. iOS compiles only on macOS — never gate Linux progress on iOS; `kotlin.native.ignoreDisabledTargets=true`.
- Namespace/package: `com.slothiesmooth.nyx.<area>` (full reverse-domain everywhere; Android `namespace` per module must be unique).
- US English in all identifiers/comments/docs/commits. No `@Suppress`-style gate-passers — the single documented exception: `NxColors.kt` may suppress MagicNumber (the one raw-ARGB file).
- kotlinx ImmutableCollections for ALL collections in state/domain surfaces (ImmutableList/Set/Map).
- Injected `Clock` only — direct `kotlin.time.Clock.System`/`kotlinx.datetime` system access banned outside `SystemClock`.
- No feature-to-feature api dependencies. `DomainEventBus` = only cross-feature channel.
- Repository writes return `AppResult`, reads return `Flow`. Use cases = single-purpose classes with `operator fun invoke`, `factoryOf`-registered.
- Composables are dumb: no filtering/sorting/mapping/pluralization in UI — VM state exposes render-ready values.
- Tests: kotlin.test + kotlinx-coroutines-test + hand-written fakes only. No mockk/kotest/turbine.
- `suspend` end-to-end for crypto (WebCrypto provider is suspend-only; `*Blocking` throws on wasm).
- No `println`; logging via Kermit.
- Commit after every green test cycle (conventional commits).

Phase-specific constraints:

- **Cross-feature navigation exception (documented):** the "no feature-to-feature api dependencies" rule governs DATA/LOGIC coupling — data flows only through shared sources + `DomainEventBus` + projection interfaces. A `basic` module MAY depend on another feature's `api` module SOLELY to reference its `@Serializable` navigation route when pushing it through `FeatureContext` (routes carry no logic). This phase adds exactly three such edges: `vault.basic → encrypt.api` (empty-state CTA → `EncryptRoute`), `vault.basic → decrypt.api` (detail "decrypt this" → `DecryptRoute(imageId)`), `settings.basic → theme.api` (settings → `ThemeRoute`). No other cross-feature edges exist.
- **Dumb UI is absolute:** bytes→`ImageBitmap` decoding, `Instant`→date-string formatting, capacity/pluralization math, and `DecryptOutcome`→error-text mapping all happen in the ViewModel (or use case). State exposes only strings, `ImmutableList`s, and `ImageBitmap?` thumbnails. Composables never `map`/`filter`/`decode`/`format`.
- **Dimension/number purity (detekt maxIssues=0):** dp/sp literals and non-token integers appear ONLY as named `private val`/`private const val` at file top (satisfies detekt `ignorePropertyDeclaration`/`ignoreConstantDeclaration`). Never inline a magic number in a composable/function body. Prefer `NxSpacing.*`/`NxRadius.*` tokens over raw dp. No `@Suppress` anywhere in this phase.
- **Feature-level previews are REQUIRED (deviation from pawdex zero-previews, per user global rule):** every screen `Content` composable gets `private @AllThemePreview` preview functions covering its meaningful `UiState`/data variants (loading, empty, content, error where each applies), rendering the pure `Content` over a fake state.

## Contracts consumed from upstream plans (pinned — do not re-derive)

Everything below is an EXACT signature this plan calls. If an upstream plan's delivered signature differs, that is a mechanical reconcile at the call site (upstream wins on names). Sources: `00-INDEX.md` INTERFACE CONTRACTS; plan 02 (`:crypto`, `:steganography`); plan 03 (`:shared:data`, `:shared:presentation`, `:shared:test-support`, `:shared:compose-test-support`); plan 04 (`:shared:design-library`); plan 05 (feature plumbing, DI, `runFeatureUiTest`).

### From `:shared:data` (plan 03) — package `com.slothiesmooth.nyx.shared.data`

```kotlin
sealed interface AppResult<out T> { data class Ok<T>(val value: T) : AppResult<T>; data class Err(val cause: AppError) : AppResult<Nothing> }
sealed interface AppError {
    data object NotFound : AppError
    data class Validation(val message: String) : AppError
    data class Storage(val message: String, val cause: Throwable? = null) : AppError
    data object Permission : AppError
    data class Conflict(val message: String) : AppError
}
@JvmInline value class StegoImageId(val value: String)
interface IdGenerator { fun newId(): String }
interface Clock { fun now(): Instant; fun today(): LocalDate; fun zone(): TimeZone; fun nowLocal(): LocalDateTime }  // kotlin.time.Instant
sealed interface DomainEvent {
    data class StegoImageStored(val id: StegoImageId) : DomainEvent
    data class StegoImageArchived(val id: StegoImageId) : DomainEvent
    data class StegoImageRestored(val id: StegoImageId) : DomainEvent
    data class StegoImageDeleted(val id: StegoImageId) : DomainEvent
    data object VaultWiped : DomainEvent
}
interface DomainEventBus { val events: SharedFlow<DomainEvent>; suspend fun emit(event: DomainEvent) }
interface ImageCodec { suspend fun decode(bytes: ByteArray): AppResult<PixelImage>; suspend fun encodePng(image: PixelImage): AppResult<ByteArray> }
data class StegoImageRecord(val id: String, val name: String, val createdAt: String, val updatedAt: String, val deletedAt: String?, val isArchived: Boolean)
interface VaultSource {
    fun observeActive(): Flow<List<StegoImageRecord>>
    fun observeArchived(): Flow<List<StegoImageRecord>>
    suspend fun getById(id: String): StegoImageRecord?
    suspend fun upsert(record: StegoImageRecord)
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: String)
    suspend fun softDelete(id: String, deletedAt: String)
    suspend fun purgeAll()
    suspend fun countActive(): Int
}
interface VaultFileStore {
    suspend fun write(id: String, bytes: ByteArray): AppResult<Unit>
    suspend fun read(id: String): AppResult<ByteArray>
    suspend fun delete(id: String): AppResult<Unit>
    suspend fun deleteAll(): AppResult<Unit>
}
data class PickedImage(val bytes: ByteArray, val suggestedName: String?)
interface CameraSource { val isAvailable: Boolean; suspend fun capture(): PickedImage? }
interface ShareSource { suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit> }
data class PlatformCapabilities(val camera: Boolean, val persistentVault: Boolean)
```

This plan ADDS one new source interface to `:shared:data` in Task 1: `AppInfo` (see Task 1).

### From `:crypto` (plan 02) — package `com.slothiesmooth.nyx.crypto`

```kotlin
sealed interface DecryptResult { data class Success(val plaintext: String) : DecryptResult; data object WrongPasswordOrTampered : DecryptResult; data class Failure(val reason: String) : DecryptResult }
interface NyxCrypto { suspend fun encrypt(plaintext: String, password: String): String; suspend fun decrypt(blob: String, password: String): DecryptResult }
class DefaultNyxCrypto(provider: CryptographyProvider = CryptographyProvider.Default) : NyxCrypto
```

### From `:steganography` (plan 02) — package `com.slothiesmooth.nyx.steganography`

```kotlin
class PixelImage(val width: Int, val height: Int, val pixels: IntArray)  // ARGB, size == width*height
sealed interface StegoEncodeResult {
    data class Success(val images: List<PixelImage>) : StegoEncodeResult
    data class CapacityExceeded(val requiredBits: Long, val availableBits: Long) : StegoEncodeResult
}
class Steganography(startMarker: String = "@!#", endMarker: String = "#!@") {
    suspend fun encode(images: List<PixelImage>, payload: String): StegoEncodeResult
    suspend fun decode(images: List<PixelImage>): String?
}
```

### From `:shared:presentation` (plan 03) — package `com.slothiesmooth.nyx.shared.presentation`

```kotlin
@Immutable abstract class BaseViewModel : ViewModel() {
    protected fun async(id: String, force: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job?
    protected fun ui(id: String, force: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job?
    protected fun withState(block: () -> Unit)
    protected open fun doInit() {}
    @Composable fun bind()
}
@Stable interface ViewState { val uiState: UiState; val uiEvent: Flow<UiEvent> }
abstract class MutableViewState : ViewState { override var uiState: UiState; fun notify(event: UiEvent) }
sealed interface UiState {
    data object Ready : UiState
    data object Loading : UiState
    data object Blocking : UiState
    data class Error(val title: String, val cause: Throwable?, val onExit: () -> Unit) : UiState
}
interface UiEvent
// This plan ADDS `expect fun ByteArray.toImageBitmap(): ImageBitmap` here (Task 1).
```

### From `:feature:common:client:{api,koin}` (plan 05)

```kotlin
// api — com.slothiesmooth.nyx.feature.common.api
interface Feature
interface FeatureContext {
    fun pushDestination(route: Any)
    fun popDestination()
    fun setDestination(route: Any)
    fun replaceDestination(route: Any)
    fun restoreDestination(route: Any)
}
interface FeatureProvider {
    @Composable fun provideContent(context: FeatureContext, content: @Composable () -> Unit)
    fun provideNavigation(context: FeatureContext, builder: NavGraphBuilder)
}
abstract class BaseFeatureProvider : FeatureProvider {   // internal nav-action SharedFlow; onSendAction/onReceiveAction
    // final override provideContent(context, content): LaunchedEffect(context) collects the Action flow, then delegates to onProvideContent
    @Composable abstract fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit)  // template method — every concrete provider MUST override
    open fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {}                   // template method; base's provideNavigation delegates here
}

// koin — com.slothiesmooth.nyx.feature.common.koin
abstract class KoinFeatureProvider : BaseFeatureProvider() {
    open fun Module.onProvideDI() {}
    @Composable fun withDI(content: @Composable () -> Unit)                               // sets the isolated Koin context for the subtree
}
@Composable inline fun <reified T : BaseViewModel> koinFeatureViewModel(): T             // resolves in isolated context + calls bind()
```

**Provider pattern this plan uses (pinned):** each `BasicXProvider` extends `KoinFeatureProvider()` and implements its api `XFeature`. It overrides `@Composable fun onProvideContent(context, content) = content()` (the abstract template on `BaseFeatureProvider` — per-route `withDI` is applied inside `onProvideNavigation`, so this chain wrapper just passes `content()` through), `fun Module.onProvideDI()` (register outer deps + repos/use-cases/VMs), and `onProvideNavigation(context, builder)` (register `composable<Route> { withDI { XScreen(...) } }`, wiring navigation lambdas that call `context.pushDestination(...)` / `context.popDestination()` directly). Screens resolve VMs via `koinFeatureViewModel<T>()`. (This plan navigates directly through the `FeatureContext` passed to `onProvideNavigation` rather than the optional `onSendAction`/`onReceiveAction` relay — simpler and fully sufficient; the relay remains available from `BaseFeatureProvider` if a later need arises.)

### From `:shared:compose-test-support` (plan 05) — `runFeatureUiTest`

Pinned assumption (see Open Questions — confirm exact shape against plan 05): `runFeatureUiTest` boots an isolated Koin app with in-memory `VaultSource`/`VaultFileStore`, `FakeClock(fixed)`, `DeterministicIdGenerator`, and the full `FeatureHost`, then runs a Compose UI test. This plan assumes the block receives a scope exposing (a) the standard `ComposeUiTest` finders (`onNodeWithText`, `onAllNodesWithText`, `performClick`, `performTextInput`, `waitUntil`, `waitForIdle`) and (b) `val koin: Koin` (the isolated app Koin) so a test can resolve a ViewModel/source to seed picker bytes (the FileKit system picker cannot be driven headlessly). Signature used: `fun runFeatureUiTest(block: FeatureUiTestScope.() -> Unit)`.

### From `:shared:design-library` (plan 04) — call surface this plan uses

Confirmed in plan 04 (Tasks 5-10): `NxText`, `NxIcon`, `NxIconButton`, `NxButton`, `NxField`, `NxPasswordField`, `NxChip`, `NxCard`, plus enums `NxTextStyle`, `NxButtonStyle{Primary,Soft,Ghost,Danger}`, `NxButtonSize{Regular,Small}`, `NxIconButtonStyle{Outline,Ghost,Filled}`, `NxCardVariant{Elevated,Flat}`, `NxIconKind{Plus,ChevronLeft,ChevronRight,Eye,EyeOff,Lock,Unlock,Image,Camera,Share,Trash,Archive,Restore,Copy,Check,Close,Settings,Palette,Info,Warning,Vault}`, tokens `NxTokens.colors/type/spacing/radius`, `NxTheme(palette)`, `@AllThemePreview`, `NxPaletteProvider`.

Pinned molecule/template signatures (00-INDEX component list; plan 04 Tasks 11-20 must expose exactly these — see Open Questions):

```kotlin
@Composable fun NxTopBar(title: String, modifier: Modifier = Modifier, subtitle: String? = null, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {})
@Composable fun NxEmptyState(icon: NxIconKind, title: String, body: String, modifier: Modifier = Modifier, ctaText: String? = null, onCta: (() -> Unit)? = null)
@Composable fun NxProgressOverlay(label: String, modifier: Modifier = Modifier)
@Composable fun NxSectionHeader(title: String, modifier: Modifier = Modifier, actionText: String? = null, onAction: (() -> Unit)? = null)
@Composable fun NxImageTile(image: ImageBitmap?, modifier: Modifier = Modifier, selected: Boolean = false, contentDescription: String? = null, onClick: (() -> Unit)? = null)
@Composable fun NxFab(icon: NxIconKind, onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String? = null)
@Composable fun NxDetailTemplate(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit)
@Composable fun NxWizardTemplate(stepLabels: ImmutableList<String>, currentStep: Int, title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)
```

`NxImageTile` takes an already-decoded `ImageBitmap?` (NOT raw bytes) — decoding is logic and lives in the VM, so the tile stays dumb.

### From feature `api` modules (00-INDEX; finalized by plan 05, this plan uses them)

```kotlin
// vault.api    — com.slothiesmooth.nyx.feature.vault.api
interface VaultFeature : Feature { fun observeActiveCount(): Flow<Int> }
@Serializable data object VaultRoute
@Serializable data class VaultDetailRoute(val imageId: String)
// encrypt.api  — com.slothiesmooth.nyx.feature.encrypt.api
interface EncryptFeature : Feature ; @Serializable data object EncryptRoute
// decrypt.api  — com.slothiesmooth.nyx.feature.decrypt.api
interface DecryptFeature : Feature ; @Serializable data class DecryptRoute(val imageId: String? = null)
// theme.api    — com.slothiesmooth.nyx.feature.theme.api
@Serializable data object ThemeRoute
// settings.api — com.slothiesmooth.nyx.feature.settings.api
interface SettingsFeature : Feature ; @Serializable data object SettingsRoute
```

This plan ADDS `@Serializable data object SettingsLicensesRoute` to `settings.api` (Task 12) — an internal settings sub-route, consumed by no other feature.

---

### Task 1: Shared support — `toImageBitmap()` (`:shared:presentation`) + `AppInfo` (`:shared:data`)

**Files:**
- Modify: `shared/presentation/build.gradle.kts` (add `skikoMain` intermediate source set uniting iosMain+jvmMain+wasmJsMain)
- Create: `shared/presentation/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/presentation/image/ImageBitmapDecode.kt` (expect)
- Create: `shared/presentation/src/androidMain/kotlin/com/slothiesmooth/nyx/shared/presentation/image/ImageBitmapDecode.android.kt` (actual — BitmapFactory)
- Create: `shared/presentation/src/skikoMain/kotlin/com/slothiesmooth/nyx/shared/presentation/image/ImageBitmapDecode.skiko.kt` (actual — skiko)
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/source/AppInfo.kt`

**Interfaces:**
- Consumes: `:shared:presentation` and `:shared:data` modules (plans 03), CMP `ImageBitmap`, skiko (transitive via `compose.ui` on non-Android targets), Android `BitmapFactory`.
- Produces (other plans/features consume):
  - `expect fun ByteArray.toImageBitmap(): ImageBitmap` (androidMain + skikoMain actuals — iOS actual compiles on macOS via the same skikoMain source).
  - `interface AppInfo { val versionName: String; val platformName: String }`.

**Steps:**

- [ ] **Step 1: Add the `skikoMain` intermediate source set** to `shared/presentation/build.gradle.kts`. Inside the existing `kotlin { sourceSets { ... } }` block, add (the default hierarchy template does NOT create a set uniting jvm+ios+wasm; this one does, so a single skiko actual serves all three non-Android skia targets):

```kotlin
        val skikoMain by creating { dependsOn(commonMain.get()) }
        iosMain.get().dependsOn(skikoMain)
        jvmMain.get().dependsOn(skikoMain)
        wasmJsMain.get().dependsOn(skikoMain)
```

Ensure `commonMain` depends on `compose.ui` (ImageBitmap type). If plan 03 did not already add it, add `implementation(compose.ui)` to `commonMain.dependencies`. Verify: `./gradlew :shared:presentation:compileKotlinJvm` → `BUILD SUCCESSFUL`.

- [ ] **Step 2: Write the `expect` decoder.** Create `ImageBitmapDecode.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.presentation.image

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes encoded image bytes (PNG/JPEG) into a Compose [ImageBitmap] for display.
 * Decoding is platform work and never belongs in a composable — call this from a ViewModel
 * on a background dispatcher and expose the resulting [ImageBitmap] as render-ready state.
 */
expect fun ByteArray.toImageBitmap(): ImageBitmap
```

- [ ] **Step 3: Write the Android actual.** Create `ImageBitmapDecode.android.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.presentation.image

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.toImageBitmap(): ImageBitmap =
    BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()
```

- [ ] **Step 4: Write the skiko actual.** Create `ImageBitmapDecode.skiko.kt` (serves jvm/ios/wasmJs; skiko is on the classpath via `compose.ui` for these targets — 00-INDEX):

```kotlin
package com.slothiesmooth.nyx.shared.presentation.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun ByteArray.toImageBitmap(): ImageBitmap =
    Image.makeFromEncoded(this).toComposeImageBitmap()
```

- [ ] **Step 5: Write `AppInfo`.** Create `AppInfo.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.source

/**
 * Read-only application identity for the About screen. Implemented per platform entry module
 * (Android in this phase; desktop/web/iOS in plan 07) and injected via the platform Koin module.
 */
interface AppInfo {
    val versionName: String
    val platformName: String
}
```

- [ ] **Step 6: Verify both modules compile on Android + JVM.**

```bash
./gradlew :shared:presentation:compileKotlinJvm :shared:presentation:compileReleaseKotlinAndroid :shared:data:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. (If skiko's `Image`/`toComposeImageBitmap` is unresolved on jvm, `compose.ui` is missing from the non-Android classpath — confirm Step 1.)

- [ ] **Step 7: Commit.**

```bash
git add shared/presentation shared/data
git commit -m "feat(shared): ByteArray.toImageBitmap expect/actual and AppInfo source interface"
```

---

### Task 2: Test-support fakes (`:shared:test-support`)

**Files:**
- Create: `shared/test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/FakeVaultSource.kt`
- Create: `shared/test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/FakeVaultFileStore.kt`
- Create: `shared/test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/FakeShareSource.kt`
- Create: `shared/test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/FakeCameraSource.kt`
- Create: `shared/test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/FakeImageCodec.kt`
- Test: `shared/test-support/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/testsupport/FakeSourcesTest.kt`

**Interfaces:**
- Consumes: `:shared:data` (`VaultSource`, `VaultFileStore`, `ShareSource`, `CameraSource`, `ImageCodec`, `StegoImageRecord`, `AppResult`, `AppError`, `PickedImage`), `:steganography` (`PixelImage`), kotlinx-coroutines Flow, kotlinx-collections-immutable.
- Produces (every feature `basic` `commonTest` consumes these):
  - `class FakeVaultSource : VaultSource` — in-memory, `MutableStateFlow`-backed observation.
  - `class FakeVaultFileStore : VaultFileStore` — in-memory `MutableMap<String, ByteArray>`; `var failNextWrite: Boolean` to force a Storage error.
  - `class FakeShareSource(var result: AppResult<Unit> = AppResult.Ok(Unit)) : ShareSource` — records `lastShared: Pair<ByteArray, String>?`.
  - `class FakeCameraSource(override val isAvailable: Boolean = true) : CameraSource` — `var next: PickedImage?`.
  - `class FakeImageCodec : ImageCodec` — lossless round-trip: `encodePng` packs `[width][height][pixels…]` little-endian; `decode` unpacks and forces alpha opaque (`or 0xFF000000`), matching the real codec contract so `encode→stego→decode` survives in `commonTest`.

**Steps:**

- [ ] **Step 1: Write the failing self-test.** Create `FakeSourcesTest.kt` (these fakes are non-trivial — the codec round-trip and the source active/archived filtering are logic worth locking):

```kotlin
package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.steganography.PixelImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeSourcesTest {

    private fun record(id: String, archived: Boolean = false, deleted: String? = null) =
        StegoImageRecord(id = id, name = "n-$id", createdAt = "2026-07-13T00:00:00Z", updatedAt = "2026-07-13T00:00:00Z", deletedAt = deleted, isArchived = archived)

    @Test
    fun `active excludes archived and deleted`() = runTest {
        val source = FakeVaultSource()
        source.upsert(record("a"))
        source.upsert(record("b", archived = true))
        source.upsert(record("c", deleted = "2026-07-13T01:00:00Z"))
        val active = source.observeActive().first()
        assertEquals(listOf("a"), active.map { it.id })
        val archived = source.observeArchived().first()
        assertEquals(listOf("b"), archived.map { it.id })
        assertEquals(1, source.countActive())
    }

    @Test
    fun `setArchived and softDelete move records between views`() = runTest {
        val source = FakeVaultSource()
        source.upsert(record("a"))
        source.setArchived("a", archived = true, updatedAt = "2026-07-13T02:00:00Z")
        assertEquals(listOf("a"), source.observeArchived().first().map { it.id })
        source.softDelete("a", deletedAt = "2026-07-13T03:00:00Z")
        assertTrue(source.observeActive().first().isEmpty())
        assertTrue(source.observeArchived().first().isEmpty())
    }

    @Test
    fun `file store writes reads deletes and can force failure`() = runTest {
        val store = FakeVaultFileStore()
        assertTrue(store.write("id", byteArrayOf(1, 2, 3)) is AppResult.Ok)
        assertEquals(listOf<Byte>(1, 2, 3), (store.read("id") as AppResult.Ok).value.toList())
        store.failNextWrite = true
        assertTrue(store.write("id2", byteArrayOf(9)) is AppResult.Err)
        assertTrue(store.deleteAll() is AppResult.Ok)
        assertTrue(store.read("id") is AppResult.Err)
    }

    @Test
    fun `image codec round-trips pixels and forces alpha opaque`() = runTest {
        val codec = FakeImageCodec()
        val original = PixelImage(width = 2, height = 1, pixels = intArrayOf(0x00112233, 0x44556677))
        val encoded = (codec.encodePng(original) as AppResult.Ok).value
        val decoded = (codec.decode(encoded) as AppResult.Ok).value
        assertEquals(2, decoded.width)
        assertEquals(1, decoded.height)
        assertEquals(0xFF112233.toInt(), decoded.pixels[0])
        assertEquals(0xFF556677.toInt(), decoded.pixels[1])
    }
}
```

- [ ] **Step 2: Run — expect RED (compile failure).**

```bash
./gradlew :shared:test-support:jvmTest --tests "com.slothiesmooth.nyx.shared.testsupport.FakeSourcesTest"
```

Expected: compilation error — `Unresolved reference 'FakeVaultSource'` (and siblings).

- [ ] **Step 3: Write `FakeVaultSource.kt`:**

```kotlin
package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeVaultSource : VaultSource {
    private val records = MutableStateFlow<List<StegoImageRecord>>(emptyList())

    /** Set to force the next [upsert] to throw — used to test rollback paths. */
    var failNextUpsert: Boolean = false

    private fun isActive(record: StegoImageRecord) = !record.isArchived && record.deletedAt == null
    private fun isArchived(record: StegoImageRecord) = record.isArchived && record.deletedAt == null

    override fun observeActive(): Flow<List<StegoImageRecord>> = records.map { list -> list.filter(::isActive) }
    override fun observeArchived(): Flow<List<StegoImageRecord>> = records.map { list -> list.filter(::isArchived) }

    override suspend fun getById(id: String): StegoImageRecord? = records.value.firstOrNull { it.id == id }

    override suspend fun upsert(record: StegoImageRecord) {
        if (failNextUpsert) {
            failNextUpsert = false
            error("forced upsert failure")
        }
        records.value = records.value.filterNot { it.id == record.id } + record
    }

    override suspend fun setArchived(id: String, archived: Boolean, updatedAt: String) {
        records.value = records.value.map { if (it.id == id) it.copy(isArchived = archived, updatedAt = updatedAt) else it }
    }

    override suspend fun softDelete(id: String, deletedAt: String) {
        records.value = records.value.map { if (it.id == id) it.copy(deletedAt = deletedAt) else it }
    }

    override suspend fun purgeAll() { records.value = emptyList() }

    override suspend fun countActive(): Int = records.value.count(::isActive)
}
```

- [ ] **Step 4: Write `FakeVaultFileStore.kt`:**

```kotlin
package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore

class FakeVaultFileStore : VaultFileStore {
    private val files = mutableMapOf<String, ByteArray>()
    var failNextWrite: Boolean = false

    override suspend fun write(id: String, bytes: ByteArray): AppResult<Unit> {
        if (failNextWrite) {
            failNextWrite = false
            return AppResult.Err(AppError.Storage("forced write failure"))
        }
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

- [ ] **Step 5: Write `FakeShareSource.kt`:**

```kotlin
package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.source.ShareSource

class FakeShareSource(var result: AppResult<Unit> = AppResult.Ok(Unit)) : ShareSource {
    var lastShared: Pair<ByteArray, String>? = null

    override suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit> {
        lastShared = bytes to fileName
        return result
    }
}
```

- [ ] **Step 6: Write `FakeCameraSource.kt`:**

```kotlin
package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.PickedImage

class FakeCameraSource(override val isAvailable: Boolean = true) : CameraSource {
    var next: PickedImage? = null

    override suspend fun capture(): PickedImage? = next
}
```

- [ ] **Step 7: Write `FakeImageCodec.kt`** (lossless pixel round-trip so real stego survives in `commonTest`; forces alpha opaque per the `ImageCodec` contract):

```kotlin
package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.steganography.PixelImage

private const val HEADER_INTS = 2
private const val BYTES_PER_INT = 4
private const val OPAQUE_ALPHA = 0xFF000000.toInt()
private const val BYTE_MASK = 0xFF

class FakeImageCodec : ImageCodec {

    override suspend fun decode(bytes: ByteArray): AppResult<PixelImage> {
        val ints = IntArray(bytes.size / BYTES_PER_INT) { index -> readInt(bytes, index * BYTES_PER_INT) }
        val width = ints[0]
        val height = ints[1]
        val pixels = IntArray(width * height) { index -> ints[HEADER_INTS + index] or OPAQUE_ALPHA }
        return AppResult.Ok(PixelImage(width = width, height = height, pixels = pixels))
    }

    override suspend fun encodePng(image: PixelImage): AppResult<ByteArray> {
        val ints = IntArray(HEADER_INTS + image.pixels.size)
        ints[0] = image.width
        ints[1] = image.height
        image.pixels.copyInto(ints, destinationOffset = HEADER_INTS)
        val out = ByteArray(ints.size * BYTES_PER_INT)
        ints.forEachIndexed { index, value -> writeInt(out, index * BYTES_PER_INT, value) }
        return AppResult.Ok(out)
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and BYTE_MASK) or
            ((bytes[offset + 1].toInt() and BYTE_MASK) shl 8) or
            ((bytes[offset + 2].toInt() and BYTE_MASK) shl 16) or
            ((bytes[offset + 3].toInt() and BYTE_MASK) shl 24)

    private fun writeInt(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and BYTE_MASK).toByte()
        bytes[offset + 1] = ((value shr 8) and BYTE_MASK).toByte()
        bytes[offset + 2] = ((value shr 16) and BYTE_MASK).toByte()
        bytes[offset + 3] = ((value shr 24) and BYTE_MASK).toByte()
    }
}
```

- [ ] **Step 8: Run — expect GREEN.**

```bash
./gradlew :shared:test-support:jvmTest --tests "com.slothiesmooth.nyx.shared.testsupport.FakeSourcesTest"
```

Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 9: Commit.**

```bash
git add shared/test-support/src
git commit -m "feat(test-support): in-memory fakes for vault/file/share/camera sources and image codec"
```

---

### Task 3: Vault feature — domain models, repository, use cases (`:feature:vault:client:basic`)

**Files:**
- Modify: `feature/vault/client/basic/build.gradle.kts` (dependencies — see Step 1)
- Create: `.../basic/domain/model/VaultImage.kt`
- Create: `.../basic/domain/VaultRepository.kt`
- Create: `.../basic/data/VaultRepositoryImpl.kt`
- Create: `.../basic/domain/usecase/VaultUseCases.kt` (7 use cases in one file — each is a one-liner delegating to the repository; grouping avoids seven near-empty files, DRY)
- Test: `.../basic/src/commonTest/kotlin/.../basic/data/VaultRepositoryImplTest.kt`

All source paths below are under `feature/vault/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/vault/basic/`.

**Interfaces:**
- Consumes: `:shared:data` (`AppResult`, `AppError`, `StegoImageId`, `Clock`, `DomainEventBus`, `DomainEvent`, `VaultSource`, `VaultFileStore`, `StegoImageRecord`, `ShareSource`), kotlin.time.Instant, kotlinx-collections-immutable.
- Produces (00-INDEX vault.basic.domain contract):
  - `data class VaultImage(val id: StegoImageId, val name: String, val createdAt: Instant, val isArchived: Boolean)`
  - `interface VaultRepository { observeActive; observeArchived; imageBytes; archive; restore; softDelete }` (exact signatures below)
  - `class VaultRepositoryImpl(vaultSource, fileStore, clock, eventBus) : VaultRepository`
  - Use cases: `ObserveVaultImagesUseCase`, `ObserveArchivedImagesUseCase`, `GetImageBytesUseCase`, `ArchiveImageUseCase`, `RestoreImageUseCase`, `DeleteImageUseCase`, `ShareVaultImageUseCase`.

**Steps:**

- [ ] **Step 1: Set the module dependencies.** Ensure `feature/vault/client/basic/build.gradle.kts` `commonMain.dependencies` includes the feature-specific deps beyond whatever the `nyx.feature.basic` convention plugin already wires (own `api`, `:feature:common:client:{api,koin}`, `:shared:data`, `:shared:presentation`, `:shared:design-library`). Add the cross-feature route deps and test deps:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.encrypt.client.api)   // EncryptRoute (empty-state CTA)
            implementation(projects.feature.decrypt.client.api)   // DecryptRoute (detail: decrypt this)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(projects.shared.testSupport)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

Verify: `./gradlew :feature:vault:client:basic:compileKotlinJvm` → `BUILD SUCCESSFUL` (empty module still compiles).

- [ ] **Step 2: Write the failing repository test.** Create `VaultRepositoryImplTest.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.data

import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.DomainEvent
import com.slothiesmooth.nyx.shared.data.StegoImageId
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.testsupport.FakeClock
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class VaultRepositoryImplTest {

    private val fixed = Instant.parse("2026-07-13T12:00:00Z")

    private fun repo(source: FakeVaultSource, store: FakeVaultFileStore, bus: DefaultDomainEventBus) =
        VaultRepositoryImpl(vaultSource = source, fileStore = store, clock = FakeClock(fixed), eventBus = bus)

    private fun record(id: String, createdAt: String = "2026-07-10T09:00:00Z") =
        StegoImageRecord(id = id, name = "nyx-$id.png", createdAt = createdAt, updatedAt = createdAt, deletedAt = null, isArchived = false)

    @Test
    fun `observeActive maps records to domain and parses ISO instant`() = runTest {
        val source = FakeVaultSource().apply { upsert(record("a")) }
        val images = repo(source, FakeVaultFileStore(), DefaultDomainEventBus()).observeActive().first()
        assertEquals(1, images.size)
        assertEquals(StegoImageId("a"), images[0].id)
        assertEquals("nyx-a.png", images[0].name)
        assertEquals(Instant.parse("2026-07-10T09:00:00Z"), images[0].createdAt)
        assertTrue(!images[0].isArchived)
    }

    @Test
    fun `imageBytes reads from the file store`() = runTest {
        val store = FakeVaultFileStore().apply { write("a", byteArrayOf(7, 8, 9)) }
        val result = repo(FakeVaultSource(), store, DefaultDomainEventBus()).imageBytes(StegoImageId("a"))
        assertTrue(result is AppResult.Ok)
        assertEquals(listOf<Byte>(7, 8, 9), result.value.toList())
    }

    @Test
    fun `archive stamps updatedAt from clock and emits StegoImageArchived`() = runTest {
        val source = FakeVaultSource().apply { upsert(record("a")) }
        val bus = DefaultDomainEventBus()
        val events = mutableListOf<DomainEvent>()
        val collector = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val job = collector.launch { bus.events.collect { events.add(it) } }
        val result = repo(source, FakeVaultFileStore(), bus).archive(StegoImageId("a"))
        assertTrue(result is AppResult.Ok)
        assertTrue(source.observeArchived().first().any { it.id == "a" && it.updatedAt == fixed.toString() })
        assertEquals(DomainEvent.StegoImageArchived(StegoImageId("a")), events.single())
        job.cancel()
    }

    @Test
    fun `restore un-archives and emits StegoImageRestored`() = runTest {
        val source = FakeVaultSource().apply { upsert(record("a")); setArchived("a", true, "2026-07-11T00:00:00Z") }
        val result = repo(source, FakeVaultFileStore(), DefaultDomainEventBus()).restore(StegoImageId("a"))
        assertTrue(result is AppResult.Ok)
        assertTrue(source.observeActive().first().any { it.id == "a" })
    }

    @Test
    fun `softDelete tombstones the row and emits StegoImageDeleted`() = runTest {
        val source = FakeVaultSource().apply { upsert(record("a")) }
        val result = repo(source, FakeVaultFileStore(), DefaultDomainEventBus()).softDelete(StegoImageId("a"))
        assertTrue(result is AppResult.Ok)
        assertTrue(source.observeActive().first().isEmpty())
    }
}
```

Note: `FakeClock` and `DefaultDomainEventBus` come from `:shared:test-support`/`:shared:data` (plans 03). `kotlinx.coroutines.launch`/`CoroutineScope` need imports — add `import kotlinx.coroutines.launch` at the top when writing (kept implicit here for brevity).

- [ ] **Step 3: Run — expect RED (compile failure).**

```bash
./gradlew :feature:vault:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.vault.basic.data.VaultRepositoryImplTest"
```

Expected: `Unresolved reference 'VaultRepositoryImpl'`.

- [ ] **Step 4: Write `VaultImage.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.domain.model

import com.slothiesmooth.nyx.shared.data.StegoImageId
import kotlin.time.Instant

data class VaultImage(
    val id: StegoImageId,
    val name: String,
    val createdAt: Instant,
    val isArchived: Boolean,
)
```

- [ ] **Step 5: Write `VaultRepository.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.domain

import com.slothiesmooth.nyx.feature.vault.basic.domain.model.VaultImage
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.StegoImageId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    fun observeActive(): Flow<ImmutableList<VaultImage>>
    fun observeArchived(): Flow<ImmutableList<VaultImage>>
    suspend fun imageBytes(id: StegoImageId): AppResult<ByteArray>
    suspend fun archive(id: StegoImageId): AppResult<Unit>
    suspend fun restore(id: StegoImageId): AppResult<Unit>
    suspend fun softDelete(id: StegoImageId): AppResult<Unit>
}
```

- [ ] **Step 6: Write `VaultRepositoryImpl.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.data

import com.slothiesmooth.nyx.feature.vault.basic.domain.VaultRepository
import com.slothiesmooth.nyx.feature.vault.basic.domain.model.VaultImage
import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.Clock
import com.slothiesmooth.nyx.shared.data.DomainEvent
import com.slothiesmooth.nyx.shared.data.DomainEventBus
import com.slothiesmooth.nyx.shared.data.StegoImageId
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

private const val ARCHIVE_FAILED = "Failed to archive image"
private const val RESTORE_FAILED = "Failed to restore image"
private const val DELETE_FAILED = "Failed to delete image"

class VaultRepositoryImpl(
    private val vaultSource: VaultSource,
    private val fileStore: VaultFileStore,
    private val clock: Clock,
    private val eventBus: DomainEventBus,
) : VaultRepository {

    override fun observeActive(): Flow<ImmutableList<VaultImage>> =
        vaultSource.observeActive().map { records -> records.map(::toDomain).toImmutableList() }

    override fun observeArchived(): Flow<ImmutableList<VaultImage>> =
        vaultSource.observeArchived().map { records -> records.map(::toDomain).toImmutableList() }

    override suspend fun imageBytes(id: StegoImageId): AppResult<ByteArray> = fileStore.read(id.value)

    override suspend fun archive(id: StegoImageId): AppResult<Unit> = mutate(ARCHIVE_FAILED) {
        vaultSource.setArchived(id.value, archived = true, updatedAt = clock.now().toString())
        eventBus.emit(DomainEvent.StegoImageArchived(id))
    }

    override suspend fun restore(id: StegoImageId): AppResult<Unit> = mutate(RESTORE_FAILED) {
        vaultSource.setArchived(id.value, archived = false, updatedAt = clock.now().toString())
        eventBus.emit(DomainEvent.StegoImageRestored(id))
    }

    override suspend fun softDelete(id: StegoImageId): AppResult<Unit> = mutate(DELETE_FAILED) {
        vaultSource.softDelete(id.value, deletedAt = clock.now().toString())
        eventBus.emit(DomainEvent.StegoImageDeleted(id))
    }

    private fun toDomain(record: StegoImageRecord) = VaultImage(
        id = StegoImageId(record.id),
        name = record.name,
        createdAt = Instant.parse(record.createdAt),
        isArchived = record.isArchived,
    )

    private suspend inline fun mutate(failureMessage: String, block: () -> Unit): AppResult<Unit> =
        try {
            block()
            AppResult.Ok(Unit)
        } catch (cause: Throwable) {
            AppResult.Err(AppError.Storage(failureMessage, cause))
        }
}
```

- [ ] **Step 7: Write `VaultUseCases.kt`** (each delegates to the repository; `ShareVaultImageUseCase` reads bytes then hands them to `ShareSource` with the caller-supplied file name):

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.domain.usecase

import com.slothiesmooth.nyx.feature.vault.basic.domain.VaultRepository
import com.slothiesmooth.nyx.feature.vault.basic.domain.model.VaultImage
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.StegoImageId
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

class ObserveVaultImagesUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<ImmutableList<VaultImage>> = repository.observeActive()
}

class ObserveArchivedImagesUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<ImmutableList<VaultImage>> = repository.observeArchived()
}

class GetImageBytesUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(id: StegoImageId): AppResult<ByteArray> = repository.imageBytes(id)
}

class ArchiveImageUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(id: StegoImageId): AppResult<Unit> = repository.archive(id)
}

class RestoreImageUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(id: StegoImageId): AppResult<Unit> = repository.restore(id)
}

class DeleteImageUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(id: StegoImageId): AppResult<Unit> = repository.softDelete(id)
}

class ShareVaultImageUseCase(
    private val repository: VaultRepository,
    private val shareSource: ShareSource,
) {
    suspend operator fun invoke(id: StegoImageId, fileName: String): AppResult<Unit> =
        when (val bytes = repository.imageBytes(id)) {
            is AppResult.Ok -> shareSource.shareImage(bytes.value, fileName)
            is AppResult.Err -> bytes
        }
}
```

- [ ] **Step 8: Run — expect GREEN.**

```bash
./gradlew :feature:vault:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.vault.basic.data.VaultRepositoryImplTest"
```

Expected: `BUILD SUCCESSFUL`, 5 tests passed.

- [ ] **Step 9: Commit.**

```bash
git add feature/vault/client/basic/build.gradle.kts feature/vault/client/basic/src
git commit -m "feat(vault): domain model, repository, and use cases with repository tests"
```

---

### Task 4: Vault presentation — list + detail (State/ViewModel/Screen/Content + previews + logic tests)

**Testable-logic strategy (used by every feature in this plan):** the render-ready mapping (bytes→`ImageBitmap`, `Instant`→label) is extracted into pure `suspend`/pure functions the ViewModel calls. Those pure functions are unit-tested in `runTest` over fakes (no `viewModelScope`, no dispatcher games). The ViewModel is thin wiring: `doInit`→`start()` collects flows and pushes mapped results into state; flow-collection wiring is exercised end-to-end by the Compose UI tests (Tasks 8, 11). This keeps VM tests reliable in `commonMain` (no `Thread.sleep`, no `Dispatchers.Default` race) while still covering all logic.

**Files (all under `feature/vault/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/vault/basic/`):**
- Create: `presentation/VaultDateFormatter.kt`
- Create: `presentation/list/VaultState.kt`, `presentation/list/VaultRows.kt`, `presentation/list/VaultViewModel.kt`, `presentation/list/VaultScreen.kt`, `presentation/list/VaultContent.kt`
- Create: `presentation/detail/VaultDetailState.kt`, `presentation/detail/VaultDetailViewModel.kt`, `presentation/detail/VaultDetailScreen.kt`, `presentation/detail/VaultDetailContent.kt`
- Test: `src/commonTest/kotlin/.../basic/presentation/VaultRowsTest.kt`

**Interfaces:**
- Consumes: Task 3 use cases, `:shared:presentation` (`BaseViewModel`, `ViewState`, `MutableViewState`, `UiState`, `UiEvent`, `ByteArray.toImageBitmap()`), `:shared:design-library` components, `koinFeatureViewModel`, kotlinx-datetime.
- Produces: `VaultState`/`VaultViewModel`/`VaultScreen`, `VaultDetailState`/`VaultDetailViewModel`/`VaultDetailScreen`, `VaultImageUi`, `buildVaultRows`, `formatVaultDate`. Consumed by `BasicVaultProvider` (Task 5).

**Steps:**

- [ ] **Step 1: Write the failing logic test.** Create `VaultRowsTest.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.presentation

import com.slothiesmooth.nyx.feature.vault.basic.data.VaultRepositoryImpl
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.GetImageBytesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.presentation.list.buildVaultRows
import com.slothiesmooth.nyx.shared.data.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.testsupport.FakeClock
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class VaultRowsTest {

    @Test
    fun `formatVaultDate renders abbreviated month day and year`() {
        val label = formatVaultDate(Instant.parse("2026-07-13T12:00:00Z"), TimeZone.UTC)
        assertEquals("Jul 13, 2026", label)
    }

    @Test
    fun `buildVaultRows maps every image to a render-ready row and calls decode with its bytes`() = runTest {
        val source = FakeVaultSource().apply {
            upsert(StegoImageRecord("a", "nyx-a.png", "2026-07-13T12:00:00Z", "2026-07-13T12:00:00Z", null, false))
        }
        val store = FakeVaultFileStore().apply { write("a", byteArrayOf(4, 2)) }
        val repo = VaultRepositoryImpl(source, store, FakeClock(Instant.parse("2026-07-13T12:00:00Z")), DefaultDomainEventBus())
        val decodedBytes = mutableListOf<Int>()
        val rows = buildVaultRows(
            images = repo.observeActive().first(),
            getImageBytes = GetImageBytesUseCase(repo),
            zone = TimeZone.UTC,
            decode = { bytes -> decodedBytes.add(bytes.size); null },
        )
        assertEquals(1, rows.size)
        assertEquals("nyx-a.png", rows[0].name)
        assertEquals("Jul 13, 2026", rows[0].createdLabel)
        assertNull(rows[0].thumbnail)
        assertEquals(listOf(2), decodedBytes)
    }
}
```

- [ ] **Step 2: Run — expect RED.**

```bash
./gradlew :feature:vault:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.vault.basic.presentation.VaultRowsTest"
```

Expected: `Unresolved reference 'buildVaultRows'` / `formatVaultDate`.

- [ ] **Step 3: Write `VaultDateFormatter.kt`** (`.day` is the kotlinx-datetime 0.7.x accessor; `.month.name` is the uppercase English enum name — see Open Questions if 0.7.1 differs):

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.presentation

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val MONTH_ABBREVIATION_LENGTH = 3

fun formatVaultDate(instant: Instant, zone: TimeZone): String {
    val dateTime = instant.toLocalDateTime(zone)
    val month = dateTime.month.name.lowercase()
        .replaceFirstChar { it.uppercase() }
        .take(MONTH_ABBREVIATION_LENGTH)
    return "$month ${dateTime.day}, ${dateTime.year}"
}
```

- [ ] **Step 4: Write `VaultState.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.presentation.list

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.shared.data.StegoImageId
import com.slothiesmooth.nyx.shared.presentation.state.ViewState
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class VaultImageUi(
    val id: StegoImageId,
    val name: String,
    val createdLabel: String,
    val thumbnail: ImageBitmap?,
)

@Stable
interface VaultState : ViewState {
    val active: ImmutableList<VaultImageUi>
    val archived: ImmutableList<VaultImageUi>
    val showArchived: Boolean
    val isLoading: Boolean
}
```

Note: `ViewState` is imported from `com.slothiesmooth.nyx.shared.presentation.state` (plan 03 package `...presentation.viewmodel/state/navigation`). If plan 03 placed `ViewState` in a different subpackage, adjust the import (mechanical).

- [ ] **Step 5: Write `VaultRows.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.presentation.list

import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.feature.vault.basic.domain.model.VaultImage
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.GetImageBytesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.presentation.formatVaultDate
import com.slothiesmooth.nyx.shared.data.AppResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone

/** Pure render-ready mapper — unit tested directly; `decode` is injected so tests avoid skiko. */
suspend fun buildVaultRows(
    images: ImmutableList<VaultImage>,
    getImageBytes: GetImageBytesUseCase,
    zone: TimeZone,
    decode: (ByteArray) -> ImageBitmap?,
): ImmutableList<VaultImageUi> = images.map { image ->
    val thumbnail = when (val bytes = getImageBytes(image.id)) {
        is AppResult.Ok -> decode(bytes.value)
        is AppResult.Err -> null
    }
    VaultImageUi(
        id = image.id,
        name = image.name,
        createdLabel = formatVaultDate(image.createdAt, zone),
        thumbnail = thumbnail,
    )
}.toImmutableList()
```

- [ ] **Step 6: Write `VaultViewModel.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.presentation.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ArchiveImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.DeleteImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.GetImageBytesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveArchivedImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveVaultImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.RestoreImageUseCase
import com.slothiesmooth.nyx.shared.data.Clock
import com.slothiesmooth.nyx.shared.data.StegoImageId
import com.slothiesmooth.nyx.shared.presentation.image.toImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private class VaultMutableState : MutableViewState(), VaultState {
    override var active: ImmutableList<VaultImageUi> by mutableStateOf(persistentListOf())
    override var archived: ImmutableList<VaultImageUi> by mutableStateOf(persistentListOf())
    override var showArchived: Boolean by mutableStateOf(false)
    override var isLoading: Boolean by mutableStateOf(true)
}

class VaultViewModel(
    private val observeActive: ObserveVaultImagesUseCase,
    private val observeArchived: ObserveArchivedImagesUseCase,
    private val getImageBytes: GetImageBytesUseCase,
    private val archiveImage: ArchiveImageUseCase,
    private val restoreImage: RestoreImageUseCase,
    private val deleteImage: DeleteImageUseCase,
    private val clock: Clock,
) : BaseViewModel() {

    private val mutableState = VaultMutableState()
    val state: VaultState get() = mutableState

    private val decode: (ByteArray) -> ImageBitmap? = { bytes -> runCatching { bytes.toImageBitmap() }.getOrNull() }

    override fun doInit() = start()

    /** Idempotent (async ids dedup) — also the entry point unit tests use. */
    fun start() {
        async("observe-active") {
            observeActive().collect { images ->
                val rows = buildVaultRows(images, getImageBytes, clock.zone(), decode)
                withState {
                    mutableState.active = rows
                    mutableState.isLoading = false
                }
            }
        }
        async("observe-archived") {
            observeArchived().collect { images ->
                val rows = buildVaultRows(images, getImageBytes, clock.zone(), decode)
                withState { mutableState.archived = rows }
            }
        }
    }

    fun toggleArchived() = withState { mutableState.showArchived = !mutableState.showArchived }
    fun archive(id: StegoImageId) { async("archive-${id.value}") { archiveImage(id) } }
    fun restore(id: StegoImageId) { async("restore-${id.value}") { restoreImage(id) } }
    fun delete(id: StegoImageId) { async("delete-${id.value}") { deleteImage(id) } }
}
```

Note imports `...presentation.state.MutableViewState` and `...presentation.viewmodel.BaseViewModel` — plan 03 package layout (`...presentation.viewmodel/state/navigation`). Adjust if plan 03 differs (mechanical).

- [ ] **Step 7: Write `VaultScreen.kt` (thin):**

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.presentation.list

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import com.slothiesmooth.nyx.shared.data.StegoImageId

@Composable
fun VaultScreen(
    onOpenDetail: (StegoImageId) -> Unit,
    onOpenEncrypt: () -> Unit,
) {
    val viewModel = koinFeatureViewModel<VaultViewModel>()
    VaultContent(
        state = viewModel.state,
        onOpenDetail = onOpenDetail,
        onOpenEncrypt = onOpenEncrypt,
        onToggleArchived = viewModel::toggleArchived,
        onOpenArchivedDetail = onOpenDetail,
    )
}
```

- [ ] **Step 8: Write `VaultContent.kt`** (dumb; grid + empty state + archived section + encrypt FAB) with previews:

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyState
import com.slothiesmooth.nyx.designlibrary.molecules.NxFab
import com.slothiesmooth.nyx.designlibrary.molecules.NxImageTile
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlay
import com.slothiesmooth.nyx.designlibrary.molecules.NxSectionHeader
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconButtonStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.shared.data.StegoImageId
import com.slothiesmooth.nyx.shared.presentation.state.UiEvent
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import com.slothiesmooth.nyx.shared.presentation.state.ViewState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val GRID_COLUMNS = 2
private val GridGap = 12.dp
private val GridPadding = 16.dp
private val FabPadding = 20.dp

@Composable
fun VaultContent(
    state: VaultState,
    onOpenDetail: (StegoImageId) -> Unit,
    onOpenEncrypt: () -> Unit,
    onToggleArchived: () -> Unit,
    onOpenArchivedDetail: (StegoImageId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(NxTokens.colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            NxTopBar(
                title = "Vault",
                trailing = {
                    NxIconButton(
                        kind = if (state.showArchived) NxIconKind.Eye else NxIconKind.Archive,
                        onClick = onToggleArchived,
                        style = NxIconButtonStyle.Ghost,
                        contentDescription = "Toggle archived",
                    )
                },
            )
            when {
                state.isLoading -> NxProgressOverlay(label = "Loading vault", modifier = Modifier.fillMaxSize())
                state.active.isEmpty() && !state.showArchived -> NxEmptyState(
                    icon = NxIconKind.Vault,
                    title = "Your vault is empty",
                    body = "Hide an encrypted message inside an image to get started.",
                    ctaText = "Encrypt a message",
                    onCta = onOpenEncrypt,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(GRID_COLUMNS),
                    contentPadding = PaddingValues(GridPadding),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(GridGap),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(GridGap),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.active, key = { it.id.value }) { row ->
                        NxImageTile(image = row.thumbnail, contentDescription = row.name, onClick = { onOpenDetail(row.id) })
                    }
                    if (state.showArchived && state.archived.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { NxSectionHeader(title = "Archived") }
                        items(state.archived, key = { "archived-${it.id.value}" }) { row ->
                            NxImageTile(image = row.thumbnail, contentDescription = row.name, onClick = { onOpenArchivedDetail(row.id) })
                        }
                    }
                }
            }
        }
        if (!state.isLoading && !(state.active.isEmpty() && !state.showArchived)) {
            NxFab(
                icon = NxIconKind.Plus,
                onClick = onOpenEncrypt,
                contentDescription = "Encrypt a message",
                modifier = Modifier.align(Alignment.BottomEnd).padding(FabPadding),
            )
        }
    }
}

private class PreviewVaultState(
    override val active: ImmutableList<VaultImageUi>,
    override val archived: ImmutableList<VaultImageUi>,
    override val showArchived: Boolean,
    override val isLoading: Boolean,
) : VaultState {
    override val uiState: UiState = UiState.Ready
    override val uiEvent: Flow<UiEvent> = emptyFlow()
}

private fun sampleRows(count: Int): ImmutableList<VaultImageUi> =
    (1..count).map { VaultImageUi(StegoImageId("id-$it"), "nyx-000$it.png", "Jul 13, 2026", null) }.toImmutableList()

@AllThemePreview
@Composable
private fun VaultLoadingPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    VaultContent(PreviewVaultState(persistentListOf(), persistentListOf(), false, true), {}, {}, {}, {})
}

@AllThemePreview
@Composable
private fun VaultEmptyPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    VaultContent(PreviewVaultState(persistentListOf(), persistentListOf(), false, false), {}, {}, {}, {})
}

@AllThemePreview
@Composable
private fun VaultContentPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    VaultContent(PreviewVaultState(sampleRows(4), sampleRows(2), true, false), {}, {}, {}, {})
}
```

Note the design-library component packages (`atoms`/`molecules`) follow plan 04's atomic layout; if a component lands in a different subpackage, fix the import (mechanical). `NxImageTile`, `NxEmptyState`, `NxProgressOverlay`, `NxSectionHeader`, `NxTopBar`, `NxFab` are the pinned molecule signatures from the contracts section.

- [ ] **Step 9: Run the logic test — expect GREEN + compile.**

```bash
./gradlew :feature:vault:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.vault.basic.presentation.VaultRowsTest"
```

Expected: `BUILD SUCCESSFUL`, 2 tests passed (this also compiles the list Screen/Content/VM).

- [ ] **Step 10: Commit the list screen.**

```bash
git add feature/vault/client/basic/src
git commit -m "feat(vault): list screen, view model, render-ready row mapper with logic tests"
```

- [ ] **Step 11: Write `VaultDetailState.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.presentation.detail

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.UiEvent
import com.slothiesmooth.nyx.shared.presentation.state.ViewState

@Stable
interface VaultDetailState : ViewState {
    val name: String
    val createdLabel: String
    val thumbnail: ImageBitmap?
    val isArchived: Boolean
    val isLoading: Boolean
}

sealed interface VaultDetailUiEvent : UiEvent {
    /** The image no longer exists (deleted here or elsewhere) — the screen should pop. */
    data object Closed : VaultDetailUiEvent
}
```

- [ ] **Step 12: Write `VaultDetailViewModel.kt`** (observes both lists, finds the target id, maps to state; `delete` emits `Closed`):

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.presentation.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ArchiveImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.DeleteImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.GetImageBytesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveArchivedImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveVaultImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.RestoreImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ShareVaultImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.presentation.formatVaultDate
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.Clock
import com.slothiesmooth.nyx.shared.data.StegoImageId
import com.slothiesmooth.nyx.shared.presentation.image.toImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.combine

private class VaultDetailMutableState : MutableViewState(), VaultDetailState {
    override var name: String by mutableStateOf("")
    override var createdLabel: String by mutableStateOf("")
    override var thumbnail: ImageBitmap? by mutableStateOf(null)
    override var isArchived: Boolean by mutableStateOf(false)
    override var isLoading: Boolean by mutableStateOf(true)
}

class VaultDetailViewModel(
    private val observeActive: ObserveVaultImagesUseCase,
    private val observeArchived: ObserveArchivedImagesUseCase,
    private val getImageBytes: GetImageBytesUseCase,
    private val archiveImage: ArchiveImageUseCase,
    private val restoreImage: RestoreImageUseCase,
    private val deleteImage: DeleteImageUseCase,
    private val shareImage: ShareVaultImageUseCase,
    private val clock: Clock,
) : BaseViewModel() {

    private val mutableState = VaultDetailMutableState()
    val state: VaultDetailState get() = mutableState
    private var currentId: StegoImageId? = null

    fun load(imageId: StegoImageId) {
        if (currentId == imageId) return
        currentId = imageId
        async("observe-detail", force = true) {
            combine(observeActive(), observeArchived()) { active, archived ->
                (active + archived).firstOrNull { it.id == imageId }
            }.collect { image ->
                if (image == null) {
                    mutableState.notify(VaultDetailUiEvent.Closed)
                } else {
                    val bytes = getImageBytes(image.id)
                    val thumb = if (bytes is AppResult.Ok) runCatching { bytes.value.toImageBitmap() }.getOrNull() else null
                    withState {
                        mutableState.name = image.name
                        mutableState.createdLabel = formatVaultDate(image.createdAt, clock.zone())
                        mutableState.isArchived = image.isArchived
                        mutableState.thumbnail = thumb
                        mutableState.isLoading = false
                    }
                }
            }
        }
    }

    fun archive() { val id = currentId ?: return; async("archive") { archiveImage(id) } }
    fun restore() { val id = currentId ?: return; async("restore") { restoreImage(id) } }
    fun share() { val id = currentId ?: return; async("share") { shareImage(id, mutableState.name) } }
    fun delete() {
        val id = currentId ?: return
        async("delete") { if (deleteImage(id) is AppResult.Ok) mutableState.notify(VaultDetailUiEvent.Closed) }
    }
}
```

- [ ] **Step 13: Write `VaultDetailScreen.kt` and `VaultDetailContent.kt`.**

`VaultDetailScreen.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import com.slothiesmooth.nyx.shared.data.StegoImageId

@Composable
fun VaultDetailScreen(
    imageId: String,
    onBack: () -> Unit,
    onDecryptThis: (String) -> Unit,
) {
    val viewModel = koinFeatureViewModel<VaultDetailViewModel>()
    LaunchedEffect(imageId) { viewModel.load(StegoImageId(imageId)) }
    LaunchedEffect(viewModel) {
        viewModel.state.uiEvent.collect { event -> if (event is VaultDetailUiEvent.Closed) onBack() }
    }
    VaultDetailContent(
        state = viewModel.state,
        onBack = onBack,
        onArchive = viewModel::archive,
        onRestore = viewModel::restore,
        onDelete = viewModel::delete,
        onShare = viewModel::share,
        onDecrypt = { onDecryptThis(imageId) },
    )
}
```

`VaultDetailContent.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxImageTile
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlay
import com.slothiesmooth.nyx.designlibrary.templates.NxDetailTemplate
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import com.slothiesmooth.nyx.shared.presentation.state.UiEvent
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private val SectionGap = 16.dp

@Composable
fun VaultDetailContent(
    state: VaultDetailState,
    onBack: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onDecrypt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NxDetailTemplate(
        title = state.name.ifBlank { "Image" },
        onBack = onBack,
        modifier = modifier,
        trailing = { NxIconButton(kind = NxIconKind.Share, onClick = onShare, contentDescription = "Share") },
    ) {
        if (state.isLoading) {
            NxProgressOverlay(label = "Loading")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
                NxImageTile(image = state.thumbnail, contentDescription = state.name, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                NxText(text = state.createdLabel, style = NxTextStyle.Caption, color = NxTokens.colors.fgMuted)
                NxButton(text = "Decrypt this", onClick = onDecrypt, style = NxButtonStyle.Primary, block = true, leadingIcon = NxIconKind.Unlock)
                if (state.isArchived) {
                    NxButton(text = "Restore", onClick = onRestore, style = NxButtonStyle.Soft, block = true, leadingIcon = NxIconKind.Restore)
                } else {
                    NxButton(text = "Archive", onClick = onArchive, style = NxButtonStyle.Soft, block = true, leadingIcon = NxIconKind.Archive)
                }
                NxButton(text = "Delete", onClick = onDelete, style = NxButtonStyle.Danger, block = true, leadingIcon = NxIconKind.Trash)
            }
        }
    }
}

private class PreviewDetailState(
    override val name: String,
    override val createdLabel: String,
    override val isArchived: Boolean,
    override val isLoading: Boolean,
) : VaultDetailState {
    override val thumbnail: androidx.compose.ui.graphics.ImageBitmap? = null
    override val uiState: UiState = UiState.Ready
    override val uiEvent: Flow<UiEvent> = emptyFlow()
}

@AllThemePreview
@Composable
private fun VaultDetailContentPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    VaultDetailContent(PreviewDetailState("nyx-0001.png", "Jul 13, 2026", false, false), {}, {}, {}, {}, {}, {})
}

@AllThemePreview
@Composable
private fun VaultDetailArchivedPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    VaultDetailContent(PreviewDetailState("nyx-0002.png", "Jul 10, 2026", true, false), {}, {}, {}, {}, {}, {})
}
```

`NxDetailTemplate` is the pinned template (from contracts). If plan 04 places templates in a `templates` subpackage under a different name, adjust the import.

- [ ] **Step 14: Verify + commit.**

```bash
./gradlew :feature:vault:client:basic:compileKotlinJvm
git add feature/vault/client/basic/src
git commit -m "feat(vault): detail screen with share/archive/restore/delete/decrypt actions"
```

---

### Task 5: `BasicVaultProvider` + DI registration

**Files:**
- Create: `feature/vault/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/vault/basic/BasicVaultProvider.kt`
- Modify: `client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/app/AppConfig.kt` (swap the vault stub for the real provider)

**Interfaces:**
- Consumes: `KoinFeatureProvider`, `FeatureContext`, `withDI`, `koinFeatureViewModel` (plan 05); `VaultFeature`, `VaultRoute`, `VaultDetailRoute` (vault.api); `EncryptRoute` (encrypt.api), `DecryptRoute` (decrypt.api); Task 3-4 types; `NavGraphBuilder`/`composable`/`toRoute` (navigation-compose 2.9.2); Koin module DSL.
- Produces: `class BasicVaultProvider(vaultSource, fileStore, clock, eventBus, shareSource) : KoinFeatureProvider(), VaultFeature`.

**Steps:**

- [ ] **Step 1: Write `BasicVaultProvider.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.vault.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptRoute
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptRoute
import com.slothiesmooth.nyx.feature.vault.api.VaultDetailRoute
import com.slothiesmooth.nyx.feature.vault.api.VaultFeature
import com.slothiesmooth.nyx.feature.vault.api.VaultRoute
import com.slothiesmooth.nyx.feature.vault.basic.data.VaultRepositoryImpl
import com.slothiesmooth.nyx.feature.vault.basic.domain.VaultRepository
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ArchiveImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.DeleteImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.GetImageBytesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveArchivedImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveVaultImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.RestoreImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ShareVaultImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.presentation.detail.VaultDetailScreen
import com.slothiesmooth.nyx.feature.vault.basic.presentation.detail.VaultDetailViewModel
import com.slothiesmooth.nyx.feature.vault.basic.presentation.list.VaultScreen
import com.slothiesmooth.nyx.feature.vault.basic.presentation.list.VaultViewModel
import com.slothiesmooth.nyx.shared.data.Clock
import com.slothiesmooth.nyx.shared.data.DomainEventBus
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

class BasicVaultProvider(
    private val vaultSource: VaultSource,
    private val fileStore: VaultFileStore,
    private val clock: Clock,
    private val eventBus: DomainEventBus,
    private val shareSource: ShareSource,
) : KoinFeatureProvider(), VaultFeature {

    override fun observeActiveCount(): Flow<Int> = vaultSource.observeActive().map { it.size }

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun Module.onProvideDI() {
        single { vaultSource }
        single { fileStore }
        single { clock }
        single { eventBus }
        single { shareSource }
        single<VaultRepository> { VaultRepositoryImpl(get(), get(), get(), get()) }
        factoryOf(::ObserveVaultImagesUseCase)
        factoryOf(::ObserveArchivedImagesUseCase)
        factoryOf(::GetImageBytesUseCase)
        factoryOf(::ArchiveImageUseCase)
        factoryOf(::RestoreImageUseCase)
        factoryOf(::DeleteImageUseCase)
        factoryOf(::ShareVaultImageUseCase)
        viewModelOf(::VaultViewModel)
        viewModelOf(::VaultDetailViewModel)
    }

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<VaultRoute> {
            withDI {
                VaultScreen(
                    onOpenDetail = { id -> context.pushDestination(VaultDetailRoute(id.value)) },
                    onOpenEncrypt = { context.pushDestination(EncryptRoute) },
                )
            }
        }
        builder.composable<VaultDetailRoute> { entry ->
            val route = entry.toRoute<VaultDetailRoute>()
            withDI {
                VaultDetailScreen(
                    imageId = route.imageId,
                    onBack = { context.popDestination() },
                    onDecryptThis = { imageId -> context.pushDestination(DecryptRoute(imageId)) },
                )
            }
        }
    }
}
```

- [ ] **Step 2: Swap the vault registration in `client/.../app/AppConfig.kt`.** In `fun appModule(platformModule: Module): Module = module { ... }` (defined by plan 05), replace the vault stub line with the real provider (the surrounding module — engines, sources, clock, id, bus, capabilities, the ordered `single<List<Feature>>` — is plan 05's and unchanged):

```kotlin
    // was: single<VaultFeature> { StubVaultFeature() }
    single<com.slothiesmooth.nyx.feature.vault.api.VaultFeature> {
        com.slothiesmooth.nyx.feature.vault.basic.BasicVaultProvider(
            vaultSource = get(),
            fileStore = get(),
            clock = get(),
            eventBus = get(),
            shareSource = get(),
        )
    }
```

(If plan 05 already imports these types at the top of `AppConfig.kt`, use the short names instead of fully-qualified — either compiles.)

- [ ] **Step 3: Verify `:client` compiles with the real provider.**

```bash
./gradlew :client:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. (`VaultSource`, `VaultFileStore`, `Clock`, `DomainEventBus`, `ShareSource` all resolve from the outer module registered by plan 05 + the platform module.)

- [ ] **Step 4: Commit.**

```bash
git add feature/vault/client/basic/src client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/app/AppConfig.kt
git commit -m "feat(vault): BasicVaultProvider with isolated DI and navigation, wired into appModule"
```

---

### Task 6: Encrypt feature — use cases + capacity estimate (`:feature:encrypt:client:basic`)

**Files:**
- Modify: `feature/encrypt/client/basic/build.gradle.kts` (deps)
- Create (under `feature/encrypt/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/encrypt/basic/`): `domain/EncryptCapacity.kt`, `domain/usecase/EncryptMessageUseCase.kt`, `domain/usecase/SaveToVaultUseCase.kt`
- Test: `src/commonTest/kotlin/.../basic/domain/EncryptDomainTest.kt`

**Interfaces:**
- Consumes: `:crypto` (`NyxCrypto`), `:steganography` (`Steganography`, `PixelImage`, `StegoEncodeResult`), `:shared:data` (`ImageCodec`, `AppResult`, `AppError`, `VaultSource`, `VaultFileStore`, `IdGenerator`, `Clock`, `DomainEventBus`, `DomainEvent`, `StegoImageId`, `StegoImageRecord`).
- Produces (00-INDEX encrypt.basic.domain contract):
  - `class EncryptMessageUseCase(crypto, stego, codec)` → `suspend operator fun invoke(coverBytes, message, password): AppResult<ByteArray>`
  - `class SaveToVaultUseCase(vaultSource, fileStore, idGenerator, clock, eventBus)` → `suspend operator fun invoke(pngBytes, name): AppResult<StegoImageId>`
  - `fun estimateMaxMessageChars(width: Int, height: Int): Int`

**Steps:**

- [ ] **Step 1: Set module deps.** In `feature/encrypt/client/basic/build.gradle.kts` add (beyond convention-plugin defaults):

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.crypto)
            implementation(projects.steganography)
            implementation(libs.filekit.dialogs.compose)   // image picker in the Screen
            implementation(libs.kotlinx.collections.immutable)
        }
        commonTest.dependencies {
            implementation(projects.crypto)
            implementation(projects.steganography)
            implementation(projects.shared.testSupport)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

Verify: `./gradlew :feature:encrypt:client:basic:compileKotlinJvm` → `BUILD SUCCESSFUL`.

- [ ] **Step 2: Write the failing test.** Create `EncryptDomainTest.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic.domain

import com.slothiesmooth.nyx.crypto.DecryptResult
import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.DomainEvent
import com.slothiesmooth.nyx.shared.data.IdGenerator
import com.slothiesmooth.nyx.shared.testsupport.FakeClock
import com.slothiesmooth.nyx.shared.testsupport.FakeImageCodec
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import com.slothiesmooth.nyx.steganography.PixelImage
import com.slothiesmooth.nyx.steganography.Steganography
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

private const val OPAQUE = 0xFF000000.toInt()

class EncryptDomainTest {

    private val codec = FakeImageCodec()
    private val crypto = DefaultNyxCrypto()
    private val stego = Steganography()

    private suspend fun coverBytes(width: Int, height: Int): ByteArray {
        val pixels = IntArray(width * height) { OPAQUE or (it and 0x00FFFFFF) }
        return (codec.encodePng(PixelImage(width, height, pixels)) as AppResult.Ok).value
    }

    @Test
    fun `encrypt then manual stego+crypto decode recovers the message`() = runTest {
        val useCase = EncryptMessageUseCase(crypto, stego, codec)
        val png = useCase(coverBytes(96, 96), "meet me at dawn", "hunter2")
        assertTrue(png is AppResult.Ok)
        val decoded = (codec.decode(png.value) as AppResult.Ok).value
        val blob = stego.decode(listOf(decoded))!!
        val result = crypto.decrypt(blob, "hunter2")
        assertEquals(DecryptResult.Success("meet me at dawn"), result)
    }

    @Test
    fun `encrypt reports a validation error when the message exceeds capacity`() = runTest {
        val useCase = EncryptMessageUseCase(crypto, stego, codec)
        val result = useCase(coverBytes(4, 4), "x".repeat(500), "pw")
        assertTrue(result is AppResult.Err)
        assertTrue(result.cause is AppError.Validation)
        assertTrue((result.cause as AppError.Validation).message.contains("too large"))
    }

    @Test
    fun `estimateMaxMessageChars grows with image area and is non-negative`() {
        assertEquals(0, estimateMaxMessageChars(2, 2))
        assertTrue(estimateMaxMessageChars(256, 256) > estimateMaxMessageChars(64, 64))
    }

    @Test
    fun `save writes the file, upserts an auto-named record, and emits StegoImageStored`() = runTest {
        val source = FakeVaultSource()
        val store = FakeVaultFileStore()
        val bus = DefaultDomainEventBus()
        val idGen = object : IdGenerator { override fun newId() = "abcdef1234567890" }
        val useCase = SaveToVaultUseCase(source, store, idGen, FakeClock(Instant.parse("2026-07-13T12:00:00Z")), bus)
        val result = useCase(byteArrayOf(1, 2, 3), name = "")
        assertTrue(result is AppResult.Ok)
        assertEquals("abcdef1234567890", result.value.value)
        val record = source.observeActive().first().single()
        assertEquals("nyx-abcdef12.png", record.name)
        assertTrue(store.read("abcdef1234567890") is AppResult.Ok)
    }

    @Test
    fun `save rolls the file back and errors when the row insert fails`() = runTest {
        val source = FakeVaultSource().apply { failNextUpsert = true }
        val store = FakeVaultFileStore()
        val idGen = object : IdGenerator { override fun newId() = "id0000000000" }
        val useCase = SaveToVaultUseCase(source, store, idGen, FakeClock(Instant.parse("2026-07-13T12:00:00Z")), DefaultDomainEventBus())
        val result = useCase(byteArrayOf(9), name = "")
        assertTrue(result is AppResult.Err)
        assertTrue(store.read("id0000000000") is AppResult.Err)
    }
}
```

- [ ] **Step 3: Run — expect RED.**

```bash
./gradlew :feature:encrypt:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.encrypt.basic.domain.EncryptDomainTest"
```

Expected: `Unresolved reference 'EncryptMessageUseCase'`.

- [ ] **Step 4: Write `EncryptCapacity.kt`** (estimate mirrors the `:steganography` 2-bit-LSB-across-RGB scheme; the real encode stays authoritative — see Open Questions on bits-per-pixel):

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic.domain

private const val USABLE_BITS_PER_PIXEL = 6      // 2 LSBs x R,G,B
private const val BITS_PER_BYTE = 8
private const val MARKER_OVERHEAD_BYTES = 6      // start + end 3-char markers
private const val CRYPTO_OVERHEAD_BYTES = 44     // salt(16) + iv(12) + tag(16)
private const val BASE64_NUMERATOR = 3
private const val BASE64_DENOMINATOR = 4

/** Advisory upper bound on plaintext characters that fit in a [width] x [height] cover. */
fun estimateMaxMessageChars(width: Int, height: Int): Int {
    val capacityBytes = width.toLong() * height.toLong() * USABLE_BITS_PER_PIXEL / BITS_PER_BYTE
    val payloadBytes = capacityBytes - MARKER_OVERHEAD_BYTES
    val encryptedBytes = payloadBytes * BASE64_NUMERATOR / BASE64_DENOMINATOR
    val messageBytes = encryptedBytes - CRYPTO_OVERHEAD_BYTES
    return messageBytes.coerceAtLeast(0L).toInt()
}
```

- [ ] **Step 5: Write `EncryptMessageUseCase.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase

import com.slothiesmooth.nyx.crypto.NyxCrypto
import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.steganography.Steganography
import com.slothiesmooth.nyx.steganography.StegoEncodeResult

private const val BITS_PER_BYTE = 8

class EncryptMessageUseCase(
    private val crypto: NyxCrypto,
    private val stego: Steganography,
    private val codec: ImageCodec,
) {
    suspend operator fun invoke(coverBytes: ByteArray, message: String, password: String): AppResult<ByteArray> {
        val pixelImage = when (val decoded = codec.decode(coverBytes)) {
            is AppResult.Ok -> decoded.value
            is AppResult.Err -> return decoded
        }
        val blob = crypto.encrypt(message, password)
        return when (val encoded = stego.encode(listOf(pixelImage), blob)) {
            is StegoEncodeResult.Success -> codec.encodePng(encoded.images.first())
            is StegoEncodeResult.CapacityExceeded -> AppResult.Err(AppError.Validation(capacityMessage(encoded)))
        }
    }

    private fun capacityMessage(result: StegoEncodeResult.CapacityExceeded): String {
        val required = result.requiredBits / BITS_PER_BYTE
        val available = result.availableBits / BITS_PER_BYTE
        return "Message too large for this image. It needs about $required characters of capacity but this image holds about $available. Use a larger image or a shorter message."
    }
}
```

- [ ] **Step 6: Write `SaveToVaultUseCase.kt`** (writes file, then row; blank `name` → auto `nyx-<id-prefix>.png`; rolls the file back if the row insert throws):

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase

import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.Clock
import com.slothiesmooth.nyx.shared.data.DomainEvent
import com.slothiesmooth.nyx.shared.data.DomainEventBus
import com.slothiesmooth.nyx.shared.data.IdGenerator
import com.slothiesmooth.nyx.shared.data.StegoImageId
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource

private const val ID_PREFIX_LENGTH = 8

class SaveToVaultUseCase(
    private val vaultSource: VaultSource,
    private val fileStore: VaultFileStore,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val eventBus: DomainEventBus,
) {
    suspend operator fun invoke(pngBytes: ByteArray, name: String): AppResult<StegoImageId> {
        val id = idGenerator.newId()
        val resolvedName = name.ifBlank { "nyx-${id.take(ID_PREFIX_LENGTH)}.png" }
        val now = clock.now().toString()
        when (val write = fileStore.write(id, pngBytes)) {
            is AppResult.Ok -> Unit
            is AppResult.Err -> return write
        }
        val record = StegoImageRecord(id = id, name = resolvedName, createdAt = now, updatedAt = now, deletedAt = null, isArchived = false)
        return try {
            vaultSource.upsert(record)
            val imageId = StegoImageId(id)
            eventBus.emit(DomainEvent.StegoImageStored(imageId))
            AppResult.Ok(imageId)
        } catch (cause: Throwable) {
            fileStore.delete(id)
            AppResult.Err(AppError.Storage("Failed to save image to the vault", cause))
        }
    }
}
```

- [ ] **Step 7: Run — expect GREEN.**

```bash
./gradlew :feature:encrypt:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.encrypt.basic.domain.EncryptDomainTest"
```

Expected: `BUILD SUCCESSFUL`, 5 tests passed. (The round-trip test exercises real AES-GCM + PBKDF2 + LSB stego end-to-end over the fake codec — the highest-value integration in this phase.)

- [ ] **Step 8: Commit.**

```bash
git add feature/encrypt/client/basic/build.gradle.kts feature/encrypt/client/basic/src
git commit -m "feat(encrypt): EncryptMessage/SaveToVault use cases and capacity estimate with tests"
```

---

### Task 7: Encrypt presentation — wizard (validation, State/ViewModel/Screen/Content + previews)

**Files (under `feature/encrypt/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/encrypt/basic/presentation/`):**
- Create: `EncryptValidation.kt`, `EncryptState.kt`, `EncryptViewModel.kt`, `EncryptScreen.kt`, `EncryptContent.kt`
- Test: `src/commonTest/kotlin/.../basic/presentation/EncryptValidationTest.kt`

**Interfaces:**
- Consumes: Task 6 use cases + `estimateMaxMessageChars`, `:shared:data` (`ImageCodec`, `CameraSource`, `ShareSource`, `PlatformCapabilities`, `AppResult`, `AppError`), `ByteArray.toImageBitmap()`, `BaseViewModel`/`UiState`, `koinFeatureViewModel`, FileKit picker.
- Produces: `EncryptStep`, `EncryptState`, `EncryptViewModel`, `EncryptScreen`, `EncryptContent`, `validateEncryptInput`. Consumed by `BasicEncryptProvider` (Task 8).

**Steps:**

- [ ] **Step 1: Write the failing validation test.** Create `EncryptValidationTest.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EncryptValidationTest {

    @Test
    fun `blank message cannot encrypt and shows no error yet`() {
        val result = validateEncryptInput(message = "", password = "pw", confirmPassword = "pw")
        assertFalse(result.canEncrypt)
        assertNull(result.error)
    }

    @Test
    fun `mismatched passwords surface an error and block encrypt`() {
        val result = validateEncryptInput(message = "hi", password = "pw", confirmPassword = "px")
        assertFalse(result.canEncrypt)
        assertEquals("Passwords do not match", result.error)
    }

    @Test
    fun `matching passwords with a message allow encrypt`() {
        val result = validateEncryptInput(message = "hi", password = "pw", confirmPassword = "pw")
        assertTrue(result.canEncrypt)
        assertNull(result.error)
    }
}
```

- [ ] **Step 2: Run — expect RED.**

```bash
./gradlew :feature:encrypt:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptValidationTest"
```

Expected: `Unresolved reference 'validateEncryptInput'`.

- [ ] **Step 3: Write `EncryptValidation.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

data class EncryptValidation(val canEncrypt: Boolean, val error: String?)

fun validateEncryptInput(message: String, password: String, confirmPassword: String): EncryptValidation {
    val mismatch = confirmPassword.isNotEmpty() && password != confirmPassword
    val error = if (mismatch) "Passwords do not match" else null
    val canEncrypt = message.isNotBlank() && password.isNotEmpty() && password == confirmPassword
    return EncryptValidation(canEncrypt = canEncrypt, error = error)
}
```

- [ ] **Step 4: Write `EncryptState.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.ViewState

enum class EncryptStep { PickImage, Compose, Result }

@Stable
interface EncryptState : ViewState {
    val step: EncryptStep
    val thumbnail: ImageBitmap?
    val hasImage: Boolean
    val maxCharsLabel: String
    val message: String
    val password: String
    val confirmPassword: String
    val validationError: String?
    val canEncrypt: Boolean
    val savedName: String?
    val showCamera: Boolean
}
```

- [ ] **Step 5: Write `EncryptViewModel.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.estimateMaxMessageChars
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import com.slothiesmooth.nyx.shared.presentation.image.toImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel

private const val ID_PREFIX_LENGTH = 8
private const val DEFAULT_SHARE_NAME = "nyx-image.png"
private const val ENCRYPT_FAILED = "Could not encrypt this image."

private class EncryptMutableState(cameraVisible: Boolean) : MutableViewState(), EncryptState {
    override var step: EncryptStep by mutableStateOf(EncryptStep.PickImage)
    override var thumbnail: ImageBitmap? by mutableStateOf(null)
    override var maxCharsLabel: String by mutableStateOf("")
    override var message: String by mutableStateOf("")
    override var password: String by mutableStateOf("")
    override var confirmPassword: String by mutableStateOf("")
    override var validationError: String? by mutableStateOf(null)
    override var canEncrypt: Boolean by mutableStateOf(false)
    override var savedName: String? by mutableStateOf(null)
    override val showCamera: Boolean = cameraVisible
    override val hasImage: Boolean get() = thumbnail != null
}

class EncryptViewModel(
    private val encryptMessage: EncryptMessageUseCase,
    private val saveToVault: SaveToVaultUseCase,
    private val codec: ImageCodec,
    private val cameraSource: CameraSource,
    private val shareSource: ShareSource,
    private val capabilities: PlatformCapabilities,
) : BaseViewModel() {

    private val mutableState = EncryptMutableState(cameraVisible = capabilities.camera && cameraSource.isAvailable)
    val state: EncryptState get() = mutableState
    private var coverBytes: ByteArray? = null
    private var resultBytes: ByteArray? = null
    private var shareFileName: String = DEFAULT_SHARE_NAME

    fun onImagePicked(bytes: ByteArray) {
        async("pick") {
            coverBytes = bytes
            val decoded = codec.decode(bytes)
            val thumb = runCatching { bytes.toImageBitmap() }.getOrNull()
            val label = if (decoded is AppResult.Ok) {
                "About ${estimateMaxMessageChars(decoded.value.width, decoded.value.height)} characters fit"
            } else {
                ""
            }
            withState {
                mutableState.thumbnail = thumb
                mutableState.maxCharsLabel = label
                mutableState.step = EncryptStep.Compose
            }
        }
    }

    fun onCameraCapture() { async("camera") { cameraSource.capture()?.let { onImagePicked(it.bytes) } } }

    fun onMessageChange(value: String) = updateInput { mutableState.message = value }
    fun onPasswordChange(value: String) = updateInput { mutableState.password = value }
    fun onConfirmChange(value: String) = updateInput { mutableState.confirmPassword = value }

    private inline fun updateInput(mutate: () -> Unit) = withState {
        mutate()
        val validation = validateEncryptInput(mutableState.message, mutableState.password, mutableState.confirmPassword)
        mutableState.canEncrypt = validation.canEncrypt
        mutableState.validationError = validation.error
    }

    fun encrypt() {
        val cover = coverBytes ?: return
        if (!mutableState.canEncrypt) return
        withState { mutableState.uiState = UiState.Blocking }
        async("encrypt") {
            when (val result = encryptMessage(cover, mutableState.message, mutableState.password)) {
                is AppResult.Ok -> onEncrypted(result.value)
                is AppResult.Err -> withState {
                    mutableState.validationError = (result.cause as? AppError.Validation)?.message ?: ENCRYPT_FAILED
                    mutableState.uiState = UiState.Ready
                }
            }
        }
    }

    private suspend fun onEncrypted(pngBytes: ByteArray) {
        resultBytes = pngBytes
        val savedId = if (capabilities.persistentVault) (saveToVault(pngBytes, name = "") as? AppResult.Ok)?.value else null
        val name = savedId?.let { "nyx-${it.value.take(ID_PREFIX_LENGTH)}.png" } ?: DEFAULT_SHARE_NAME
        shareFileName = name
        withState {
            mutableState.savedName = name
            mutableState.step = EncryptStep.Result
            mutableState.uiState = UiState.Ready
        }
    }

    fun share() { val bytes = resultBytes ?: return; async("share") { shareSource.shareImage(bytes, shareFileName) } }

    fun back() = withState {
        mutableState.step = when (mutableState.step) {
            EncryptStep.Result -> EncryptStep.Compose
            EncryptStep.Compose -> EncryptStep.PickImage
            EncryptStep.PickImage -> EncryptStep.PickImage
        }
    }

    fun reset() {
        coverBytes = null
        resultBytes = null
        shareFileName = DEFAULT_SHARE_NAME
        withState {
            mutableState.step = EncryptStep.PickImage
            mutableState.thumbnail = null
            mutableState.maxCharsLabel = ""
            mutableState.message = ""
            mutableState.password = ""
            mutableState.confirmPassword = ""
            mutableState.validationError = null
            mutableState.canEncrypt = false
            mutableState.savedName = null
        }
    }
}
```

- [ ] **Step 6: Run the validation test — expect GREEN + compile.**

```bash
./gradlew :feature:encrypt:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptValidationTest"
```

Expected: `BUILD SUCCESSFUL`, 3 tests passed (also compiles State + ViewModel).

- [ ] **Step 7: Commit the wizard model.**

```bash
git add feature/encrypt/client/basic/src
git commit -m "feat(encrypt): wizard state machine, validation, and view model"
```

- [ ] **Step 8: Write `EncryptScreen.kt` (thin; owns the FileKit picker):**

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

@Composable
fun EncryptScreen(onClose: () -> Unit) {
    val viewModel = koinFeatureViewModel<EncryptViewModel>()
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(type = FileKitType.Image, mode = FileKitMode.Single) { file ->
        if (file != null) scope.launch { viewModel.onImagePicked(file.readBytes()) }
    }
    EncryptContent(
        state = viewModel.state,
        onPickImage = { picker.launch() },
        onCaptureImage = viewModel::onCameraCapture,
        onMessageChange = viewModel::onMessageChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onEncrypt = viewModel::encrypt,
        onShare = viewModel::share,
        onReset = viewModel::reset,
        onBack = { if (viewModel.state.step == EncryptStep.PickImage) onClose() else viewModel.back() },
    )
}
```

FileKit import packages are the 0.13.0 layout (`io.github.vinceglb.filekit.*`) — see Open Questions to confirm exact subpackages against the resolved artifact.

- [ ] **Step 9: Write `EncryptContent.kt`** (dumb wizard body + previews):

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxField
import com.slothiesmooth.nyx.designlibrary.atoms.NxPasswordField
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxImageTile
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlay
import com.slothiesmooth.nyx.designlibrary.templates.NxWizardTemplate
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import com.slothiesmooth.nyx.shared.presentation.state.UiEvent
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private val StepGap = 16.dp
private val STEP_LABELS = persistentListOf("Image", "Message", "Done")

@Composable
fun EncryptContent(
    state: EncryptState,
    onPickImage: () -> Unit,
    onCaptureImage: () -> Unit,
    onMessageChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onEncrypt: () -> Unit,
    onShare: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        NxWizardTemplate(stepLabels = STEP_LABELS, currentStep = state.step.ordinal, title = "Encrypt", onBack = onBack) {
            Column(verticalArrangement = Arrangement.spacedBy(StepGap)) {
                when (state.step) {
                    EncryptStep.PickImage -> PickImageStep(state, onPickImage, onCaptureImage)
                    EncryptStep.Compose -> ComposeStep(state, onMessageChange, onPasswordChange, onConfirmChange, onEncrypt)
                    EncryptStep.Result -> ResultStep(state, onShare, onReset)
                }
            }
        }
        if (state.uiState is UiState.Blocking) NxProgressOverlay(label = "Encrypting", modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ColumnScopePickImage(state: EncryptState) {
    NxImageTile(image = state.thumbnail, contentDescription = "Selected image", modifier = Modifier.fillMaxWidth().aspectRatio(1f))
}

@Composable
private fun PickImageStep(state: EncryptState, onPickImage: () -> Unit, onCaptureImage: () -> Unit) {
    if (state.hasImage) ColumnScopePickImage(state)
    NxText(text = "Choose a cover image to hide your message in.", style = NxTextStyle.Body, color = NxTokens.colors.fgMuted)
    NxButton(text = "Choose image", onClick = onPickImage, style = NxButtonStyle.Primary, block = true, leadingIcon = NxIconKind.Image)
    if (state.showCamera) {
        NxButton(text = "Take photo", onClick = onCaptureImage, style = NxButtonStyle.Soft, block = true, leadingIcon = NxIconKind.Camera)
    }
}

@Composable
private fun ComposeStep(
    state: EncryptState,
    onMessageChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onEncrypt: () -> Unit,
) {
    NxImageTile(image = state.thumbnail, contentDescription = "Selected image", modifier = Modifier.fillMaxWidth().aspectRatio(1f))
    NxText(text = state.maxCharsLabel, style = NxTextStyle.Caption, color = NxTokens.colors.fgMuted)
    NxField(value = state.message, onValueChange = onMessageChange, label = "Secret message", multiline = true)
    NxPasswordField(value = state.password, onValueChange = onPasswordChange, label = "Password")
    NxPasswordField(value = state.confirmPassword, onValueChange = onConfirmChange, label = "Confirm password")
    val error = state.validationError
    if (error != null) NxText(text = error, style = NxTextStyle.Caption, color = NxTokens.colors.danger)
    NxButton(text = "Encrypt", onClick = onEncrypt, style = NxButtonStyle.Primary, block = true, leadingIcon = NxIconKind.Lock, enabled = state.canEncrypt)
}

@Composable
private fun ResultStep(state: EncryptState, onShare: () -> Unit, onReset: () -> Unit) {
    NxImageTile(image = state.thumbnail, contentDescription = "Encrypted image", modifier = Modifier.fillMaxWidth().aspectRatio(1f))
    NxText(text = "Encrypted and saved", style = NxTextStyle.Heading)
    NxText(text = state.savedName ?: "", style = NxTextStyle.Caption, color = NxTokens.colors.fgMuted)
    NxButton(text = "Share image", onClick = onShare, style = NxButtonStyle.Primary, block = true, leadingIcon = NxIconKind.Share)
    NxButton(text = "Encrypt another", onClick = onReset, style = NxButtonStyle.Ghost, block = true, leadingIcon = NxIconKind.Plus)
}

private class PreviewEncryptState(
    override val step: EncryptStep,
    override val message: String = "",
    override val password: String = "",
    override val confirmPassword: String = "",
    override val validationError: String? = null,
    override val canEncrypt: Boolean = false,
    override val savedName: String? = null,
    override val showCamera: Boolean = true,
) : EncryptState {
    override val thumbnail: androidx.compose.ui.graphics.ImageBitmap? = null
    override val hasImage: Boolean = false
    override val maxCharsLabel: String = "About 1820 characters fit"
    override val uiState: UiState = UiState.Ready
    override val uiEvent: Flow<UiEvent> = emptyFlow()
}

@AllThemePreview
@Composable
private fun EncryptPickPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    EncryptContent(PreviewEncryptState(EncryptStep.PickImage), {}, {}, {}, {}, {}, {}, {}, {}, {})
}

@AllThemePreview
@Composable
private fun EncryptComposePreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    EncryptContent(
        PreviewEncryptState(EncryptStep.Compose, message = "meet me", password = "pw", confirmPassword = "px", validationError = "Passwords do not match"),
        {}, {}, {}, {}, {}, {}, {}, {}, {},
    )
}

@AllThemePreview
@Composable
private fun EncryptResultPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    EncryptContent(PreviewEncryptState(EncryptStep.Result, savedName = "nyx-abcdef12.png"), {}, {}, {}, {}, {}, {}, {}, {}, {})
}
```

Note: the `ColumnScope` receiver for the step composables is provided by the enclosing `Column` in `EncryptContent`; the helper `ColumnScopePickImage` avoids a needless nested Column. (If detekt flags the helper as trivial, inline the `NxImageTile` call into `PickImageStep` instead — mechanical.)

- [ ] **Step 10: Verify compile + commit.**

```bash
./gradlew :feature:encrypt:client:basic:compileKotlinJvm
git add feature/encrypt/client/basic/src
git commit -m "feat(encrypt): wizard screen and content with previews"
```

---

### Task 8: `BasicEncryptProvider` + DI wiring + encrypt happy-path UI test

**UI-test approach (applies to Tasks 8 and 11):** the FileKit picker is a system dialog that cannot be driven headlessly, and a feature ViewModel lives in the feature's *isolated* Koin (not the app Koin), so it cannot be resolved through the whole-app `runFeatureUiTest` harness to seed picker bytes. Therefore the two critical-flow UI tests use `runComposeUiTest` directly: they hand-wire the ViewModel with real use cases over the `:shared:test-support` fakes, seed the picked/vault bytes programmatically, render the real `Content` composable, and drive the real UI (button clicks, result assertions) — exercising real crypto + stego + codec + save/read paths. They live in `jvmTest` (headless desktop Compose on Linux CI). `runFeatureUiTest` remains available (plan 05) for whole-app navigation flows later.

**Files:**
- Modify: `feature/encrypt/client/basic/build.gradle.kts` (add compose UI-test + test-support deps)
- Create: `feature/encrypt/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/encrypt/basic/BasicEncryptProvider.kt`
- Modify: `client/.../app/AppConfig.kt` (swap encrypt stub)
- Test: `feature/encrypt/client/basic/src/jvmTest/kotlin/.../basic/presentation/EncryptFlowUiTest.kt`

**Interfaces:**
- Consumes: `KoinFeatureProvider`, `FeatureContext`, `withDI`; `EncryptFeature`, `EncryptRoute`; Task 6-7 types; all outer sources/engines.
- Produces: `class BasicEncryptProvider(crypto, stego, codec, vaultSource, fileStore, idGenerator, clock, eventBus, cameraSource, shareSource, capabilities) : KoinFeatureProvider(), EncryptFeature`.

**Steps:**

- [ ] **Step 1: Add compose UI-test deps.** In `feature/encrypt/client/basic/build.gradle.kts` `commonTest.dependencies`, add:

```kotlin
            implementation(projects.shared.composeTestSupport)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
```

(`compose.uiTest` is the Compose Multiplatform `ui-test` artifact carrying `runComposeUiTest`; it is marked `@ExperimentalComposeLibrary` in the Compose Gradle DSL.)

- [ ] **Step 2: Write `BasicEncryptProvider.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.crypto.NyxCrypto
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptFeature
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptRoute
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptScreen
import com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptViewModel
import com.slothiesmooth.nyx.shared.data.Clock
import com.slothiesmooth.nyx.shared.data.DomainEventBus
import com.slothiesmooth.nyx.shared.data.IdGenerator
import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import com.slothiesmooth.nyx.steganography.Steganography
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

class BasicEncryptProvider(
    private val crypto: NyxCrypto,
    private val stego: Steganography,
    private val codec: ImageCodec,
    private val vaultSource: VaultSource,
    private val fileStore: VaultFileStore,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val eventBus: DomainEventBus,
    private val cameraSource: CameraSource,
    private val shareSource: ShareSource,
    private val capabilities: PlatformCapabilities,
) : KoinFeatureProvider(), EncryptFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun Module.onProvideDI() {
        single { crypto }
        single { stego }
        single { codec }
        single { vaultSource }
        single { fileStore }
        single { idGenerator }
        single { clock }
        single { eventBus }
        single { cameraSource }
        single { shareSource }
        single { capabilities }
        factoryOf(::EncryptMessageUseCase)
        factoryOf(::SaveToVaultUseCase)
        viewModelOf(::EncryptViewModel)
    }

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<EncryptRoute> {
            withDI { EncryptScreen(onClose = { context.popDestination() }) }
        }
    }
}
```

- [ ] **Step 3: Swap the encrypt registration in `AppConfig.kt`:**

```kotlin
    single<com.slothiesmooth.nyx.feature.encrypt.api.EncryptFeature> {
        com.slothiesmooth.nyx.feature.encrypt.basic.BasicEncryptProvider(
            crypto = get(), stego = get(), codec = get(),
            vaultSource = get(), fileStore = get(), idGenerator = get(),
            clock = get(), eventBus = get(), cameraSource = get(),
            shareSource = get(), capabilities = get(),
        )
    }
```

- [ ] **Step 4: Write the failing UI test.** Create `EncryptFlowUiTest.kt` in `src/jvmTest`:

```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.IdGenerator
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
import com.slothiesmooth.nyx.shared.testsupport.FakeCameraSource
import com.slothiesmooth.nyx.shared.testsupport.FakeClock
import com.slothiesmooth.nyx.shared.testsupport.FakeImageCodec
import com.slothiesmooth.nyx.shared.testsupport.FakeShareSource
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import com.slothiesmooth.nyx.steganography.PixelImage
import com.slothiesmooth.nyx.steganography.Steganography
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private const val COVER_SIDE = 96
private const val OPAQUE_BLACK = 0xFF000000.toInt()

@OptIn(ExperimentalTestApi::class)
class EncryptFlowUiTest {

    @Test
    fun `encrypting a message stores exactly one active vault item and shows the result`() = runComposeUiTest {
        val codec = FakeImageCodec()
        val source = FakeVaultSource()
        val store = FakeVaultFileStore()
        val idGenerator = object : IdGenerator { override fun newId() = "cafebabe12345678" }
        val viewModel = EncryptViewModel(
            encryptMessage = EncryptMessageUseCase(DefaultNyxCrypto(), Steganography(), codec),
            saveToVault = SaveToVaultUseCase(source, store, idGenerator, FakeClock(Instant.parse("2026-07-13T12:00:00Z")), DefaultDomainEventBus()),
            codec = codec,
            cameraSource = FakeCameraSource(isAvailable = false),
            shareSource = FakeShareSource(),
            capabilities = PlatformCapabilities(camera = false, persistentVault = true),
        )
        val cover = runBlocking {
            (codec.encodePng(PixelImage(COVER_SIDE, COVER_SIDE, IntArray(COVER_SIDE * COVER_SIDE) { OPAQUE_BLACK })) as AppResult.Ok).value
        }
        viewModel.onImagePicked(cover)

        setContent {
            NxTheme(NxPalette.Umbra) {
                EncryptContent(
                    state = viewModel.state,
                    onPickImage = {},
                    onCaptureImage = {},
                    onMessageChange = viewModel::onMessageChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onConfirmChange = viewModel::onConfirmChange,
                    onEncrypt = viewModel::encrypt,
                    onShare = viewModel::share,
                    onReset = viewModel::reset,
                    onBack = {},
                )
            }
        }

        waitUntil(timeoutMillis = 5_000) { viewModel.state.step == EncryptStep.Compose }
        onNodeWithText("Secret message").performTextInput("meet me at dawn")
        onNodeWithText("Password").performTextInput("hunter2")
        onNodeWithText("Confirm password").performTextInput("hunter2")
        onNodeWithText("Encrypt").performClick()
        waitUntil(timeoutMillis = 10_000) { viewModel.state.step == EncryptStep.Result }

        assertEquals(1, runBlocking { source.countActive() })
        onNodeWithText("Encrypted and saved").assertExists()
    }
}
```

- [ ] **Step 5: Run — expect RED then GREEN.** First run should fail to compile (`BasicEncryptProvider` unreferenced is fine; the test compiles once the provider+swap exist). Run:

```bash
./gradlew :feature:encrypt:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptFlowUiTest"
```

Expected: `BUILD SUCCESSFUL`, 1 test passed. If `onNodeWithText("Secret message")` cannot target the field (NxField renders its label outside the field's text semantics), add `Modifier.testTag("message"/"password"/"confirm")` params to those `NxField`/`NxPasswordField` calls in `EncryptContent` and switch the test to `onNodeWithTag(...)` — mechanical (see Open Questions).

- [ ] **Step 6: Verify `:client` still compiles + commit.**

```bash
./gradlew :client:compileKotlinJvm
git add feature/encrypt/client/basic client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/app/AppConfig.kt
git commit -m "feat(encrypt): BasicEncryptProvider wired into appModule with happy-path UI test"
```

---

### Task 9: Decrypt feature — use cases + outcome mapping (`:feature:decrypt:client:basic`)

**Files:**
- Modify: `feature/decrypt/client/basic/build.gradle.kts` (deps)
- Create (under `feature/decrypt/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/decrypt/basic/`): `domain/DecryptOutcome.kt`, `domain/usecase/DecryptMessageUseCase.kt`, `domain/usecase/LoadVaultImageBytesUseCase.kt`
- Test: `src/commonTest/kotlin/.../basic/domain/DecryptDomainTest.kt`

**Interfaces:**
- Consumes: `:crypto` (`NyxCrypto`, `DecryptResult`), `:steganography` (`Steganography`, `PixelImage`), `:shared:data` (`ImageCodec`, `AppResult`, `AppError`, `VaultFileStore`, `StegoImageId`).
- Produces (00-INDEX decrypt.basic.domain contract):
  - `sealed interface DecryptOutcome { Success(plaintext); NoHiddenMessage; WrongPasswordOrTampered; Failure(reason) }`
  - `class DecryptMessageUseCase(crypto, stego, codec)` → `suspend operator fun invoke(imageBytes, password): DecryptOutcome`
  - `class LoadVaultImageBytesUseCase(fileStore)` → `suspend operator fun invoke(id): AppResult<ByteArray>`

**Steps:**

- [ ] **Step 1: Set module deps.** In `feature/decrypt/client/basic/build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.crypto)
            implementation(projects.steganography)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.kotlinx.collections.immutable)
        }
        commonTest.dependencies {
            implementation(projects.crypto)
            implementation(projects.steganography)
            implementation(projects.shared.testSupport)
            implementation(projects.shared.composeTestSupport)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
    }
}
```

Verify: `./gradlew :feature:decrypt:client:basic:compileKotlinJvm` → `BUILD SUCCESSFUL`.

- [ ] **Step 2: Write the failing test.** Create `DecryptDomainTest.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic.domain

import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.DecryptMessageUseCase
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.LoadVaultImageBytesUseCase
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.StegoImageId
import com.slothiesmooth.nyx.shared.testsupport.FakeImageCodec
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.steganography.PixelImage
import com.slothiesmooth.nyx.steganography.StegoEncodeResult
import com.slothiesmooth.nyx.steganography.Steganography
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

private const val OPAQUE = 0xFF000000.toInt()

class DecryptDomainTest {

    private val codec = FakeImageCodec()
    private val crypto = DefaultNyxCrypto()
    private val stego = Steganography()

    private suspend fun stegoImageBytes(message: String, password: String): ByteArray {
        val cover = PixelImage(96, 96, IntArray(96 * 96) { OPAQUE })
        val blob = crypto.encrypt(message, password)
        val encoded = stego.encode(listOf(cover), blob) as StegoEncodeResult.Success
        return (codec.encodePng(encoded.images.first()) as AppResult.Ok).value
    }

    private suspend fun plainImageBytes(): ByteArray =
        (codec.encodePng(PixelImage(8, 8, IntArray(64) { OPAQUE })) as AppResult.Ok).value

    @Test
    fun `correct password reveals the plaintext`() = runTest {
        val bytes = stegoImageBytes("the eagle lands at noon", "correct-horse")
        val outcome = DecryptMessageUseCase(crypto, stego, codec)(bytes, "correct-horse")
        assertEquals(DecryptOutcome.Success("the eagle lands at noon"), outcome)
    }

    @Test
    fun `wrong password is distinct from a malformed image`() = runTest {
        val bytes = stegoImageBytes("secret", "right")
        val outcome = DecryptMessageUseCase(crypto, stego, codec)(bytes, "wrong")
        assertEquals(DecryptOutcome.WrongPasswordOrTampered, outcome)
    }

    @Test
    fun `image without a hidden payload reports no hidden message`() = runTest {
        val outcome = DecryptMessageUseCase(crypto, stego, codec)(plainImageBytes(), "anything")
        assertEquals(DecryptOutcome.NoHiddenMessage, outcome)
    }

    @Test
    fun `load vault image bytes reads the file store`() = runTest {
        val store = FakeVaultFileStore().apply { write("a", byteArrayOf(5, 6)) }
        val result = LoadVaultImageBytesUseCase(store)(StegoImageId("a"))
        assertIs<AppResult.Ok<ByteArray>>(result)
        assertTrue(result.value.contentEquals(byteArrayOf(5, 6)))
    }
}
```

- [ ] **Step 3: Run — expect RED.**

```bash
./gradlew :feature:decrypt:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptDomainTest"
```

Expected: `Unresolved reference 'DecryptMessageUseCase'`.

- [ ] **Step 4: Write `DecryptOutcome.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic.domain

sealed interface DecryptOutcome {
    data class Success(val plaintext: String) : DecryptOutcome
    data object NoHiddenMessage : DecryptOutcome
    data object WrongPasswordOrTampered : DecryptOutcome
    data class Failure(val reason: String) : DecryptOutcome
}
```

- [ ] **Step 5: Write `DecryptMessageUseCase.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase

import com.slothiesmooth.nyx.crypto.DecryptResult
import com.slothiesmooth.nyx.crypto.NyxCrypto
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptOutcome
import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.steganography.Steganography

private const val UNREADABLE_IMAGE = "This image could not be read."

class DecryptMessageUseCase(
    private val crypto: NyxCrypto,
    private val stego: Steganography,
    private val codec: ImageCodec,
) {
    suspend operator fun invoke(imageBytes: ByteArray, password: String): DecryptOutcome {
        val pixelImage = when (val decoded = codec.decode(imageBytes)) {
            is AppResult.Ok -> decoded.value
            is AppResult.Err -> return DecryptOutcome.Failure(reasonFor(decoded.cause))
        }
        val blob = stego.decode(listOf(pixelImage)) ?: return DecryptOutcome.NoHiddenMessage
        return when (val result = crypto.decrypt(blob, password)) {
            is DecryptResult.Success -> DecryptOutcome.Success(result.plaintext)
            DecryptResult.WrongPasswordOrTampered -> DecryptOutcome.WrongPasswordOrTampered
            is DecryptResult.Failure -> DecryptOutcome.Failure(result.reason)
        }
    }

    private fun reasonFor(error: AppError): String = when (error) {
        is AppError.Storage -> error.message
        is AppError.Validation -> error.message
        else -> UNREADABLE_IMAGE
    }
}
```

- [ ] **Step 6: Write `LoadVaultImageBytesUseCase.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase

import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.StegoImageId
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore

class LoadVaultImageBytesUseCase(private val fileStore: VaultFileStore) {
    suspend operator fun invoke(id: StegoImageId): AppResult<ByteArray> = fileStore.read(id.value)
}
```

- [ ] **Step 7: Run — expect GREEN.**

```bash
./gradlew :feature:decrypt:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptDomainTest"
```

Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 8: Commit.**

```bash
git add feature/decrypt/client/basic/build.gradle.kts feature/decrypt/client/basic/src
git commit -m "feat(decrypt): DecryptMessage/LoadVaultImageBytes use cases with outcome tests"
```

---

### Task 10: Decrypt presentation — reveal screen (outcome mapping, State/ViewModel/Screen/Content + previews)

**Files (under `feature/decrypt/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/decrypt/basic/presentation/`):**
- Create: `DecryptDisplay.kt`, `DecryptState.kt`, `DecryptViewModel.kt`, `DecryptScreen.kt`, `DecryptContent.kt`
- Test: `src/commonTest/kotlin/.../basic/presentation/DecryptDisplayTest.kt`

**Interfaces:**
- Consumes: Task 9 use cases + `DecryptOutcome`, `:shared:data` (`AppResult`, `StegoImageId`), `ByteArray.toImageBitmap()`, `BaseViewModel`/`UiState`, `koinFeatureViewModel`, FileKit picker, CMP `LocalClipboardManager`.
- Produces: `DecryptDisplay`, `mapDecryptOutcome`, `DecryptState`, `DecryptViewModel`, `DecryptScreen`, `DecryptContent`.

**Steps:**

- [ ] **Step 1: Write the failing mapper test.** Create `DecryptDisplayTest.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class DecryptDisplayTest {

    @Test
    fun `success exposes plaintext and no error`() {
        val display = mapDecryptOutcome(DecryptOutcome.Success("hello"))
        assertEquals("hello", display.plaintext)
        assertNull(display.error)
    }

    @Test
    fun `wrong password and no-hidden-message render distinct honest errors`() {
        val wrong = mapDecryptOutcome(DecryptOutcome.WrongPasswordOrTampered)
        val none = mapDecryptOutcome(DecryptOutcome.NoHiddenMessage)
        assertNull(wrong.plaintext)
        assertNull(none.plaintext)
        assertNotEquals(wrong.error, none.error)
    }

    @Test
    fun `failure surfaces its reason`() {
        val display = mapDecryptOutcome(DecryptOutcome.Failure("This image could not be read."))
        assertEquals("This image could not be read.", display.error)
    }
}
```

- [ ] **Step 2: Run — expect RED.**

```bash
./gradlew :feature:decrypt:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.decrypt.basic.presentation.DecryptDisplayTest"
```

Expected: `Unresolved reference 'mapDecryptOutcome'`.

- [ ] **Step 3: Write `DecryptDisplay.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptOutcome

private const val WRONG_PASSWORD = "Wrong password, or this image has been tampered with."
private const val NO_MESSAGE = "No hidden message was found in this image."

data class DecryptDisplay(val plaintext: String?, val error: String?)

fun mapDecryptOutcome(outcome: DecryptOutcome): DecryptDisplay = when (outcome) {
    is DecryptOutcome.Success -> DecryptDisplay(plaintext = outcome.plaintext, error = null)
    DecryptOutcome.WrongPasswordOrTampered -> DecryptDisplay(plaintext = null, error = WRONG_PASSWORD)
    DecryptOutcome.NoHiddenMessage -> DecryptDisplay(plaintext = null, error = NO_MESSAGE)
    is DecryptOutcome.Failure -> DecryptDisplay(plaintext = null, error = outcome.reason)
}
```

- [ ] **Step 4: Write `DecryptState.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.ViewState

@Stable
interface DecryptState : ViewState {
    val thumbnail: ImageBitmap?
    val hasImage: Boolean
    val isFromVault: Boolean
    val password: String
    val canDecrypt: Boolean
    val plaintext: String?
    val errorMessage: String?
}
```

- [ ] **Step 5: Write `DecryptViewModel.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.DecryptMessageUseCase
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.LoadVaultImageBytesUseCase
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.StegoImageId
import com.slothiesmooth.nyx.shared.presentation.image.toImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel

private const val LOAD_FAILED = "This image could not be loaded from the vault."

private class DecryptMutableState : MutableViewState(), DecryptState {
    override var thumbnail: ImageBitmap? by mutableStateOf(null)
    override var hasImage: Boolean by mutableStateOf(false)
    override var isFromVault: Boolean by mutableStateOf(false)
    override var password: String by mutableStateOf("")
    override var canDecrypt: Boolean by mutableStateOf(false)
    override var plaintext: String? by mutableStateOf(null)
    override var errorMessage: String? by mutableStateOf(null)
}

class DecryptViewModel(
    private val decryptMessage: DecryptMessageUseCase,
    private val loadVaultImageBytes: LoadVaultImageBytesUseCase,
) : BaseViewModel() {

    private val mutableState = DecryptMutableState()
    val state: DecryptState get() = mutableState
    private var imageBytes: ByteArray? = null

    fun load(imageId: String?) {
        if (imageId == null) return
        async("load", force = true) {
            when (val result = loadVaultImageBytes(StegoImageId(imageId))) {
                is AppResult.Ok -> setImage(result.value, fromVault = true)
                is AppResult.Err -> withState {
                    mutableState.isFromVault = true
                    mutableState.errorMessage = LOAD_FAILED
                }
            }
        }
    }

    fun onImagePicked(bytes: ByteArray) { async("pick") { setImage(bytes, fromVault = false) } }

    private suspend fun setImage(bytes: ByteArray, fromVault: Boolean) {
        imageBytes = bytes
        val thumb = runCatching { bytes.toImageBitmap() }.getOrNull()
        withState {
            mutableState.thumbnail = thumb
            mutableState.hasImage = true
            mutableState.isFromVault = fromVault
            mutableState.plaintext = null
            mutableState.errorMessage = null
            recomputeCanDecrypt()
        }
    }

    fun onPasswordChange(value: String) = withState {
        mutableState.password = value
        recomputeCanDecrypt()
    }

    fun decrypt() {
        val bytes = imageBytes ?: return
        if (!mutableState.canDecrypt) return
        withState { mutableState.uiState = UiState.Blocking }
        async("decrypt") {
            val display = mapDecryptOutcome(decryptMessage(bytes, mutableState.password))
            withState {
                mutableState.plaintext = display.plaintext
                mutableState.errorMessage = display.error
                mutableState.uiState = UiState.Ready
            }
        }
    }

    private fun recomputeCanDecrypt() {
        mutableState.canDecrypt = imageBytes != null && mutableState.password.isNotEmpty()
    }
}
```

- [ ] **Step 6: Run the mapper test — expect GREEN + compile.**

```bash
./gradlew :feature:decrypt:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.decrypt.basic.presentation.DecryptDisplayTest"
```

Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 7: Commit.**

```bash
git add feature/decrypt/client/basic/src
git commit -m "feat(decrypt): reveal state machine, outcome mapping, and view model"
```

- [ ] **Step 8: Write `DecryptScreen.kt` (owns the picker + clipboard copy):**

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

@Composable
fun DecryptScreen(
    imageId: String?,
    onBack: () -> Unit,
) {
    val viewModel = koinFeatureViewModel<DecryptViewModel>()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(imageId) { viewModel.load(imageId) }
    val picker = rememberFilePickerLauncher(type = FileKitType.Image, mode = FileKitMode.Single) { file ->
        if (file != null) scope.launch { viewModel.onImagePicked(file.readBytes()) }
    }
    DecryptContent(
        state = viewModel.state,
        onPickImage = { picker.launch() },
        onPasswordChange = viewModel::onPasswordChange,
        onDecrypt = viewModel::decrypt,
        onCopy = { text -> clipboard.setText(AnnotatedString(text)) },
        onBack = onBack,
    )
}
```

- [ ] **Step 9: Write `DecryptContent.kt`** (dumb; distinct reveal vs error + previews):

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxPasswordField
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxCard
import com.slothiesmooth.nyx.designlibrary.molecules.NxImageTile
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlay
import com.slothiesmooth.nyx.designlibrary.templates.NxDetailTemplate
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxCardVariant
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import com.slothiesmooth.nyx.shared.presentation.state.UiEvent
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private val SectionGap = 16.dp
private val CodePadding = 12.dp

@Composable
fun DecryptContent(
    state: DecryptState,
    onPickImage: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onDecrypt: () -> Unit,
    onCopy: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        NxDetailTemplate(title = "Decrypt", onBack = onBack) {
            Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
                if (!state.isFromVault && !state.hasImage) {
                    NxText(text = "Choose an image that hides an encrypted message.", style = NxTextStyle.Body, color = NxTokens.colors.fgMuted)
                    NxButton(text = "Choose image", onClick = onPickImage, style = NxButtonStyle.Primary, block = true, leadingIcon = NxIconKind.Image)
                } else {
                    NxImageTile(image = state.thumbnail, contentDescription = "Image to decrypt", modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                }
                NxPasswordField(value = state.password, onValueChange = onPasswordChange, label = "Password")
                NxButton(text = "Reveal message", onClick = onDecrypt, style = NxButtonStyle.Primary, block = true, leadingIcon = NxIconKind.Unlock, enabled = state.canDecrypt)

                val plaintext = state.plaintext
                if (plaintext != null) {
                    NxCard(variant = NxCardVariant.Flat) {
                        Row(modifier = Modifier.fillMaxWidth().padding(CodePadding), verticalAlignment = Alignment.Top) {
                            NxText(text = plaintext, style = NxTextStyle.Mono, color = NxTokens.colors.codeFg, modifier = Modifier.weight(1f))
                            NxIconButton(kind = NxIconKind.Copy, onClick = { onCopy(plaintext) }, contentDescription = "Copy message")
                        }
                    }
                }
                val error = state.errorMessage
                if (error != null) {
                    NxText(text = error, style = NxTextStyle.Body, color = NxTokens.colors.danger)
                }
            }
        }
        if (state.uiState is UiState.Blocking) NxProgressOverlay(label = "Decrypting", modifier = Modifier.fillMaxSize())
    }
}

private class PreviewDecryptState(
    override val hasImage: Boolean,
    override val isFromVault: Boolean,
    override val password: String,
    override val canDecrypt: Boolean,
    override val plaintext: String?,
    override val errorMessage: String?,
) : DecryptState {
    override val thumbnail: androidx.compose.ui.graphics.ImageBitmap? = null
    override val uiState: UiState = UiState.Ready
    override val uiEvent: Flow<UiEvent> = emptyFlow()
}

@AllThemePreview
@Composable
private fun DecryptPickPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    DecryptContent(PreviewDecryptState(false, false, "", false, null, null), {}, {}, {}, {}, {})
}

@AllThemePreview
@Composable
private fun DecryptRevealedPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    DecryptContent(PreviewDecryptState(true, true, "hunter2", true, "meet me at the north pier at 0500", null), {}, {}, {}, {}, {})
}

@AllThemePreview
@Composable
private fun DecryptErrorPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    DecryptContent(PreviewDecryptState(true, true, "wrong", true, null, "Wrong password, or this image has been tampered with."), {}, {}, {}, {}, {})
}
```

- [ ] **Step 10: Verify compile + commit.**

```bash
./gradlew :feature:decrypt:client:basic:compileKotlinJvm
git add feature/decrypt/client/basic/src
git commit -m "feat(decrypt): reveal screen and content with copy-to-clipboard and previews"
```

---

### Task 11: `BasicDecryptProvider` + DI wiring + wrong-password UI test

**Files:**
- Create: `feature/decrypt/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/decrypt/basic/BasicDecryptProvider.kt`
- Modify: `client/.../app/AppConfig.kt` (swap decrypt stub)
- Test: `feature/decrypt/client/basic/src/jvmTest/kotlin/.../basic/presentation/DecryptFlowUiTest.kt`

**Interfaces:**
- Consumes: `KoinFeatureProvider`, `FeatureContext`, `withDI`; `DecryptFeature`, `DecryptRoute`; Task 9-10 types; `NyxCrypto`, `Steganography`, `ImageCodec`, `VaultFileStore`.
- Produces: `class BasicDecryptProvider(crypto, stego, codec, fileStore) : KoinFeatureProvider(), DecryptFeature`.

**Steps:**

- [ ] **Step 1: Write `BasicDecryptProvider.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.slothiesmooth.nyx.crypto.NyxCrypto
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptFeature
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptRoute
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.DecryptMessageUseCase
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.LoadVaultImageBytesUseCase
import com.slothiesmooth.nyx.feature.decrypt.basic.presentation.DecryptScreen
import com.slothiesmooth.nyx.feature.decrypt.basic.presentation.DecryptViewModel
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.steganography.Steganography
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

class BasicDecryptProvider(
    private val crypto: NyxCrypto,
    private val stego: Steganography,
    private val codec: ImageCodec,
    private val fileStore: VaultFileStore,
) : KoinFeatureProvider(), DecryptFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun Module.onProvideDI() {
        single { crypto }
        single { stego }
        single { codec }
        single { fileStore }
        factoryOf(::DecryptMessageUseCase)
        factoryOf(::LoadVaultImageBytesUseCase)
        viewModelOf(::DecryptViewModel)
    }

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<DecryptRoute> { entry ->
            val route = entry.toRoute<DecryptRoute>()
            withDI { DecryptScreen(imageId = route.imageId, onBack = { context.popDestination() }) }
        }
    }
}
```

- [ ] **Step 2: Swap the decrypt registration in `AppConfig.kt`:**

```kotlin
    single<com.slothiesmooth.nyx.feature.decrypt.api.DecryptFeature> {
        com.slothiesmooth.nyx.feature.decrypt.basic.BasicDecryptProvider(
            crypto = get(), stego = get(), codec = get(), fileStore = get(),
        )
    }
```

- [ ] **Step 3: Write the failing UI test.** Create `DecryptFlowUiTest.kt` in `src/jvmTest`:

```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.DecryptMessageUseCase
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.LoadVaultImageBytesUseCase
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.testsupport.FakeImageCodec
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.steganography.PixelImage
import com.slothiesmooth.nyx.steganography.StegoEncodeResult
import com.slothiesmooth.nyx.steganography.Steganography
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNull

private const val COVER_SIDE = 96
private const val OPAQUE_BLACK = 0xFF000000.toInt()

@OptIn(ExperimentalTestApi::class)
class DecryptFlowUiTest {

    @Test
    fun `a wrong password shows the distinct tamper error and no plaintext`() = runComposeUiTest {
        val codec = FakeImageCodec()
        val crypto = DefaultNyxCrypto()
        val stego = Steganography()
        val viewModel = DecryptViewModel(
            decryptMessage = DecryptMessageUseCase(crypto, stego, codec),
            loadVaultImageBytes = LoadVaultImageBytesUseCase(FakeVaultFileStore()),
        )
        val stegoBytes = runBlocking {
            val cover = PixelImage(COVER_SIDE, COVER_SIDE, IntArray(COVER_SIDE * COVER_SIDE) { OPAQUE_BLACK })
            val blob = crypto.encrypt("the eagle lands at noon", "right-key")
            val encoded = stego.encode(listOf(cover), blob) as StegoEncodeResult.Success
            (codec.encodePng(encoded.images.first()) as AppResult.Ok).value
        }
        viewModel.onImagePicked(stegoBytes)

        setContent {
            NxTheme(NxPalette.Umbra) {
                DecryptContent(
                    state = viewModel.state,
                    onPickImage = {},
                    onPasswordChange = viewModel::onPasswordChange,
                    onDecrypt = viewModel::decrypt,
                    onCopy = {},
                    onBack = {},
                )
            }
        }

        waitUntil(timeoutMillis = 5_000) { viewModel.state.hasImage }
        onNodeWithText("Password").performTextInput("wrong-key")
        onNodeWithText("Reveal message").performClick()
        waitUntil(timeoutMillis = 10_000) { viewModel.state.errorMessage != null || viewModel.state.plaintext != null }

        onNodeWithText("Wrong password, or this image has been tampered with.").assertExists()
        assertNull(viewModel.state.plaintext)
    }
}
```

- [ ] **Step 4: Run — expect GREEN.**

```bash
./gradlew :feature:decrypt:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.decrypt.basic.presentation.DecryptFlowUiTest"
```

Expected: `BUILD SUCCESSFUL`, 1 test passed. (Same NxField label-targeting caveat as Task 8 applies — switch to `testTag` if needed.)

- [ ] **Step 5: Verify `:client` compiles + commit.**

```bash
./gradlew :client:compileKotlinJvm
git add feature/decrypt/client/basic client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/app/AppConfig.kt
git commit -m "feat(decrypt): BasicDecryptProvider wired into appModule with wrong-password UI test"
```

---

### Task 12: Settings feature — wipe use case, licenses data, licenses route (`:feature:settings:client:{api,basic}`)

**Files:**
- Modify: `feature/settings/client/basic/build.gradle.kts` (deps)
- Create: `feature/settings/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/settings/api/SettingsLicensesRoute.kt`
- Create (under `feature/settings/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/settings/basic/`): `domain/LicenseEntry.kt`, `domain/NyxLicenses.kt`, `domain/usecase/WipeVaultUseCase.kt`
- Test: `src/commonTest/kotlin/.../basic/domain/WipeVaultUseCaseTest.kt`

**Interfaces:**
- Consumes: `:shared:data` (`AppResult`, `AppError`, `VaultSource`, `VaultFileStore`, `DomainEventBus`, `DomainEvent`), kotlinx-collections-immutable.
- Produces:
  - `@Serializable data object SettingsLicensesRoute` (settings.api addition — internal sub-route)
  - `data class LicenseEntry(val name: String, val license: String, val url: String)` + `val nyxLicenses: ImmutableList<LicenseEntry>`
  - `class WipeVaultUseCase(vaultSource, fileStore, eventBus)` → `suspend operator fun invoke(): AppResult<Unit>`

**Steps:**

- [ ] **Step 1: Set module deps.** In `feature/settings/client/basic/build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.theme.client.api)   // ThemeRoute (settings -> theme)
            implementation(libs.kotlinx.collections.immutable)
        }
        commonTest.dependencies {
            implementation(projects.shared.testSupport)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

Verify: `./gradlew :feature:settings:client:basic:compileKotlinJvm` → `BUILD SUCCESSFUL`.

- [ ] **Step 2: Add the licenses route to `settings.api`.** Create `SettingsLicensesRoute.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.settings.api

import kotlinx.serialization.Serializable

@Serializable
data object SettingsLicensesRoute
```

Verify: `./gradlew :feature:settings:client:api:compileKotlinJvm` → `BUILD SUCCESSFUL`.

- [ ] **Step 3: Write the failing test.** Create `WipeVaultUseCaseTest.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.domain

import com.slothiesmooth.nyx.feature.settings.basic.domain.usecase.WipeVaultUseCase
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.DomainEvent
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WipeVaultUseCaseTest {

    @Test
    fun `wipe purges rows and files and emits VaultWiped`() = runTest {
        val source = FakeVaultSource().apply {
            upsert(StegoImageRecord("a", "nyx-a.png", "2026-07-13T00:00:00Z", "2026-07-13T00:00:00Z", null, false))
        }
        val store = FakeVaultFileStore().apply { write("a", byteArrayOf(1)) }
        val bus = DefaultDomainEventBus()
        val events = mutableListOf<DomainEvent>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch { bus.events.collect { events.add(it) } }

        val result = WipeVaultUseCase(source, store, bus)()

        assertTrue(result is AppResult.Ok)
        assertTrue(source.observeActive().first().isEmpty())
        assertTrue(store.read("a") is AppResult.Err)
        assertEquals(DomainEvent.VaultWiped, events.single())
        job.cancel()
    }

    @Test
    fun `wipe reports a storage error when file deletion fails and does not emit`() = runTest {
        val failingStore = FailingDeleteAllStore()
        val bus = DefaultDomainEventBus()
        val events = mutableListOf<DomainEvent>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch { bus.events.collect { events.add(it) } }

        val result = WipeVaultUseCase(FakeVaultSource(), failingStore, bus)()

        assertTrue(result is AppResult.Err)
        assertTrue(events.isEmpty())
        job.cancel()
    }
}

private class FailingDeleteAllStore : com.slothiesmooth.nyx.shared.data.source.VaultFileStore {
    override suspend fun write(id: String, bytes: ByteArray) = com.slothiesmooth.nyx.shared.data.AppResult.Ok(Unit)
    override suspend fun read(id: String) = com.slothiesmooth.nyx.shared.data.AppResult.Err(com.slothiesmooth.nyx.shared.data.AppError.NotFound)
    override suspend fun delete(id: String) = com.slothiesmooth.nyx.shared.data.AppResult.Ok(Unit)
    override suspend fun deleteAll() = com.slothiesmooth.nyx.shared.data.AppResult.Err(com.slothiesmooth.nyx.shared.data.AppError.Storage("disk error"))
}
```

The `FailingDeleteAllStore` double lives at the bottom of the same test file (`FakeVaultFileStore` always succeeds `deleteAll`, so a purpose-built double is needed for this branch).

- [ ] **Step 4: Run — expect RED.**

```bash
./gradlew :feature:settings:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.settings.basic.domain.WipeVaultUseCaseTest"
```

Expected: `Unresolved reference 'WipeVaultUseCase'`.

- [ ] **Step 5: Write `WipeVaultUseCase.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.domain.usecase

import com.slothiesmooth.nyx.shared.data.AppError
import com.slothiesmooth.nyx.shared.data.AppResult
import com.slothiesmooth.nyx.shared.data.DomainEvent
import com.slothiesmooth.nyx.shared.data.DomainEventBus
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource

private const val WIPE_FAILED = "Failed to wipe the vault"

/** Hard delete: removes every file AND every row (bypasses tombstones), then emits [DomainEvent.VaultWiped]. */
class WipeVaultUseCase(
    private val vaultSource: VaultSource,
    private val fileStore: VaultFileStore,
    private val eventBus: DomainEventBus,
) {
    suspend operator fun invoke(): AppResult<Unit> {
        when (val files = fileStore.deleteAll()) {
            is AppResult.Ok -> Unit
            is AppResult.Err -> return files
        }
        return try {
            vaultSource.purgeAll()
            eventBus.emit(DomainEvent.VaultWiped)
            AppResult.Ok(Unit)
        } catch (cause: Throwable) {
            AppResult.Err(AppError.Storage(WIPE_FAILED, cause))
        }
    }
}
```

- [ ] **Step 6: Write `LicenseEntry.kt` and `NyxLicenses.kt`** (hand-maintained; no plugin):

`LicenseEntry.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.domain

import androidx.compose.runtime.Immutable

@Immutable
data class LicenseEntry(val name: String, val license: String, val url: String)
```

`NyxLicenses.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.domain

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Hand-maintained list of the third-party libraries Nyx ships. Keep in sync with the version catalog. */
val nyxLicenses: ImmutableList<LicenseEntry> = persistentListOf(
    LicenseEntry("Kotlin", "Apache-2.0", "https://github.com/JetBrains/kotlin"),
    LicenseEntry("Jetpack Compose Multiplatform", "Apache-2.0", "https://github.com/JetBrains/compose-multiplatform"),
    LicenseEntry("kotlinx.coroutines", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    LicenseEntry("kotlinx.serialization", "Apache-2.0", "https://github.com/Kotlin/kotlinx.serialization"),
    LicenseEntry("kotlinx-datetime", "Apache-2.0", "https://github.com/Kotlin/kotlinx-datetime"),
    LicenseEntry("kotlinx.collections.immutable", "Apache-2.0", "https://github.com/Kotlin/kotlinx.collections.immutable"),
    LicenseEntry("Koin", "Apache-2.0", "https://github.com/InsertKoinIO/koin"),
    LicenseEntry("SQLDelight", "Apache-2.0", "https://github.com/cashapp/sqldelight"),
    LicenseEntry("cryptography-kotlin", "Apache-2.0", "https://github.com/whyoleg/cryptography-kotlin"),
    LicenseEntry("FileKit", "MIT", "https://github.com/vinceglb/FileKit"),
    LicenseEntry("Kermit", "Apache-2.0", "https://github.com/touchlab/Kermit"),
    LicenseEntry("JetBrains Mono", "OFL-1.1", "https://github.com/JetBrains/JetBrainsMono"),
)
```

- [ ] **Step 7: Run — expect GREEN.**

```bash
./gradlew :feature:settings:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.settings.basic.domain.WipeVaultUseCaseTest"
```

Expected: `BUILD SUCCESSFUL`, 2 tests passed.

- [ ] **Step 8: Commit.**

```bash
git add feature/settings/client/api/src feature/settings/client/basic/build.gradle.kts feature/settings/client/basic/src
git commit -m "feat(settings): wipe-vault use case, OSS license list, and licenses route with tests"
```

---

### Task 13: Settings presentation — about + licenses (State/ViewModel/Screen/Content + previews)

**Files (under `feature/settings/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/settings/basic/presentation/`):**
- Create: `SettingsLabels.kt`, `SettingsState.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`, `SettingsContent.kt`, `licenses/LicensesScreen.kt`, `licenses/LicensesContent.kt`
- Test: `src/commonTest/kotlin/.../basic/presentation/SettingsLabelsTest.kt`

**Interfaces:**
- Consumes: Task 12 `WipeVaultUseCase` + `nyxLicenses`/`LicenseEntry`, `:shared:data` `AppInfo`, `BaseViewModel`/`UiState`, `koinFeatureViewModel`, CMP `LocalUriHandler`, Material3 `AlertDialog`.
- Produces: `versionLabel`, `SettingsState`, `SettingsViewModel`, `SettingsScreen`, `SettingsContent`, `LicensesScreen`, `LicensesContent`.

**Steps:**

- [ ] **Step 1: Write the failing labels test.** Create `SettingsLabelsTest.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsLabelsTest {
    @Test
    fun `version label combines app name version and platform`() {
        assertEquals("Nyx 1.0.0 · Android", versionLabel(versionName = "1.0.0", platformName = "Android"))
    }
}
```

- [ ] **Step 2: Run — expect RED.**

```bash
./gradlew :feature:settings:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.settings.basic.presentation.SettingsLabelsTest"
```

Expected: `Unresolved reference 'versionLabel'`.

- [ ] **Step 3: Write `SettingsLabels.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.presentation

private const val APP_NAME = "Nyx"

fun versionLabel(versionName: String, platformName: String): String = "$APP_NAME $versionName · $platformName"
```

- [ ] **Step 4: Write `SettingsState.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.presentation

import androidx.compose.runtime.Stable
import com.slothiesmooth.nyx.shared.presentation.state.ViewState

@Stable
interface SettingsState : ViewState {
    val versionLabel: String
    val showWipeConfirm: Boolean
    val isWiping: Boolean
}
```

- [ ] **Step 5: Write `SettingsViewModel.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slothiesmooth.nyx.feature.settings.basic.domain.usecase.WipeVaultUseCase
import com.slothiesmooth.nyx.shared.data.source.AppInfo
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel

private class SettingsMutableState(initialVersionLabel: String) : MutableViewState(), SettingsState {
    override var versionLabel: String by mutableStateOf(initialVersionLabel)
    override var showWipeConfirm: Boolean by mutableStateOf(false)
    override var isWiping: Boolean by mutableStateOf(false)
}

class SettingsViewModel(
    private val wipeVault: WipeVaultUseCase,
    appInfo: AppInfo,
) : BaseViewModel() {

    private val mutableState = SettingsMutableState(versionLabel(appInfo.versionName, appInfo.platformName))
    val state: SettingsState get() = mutableState

    fun requestWipe() = withState { mutableState.showWipeConfirm = true }
    fun cancelWipe() = withState { mutableState.showWipeConfirm = false }

    fun confirmWipe() {
        withState {
            mutableState.showWipeConfirm = false
            mutableState.isWiping = true
        }
        async("wipe") {
            wipeVault()
            withState { mutableState.isWiping = false }
        }
    }
}
```

- [ ] **Step 6: Run the labels test — expect GREEN + compile.**

```bash
./gradlew :feature:settings:client:basic:jvmTest --tests "com.slothiesmooth.nyx.feature.settings.basic.presentation.SettingsLabelsTest"
```

Expected: `BUILD SUCCESSFUL`, 1 test passed.

- [ ] **Step 7: Commit the settings model.**

```bash
git add feature/settings/client/basic/src
git commit -m "feat(settings): about/wipe state, view model, and version label"
```

- [ ] **Step 8: Write `SettingsScreen.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel

private const val NYX_REPO_URL = "https://github.com/slothiesmooth/nyx"

@Composable
fun SettingsScreen(
    onOpenTheme: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val viewModel = koinFeatureViewModel<SettingsViewModel>()
    val uriHandler = LocalUriHandler.current
    SettingsContent(
        state = viewModel.state,
        onOpenTheme = onOpenTheme,
        onOpenLicenses = onOpenLicenses,
        onOpenRepo = { uriHandler.openUri(NYX_REPO_URL) },
        onRequestWipe = viewModel::requestWipe,
        onConfirmWipe = viewModel::confirmWipe,
        onCancelWipe = viewModel::cancelWipe,
    )
}
```

- [ ] **Step 9: Write `SettingsContent.kt`** (about + row cards + wipe confirm dialog + previews):

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxCard
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlay
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxCardVariant
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import com.slothiesmooth.nyx.shared.presentation.state.UiEvent
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private val ContentPadding = 16.dp
private val RowGap = 12.dp
private val RowInnerGap = 12.dp

@Composable
fun SettingsContent(
    state: SettingsState,
    onOpenTheme: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenRepo: () -> Unit,
    onRequestWipe: () -> Unit,
    onConfirmWipe: () -> Unit,
    onCancelWipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NxTopBar(title = "Settings")
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(ContentPadding),
                verticalArrangement = Arrangement.spacedBy(RowGap),
            ) {
                NxText(text = state.versionLabel, style = NxTextStyle.Caption, color = NxTokens.colors.fgMuted)
                SettingsRow(icon = NxIconKind.Palette, label = "Appearance", onClick = onOpenTheme)
                SettingsRow(icon = NxIconKind.Info, label = "Open-source licenses", onClick = onOpenLicenses)
                SettingsRow(icon = NxIconKind.Share, label = "Source code", onClick = onOpenRepo)
                NxButton(text = "Wipe vault", onClick = onRequestWipe, style = NxButtonStyle.Danger, block = true, leadingIcon = NxIconKind.Trash)
            }
        }
        if (state.showWipeConfirm) {
            AlertDialog(
                onDismissRequest = onCancelWipe,
                title = { NxText(text = "Wipe vault?", style = NxTextStyle.Subhead) },
                text = { NxText(text = "This permanently deletes every stored image and its hidden message. This cannot be undone.", style = NxTextStyle.Body) },
                confirmButton = { NxButton(text = "Wipe", onClick = onConfirmWipe, style = NxButtonStyle.Danger) },
                dismissButton = { NxButton(text = "Cancel", onClick = onCancelWipe, style = NxButtonStyle.Ghost) },
            )
        }
        if (state.isWiping) NxProgressOverlay(label = "Wiping vault", modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun SettingsRow(icon: NxIconKind, label: String, onClick: () -> Unit) {
    NxCard(variant = NxCardVariant.Elevated, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(RowInnerGap),
            horizontalArrangement = Arrangement.spacedBy(RowInnerGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NxIcon(kind = icon, tint = NxTokens.colors.brand)
            NxText(text = label, style = NxTextStyle.Body)
        }
    }
}

private class PreviewSettingsState(
    override val versionLabel: String,
    override val showWipeConfirm: Boolean,
    override val isWiping: Boolean,
) : SettingsState {
    override val uiState: UiState = UiState.Ready
    override val uiEvent: Flow<UiEvent> = emptyFlow()
}

@AllThemePreview
@Composable
private fun SettingsContentPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    SettingsContent(PreviewSettingsState("Nyx 1.0.0 · Android", false, false), {}, {}, {}, {}, {}, {})
}

@AllThemePreview
@Composable
private fun SettingsWipeConfirmPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    SettingsContent(PreviewSettingsState("Nyx 1.0.0 · Android", true, false), {}, {}, {}, {}, {}, {})
}
```

- [ ] **Step 10: Write `LicensesScreen.kt` and `LicensesContent.kt`.**

`licenses/LicensesScreen.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.presentation.licenses

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.slothiesmooth.nyx.feature.settings.basic.domain.nyxLicenses

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    LicensesContent(entries = nyxLicenses, onOpenUrl = { url -> uriHandler.openUri(url) }, onBack = onBack)
}
```

`licenses/LicensesContent.kt`:

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic.presentation.licenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxCard
import com.slothiesmooth.nyx.designlibrary.templates.NxDetailTemplate
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxCardVariant
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import com.slothiesmooth.nyx.feature.settings.basic.domain.LicenseEntry
import com.slothiesmooth.nyx.feature.settings.basic.domain.nyxLicenses
import kotlinx.collections.immutable.ImmutableList

private val CardPadding = 12.dp
private val RowGap = 8.dp

@Composable
fun LicensesContent(
    entries: ImmutableList<LicenseEntry>,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NxDetailTemplate(title = "Open-source licenses", onBack = onBack, modifier = modifier) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(RowGap)) {
            items(entries) { entry ->
                NxCard(variant = NxCardVariant.Elevated, onClick = { onOpenUrl(entry.url) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(CardPadding)) {
                        NxText(text = entry.name, style = NxTextStyle.BodyStrong)
                        NxText(text = entry.license, style = NxTextStyle.Caption, color = NxTokens.colors.fgMuted)
                    }
                }
            }
        }
    }
}

@AllThemePreview
@Composable
private fun LicensesContentPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) {
    LicensesContent(entries = nyxLicenses, onOpenUrl = {}, onBack = {})
}
```

- [ ] **Step 11: Verify compile + commit.**

```bash
./gradlew :feature:settings:client:basic:compileKotlinJvm
git add feature/settings/client/basic/src
git commit -m "feat(settings): about screen with wipe dialog and open-source licenses screen"
```

---

### Task 14: `BasicSettingsProvider` + Android `AppInfo` + final wiring + Android verification

**Files:**
- Create: `feature/settings/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/settings/basic/BasicSettingsProvider.kt`
- Create: `androidApp/src/main/kotlin/com/slothiesmooth/nyx/di/AndroidAppInfo.kt`
- Modify: `androidApp/src/main/kotlin/com/slothiesmooth/nyx/di/AndroidPlatformModule.kt` (register `AppInfo`) — path per plan 05; adjust if different
- Modify: `androidApp/build.gradle.kts` (ensure `buildFeatures { buildConfig = true }` + a `versionName`)
- Modify: `client/.../app/AppConfig.kt` (swap settings stub)

**Interfaces:**
- Consumes: `KoinFeatureProvider`, `FeatureContext`, `withDI`; `SettingsFeature`, `SettingsRoute`, `SettingsLicensesRoute`, `ThemeRoute`; Task 12-13 types; `VaultSource`, `VaultFileStore`, `DomainEventBus`, `AppInfo`.
- Produces: `class BasicSettingsProvider(vaultSource, fileStore, eventBus, appInfo) : KoinFeatureProvider(), SettingsFeature`; `class AndroidAppInfo(versionName) : AppInfo`.

**Steps:**

- [ ] **Step 1: Write `BasicSettingsProvider.kt`:**

```kotlin
package com.slothiesmooth.nyx.feature.settings.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.settings.api.SettingsFeature
import com.slothiesmooth.nyx.feature.settings.api.SettingsLicensesRoute
import com.slothiesmooth.nyx.feature.settings.api.SettingsRoute
import com.slothiesmooth.nyx.feature.settings.basic.domain.usecase.WipeVaultUseCase
import com.slothiesmooth.nyx.feature.settings.basic.presentation.SettingsScreen
import com.slothiesmooth.nyx.feature.settings.basic.presentation.SettingsViewModel
import com.slothiesmooth.nyx.feature.settings.basic.presentation.licenses.LicensesScreen
import com.slothiesmooth.nyx.feature.theme.api.ThemeRoute
import com.slothiesmooth.nyx.shared.data.DomainEventBus
import com.slothiesmooth.nyx.shared.data.source.AppInfo
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

class BasicSettingsProvider(
    private val vaultSource: VaultSource,
    private val fileStore: VaultFileStore,
    private val eventBus: DomainEventBus,
    private val appInfo: AppInfo,
) : KoinFeatureProvider(), SettingsFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun Module.onProvideDI() {
        single { vaultSource }
        single { fileStore }
        single { eventBus }
        single { appInfo }
        factoryOf(::WipeVaultUseCase)
        viewModelOf(::SettingsViewModel)
    }

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<SettingsRoute> {
            withDI {
                SettingsScreen(
                    onOpenTheme = { context.pushDestination(ThemeRoute) },
                    onOpenLicenses = { context.pushDestination(SettingsLicensesRoute) },
                )
            }
        }
        builder.composable<SettingsLicensesRoute> {
            withDI { LicensesScreen(onBack = { context.popDestination() }) }
        }
    }
}
```

- [ ] **Step 2: Write `AndroidAppInfo.kt`** (Android platform impl of the `AppInfo` source; other platforms are added in plan 07):

```kotlin
package com.slothiesmooth.nyx.di

import com.slothiesmooth.nyx.shared.data.source.AppInfo

class AndroidAppInfo(override val versionName: String) : AppInfo {
    override val platformName: String = "Android"
}
```

- [ ] **Step 3: Enable BuildConfig + register `AppInfo` in `AndroidPlatformModule`.** In `androidApp/build.gradle.kts` `android { }` add (if not present):

```kotlin
    buildFeatures { buildConfig = true }
    defaultConfig { versionName = "1.0.0" }   // keep if already set by plan 05/10
```

In `AndroidPlatformModule.kt` (the `Module` that plan 05 passes to `initKoin`), add:

```kotlin
    single<com.slothiesmooth.nyx.shared.data.source.AppInfo> {
        com.slothiesmooth.nyx.di.AndroidAppInfo(versionName = com.slothiesmooth.nyx.BuildConfig.VERSION_NAME)
    }
```

- [ ] **Step 4: Swap the settings registration in `AppConfig.kt`:**

```kotlin
    single<com.slothiesmooth.nyx.feature.settings.api.SettingsFeature> {
        com.slothiesmooth.nyx.feature.settings.basic.BasicSettingsProvider(
            vaultSource = get(), fileStore = get(), eventBus = get(), appInfo = get(),
        )
    }
```

- [ ] **Step 5: Confirm the ordered feature list is complete.** In `AppConfig.kt`, verify plan 05's `single<List<Feature>> { listOf(...) }` includes all seven features in nav order, e.g.:

```kotlin
    single<List<com.slothiesmooth.nyx.feature.common.api.Feature>> {
        listOf(
            get<com.slothiesmooth.nyx.feature.splash.api.SplashFeature>(),
            get<com.slothiesmooth.nyx.feature.navigation.api.NavigationFeature>(),
            get<com.slothiesmooth.nyx.feature.theme.api.ThemeFeature>(),
            get<com.slothiesmooth.nyx.feature.vault.api.VaultFeature>(),
            get<com.slothiesmooth.nyx.feature.encrypt.api.EncryptFeature>(),
            get<com.slothiesmooth.nyx.feature.decrypt.api.DecryptFeature>(),
            get<com.slothiesmooth.nyx.feature.settings.api.SettingsFeature>(),
        )
    }
```

If plan 05 already declares this list, leave it; the four product features now resolve to their real `BasicXProvider`s.

- [ ] **Step 6: Compile `:client` and the Android app.**

```bash
./gradlew :client:compileKotlinJvm :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. This proves the whole DI graph (all four real providers + platform sources + `AppInfo`) links on Android.

- [ ] **Step 7: Run the full phase test suite + quality gates.**

```bash
./gradlew \
  :shared:test-support:jvmTest \
  :feature:vault:client:basic:jvmTest \
  :feature:encrypt:client:basic:jvmTest \
  :feature:decrypt:client:basic:jvmTest \
  :feature:settings:client:basic:jvmTest \
  detekt spotlessCheck
```

Expected: `BUILD SUCCESSFUL` — every use-case/repository/VM-logic test green, both UI tests green, detekt maxIssues=0, spotless clean.

- [ ] **Step 8: Manual smoke on an Android device/emulator (the phase deliverable).** Install and drive the golden path:

```bash
./gradlew :androidApp:installDebug
```

Then on the device confirm, in order: (1) launch → splash → Vault tab shows the empty state; (2) tap "Encrypt a message" → pick an image → wizard advances to Message → type a message + matching password → Encrypt → Result shows "Encrypted and saved"; (3) return to Vault → the new tile appears; (4) open the tile → detail shows the image + date → "Decrypt this" → Decrypt screen preloaded with that image → type the password → the plaintext reveals in the code card → Copy works; (5) type a wrong password on a fresh Decrypt → the distinct "Wrong password, or this image has been tampered with." error shows; (6) Settings → version label shows "Nyx 1.0.0 · Android" → "Wipe vault" → confirm dialog → Wipe → Vault returns to the empty state. If every step behaves, the phase deliverable is met.

- [ ] **Step 9: Commit.**

```bash
git add feature/settings/client/basic/src androidApp/src androidApp/build.gradle.kts client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/app/AppConfig.kt
git commit -m "feat(settings): BasicSettingsProvider, Android AppInfo, and final feature DI wiring"
```

---

## Phase completion checklist (Definition of Done)

- [ ] `:shared:presentation` exposes `ByteArray.toImageBitmap()` (androidMain + skikoMain actuals); `:shared:data` exposes `AppInfo`.
- [ ] `:shared:test-support` exposes `FakeVaultSource`, `FakeVaultFileStore`, `FakeShareSource`, `FakeCameraSource`, `FakeImageCodec` (with green self-tests).
- [ ] Vault: `VaultRepository`/`VaultRepositoryImpl` + 7 use cases; list + detail screens; `BasicVaultProvider` wired; repository + row-mapper tests green.
- [ ] Encrypt: `EncryptMessageUseCase` (CapacityExceeded → `AppError.Validation`), `SaveToVaultUseCase` (write-then-row, rollback, `StegoImageStored`), `estimateMaxMessageChars`; wizard screen; `BasicEncryptProvider` wired; domain + validation tests green; encrypt happy-path UI test green.
- [ ] Decrypt: `DecryptMessageUseCase` (distinct `WrongPasswordOrTampered`/`NoHiddenMessage`/`Failure`), `LoadVaultImageBytesUseCase`; reveal screen with copy; `BasicDecryptProvider` wired; domain + mapper tests green; wrong-password UI test green.
- [ ] Settings: `WipeVaultUseCase` (hard delete + `VaultWiped`), OSS license list, about + licenses screens; `BasicSettingsProvider` + Android `AppInfo` wired; wipe + label tests green.
- [ ] `AppConfig.kt` registers all four real `BasicXProvider`s; `:androidApp:assembleDebug` succeeds; the manual golden-path smoke passes; `detekt` maxIssues=0 and `spotlessCheck` clean.
- [ ] `StegoImageStored`/`StegoImageArchived`/`StegoImageRestored`/`StegoImageDeleted`/`VaultWiped` are emitted by the write paths. **No feature listens to `DomainEventBus` in this phase** — vault reads are DB `Flow`s that auto-update, so cross-feature refresh needs no event listener. Events are emitted for future consumers (analytics, widgets, sync); do NOT add listeners now.

## Cross-feature dependency ledger (for the reviewer)

- Data/logic: every feature depends only on `:shared:data` source interfaces + `:crypto`/`:steganography` + its own `api`. No feature `basic` depends on another feature `basic`.
- Navigation routes only (documented exception): `vault.basic → {encrypt.api, decrypt.api}`, `settings.basic → theme.api`. These carry `@Serializable` route objects, no logic.
- `:client` `AppConfig.kt` depends on all feature `basic` modules (to construct providers) — this is the composition root, expected.
