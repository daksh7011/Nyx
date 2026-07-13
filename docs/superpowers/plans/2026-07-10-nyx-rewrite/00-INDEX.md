# Nyx Rewrite — Master Plan Index & Interface Contracts

> **For agentic workers:** This is the index for an 8-plan set. Execute plans in dependency
> order (below). Every plan's Global Constraints implicitly include everything in this file.
> Spec: `docs/superpowers/specs/2026-07-10-nyx-rewrite-design.md`.

## Phase plans and dependency order

| # | Plan file | Delivers | Depends on |
|---|---|---|---|
| 01 | `01-foundation.md` | Gradle 9.4.1 wrapper, version catalog, build-logic conventions, new settings.gradle.kts (old modules dropped from build), detekt+spotless, CI workflow, all module skeletons compile | — |
| 02 | `02-engines.md` | `:steganography` (PixelImage + LSB) and `:crypto` (NyxCrypto AES-GCM) with full acceptance tests | 01 |
| 03 | `03-shared-infra.md` | `:shared:data`, `:shared:presentation`, `:shared:test-support`, `:shared:compose-test-support`, SqlDelight schema + VaultSqlSource in `:client` | 01 |
| 04 | `04-design-library.md` | `:shared:design-library` (Nx tokens/palettes/components/previews) + `:snapshot` paparazzi module | 01 |
| 05 | `05-feature-shell.md` | `:feature:common:{api,koin}` plumbing, navigation/splash/theme features, `:client` DI + App composable + AppViewModel, `:androidApp` runs | 02*, 03, 04 (*for DI registration of engines) |
| 06 | `06-product-features.md` | vault, encrypt, decrypt, settings features end-to-end on Android | 05 |
| 07 | `07-entry-apps.md` | `:desktopApp`, `:webApp`, iosApp scaffold, all platform source-set impls (codec/vault/settings/share/camera actuals) | 06 |
| 08 | `08-teardown.md` | Delete old modules/files, README/CONTRIBUTING rewrite, final CI polish | 07 |

02, 03, 04 are mutually independent (parallelizable after 01).

## Global constraints (verbatim, apply to every task in every plan)

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
- Injected `Clock` only — direct `kotlin.time.Clock.System`/`kotlinx.datetime` system access banned
  outside `SystemClock`.
- No feature-to-feature api dependencies, with ONE narrow documented exception: a feature's
  `:basic` may depend on ANOTHER feature's `:api` module SOLELY to reference that feature's
  `@Serializable` route for navigation (route-only) — allowed: `vault.basic→encrypt.api`,
  `vault.basic→decrypt.api`, `settings.basic→theme.api`. Still NO `basic→basic` deps;
  `DomainEventBus` remains the only cross-feature DATA channel.
- Repository writes return `AppResult`, reads return `Flow`. Use cases = single-purpose classes
  with `operator fun invoke`, `factoryOf`-registered.
- Composables are dumb: no filtering/sorting/mapping/pluralization in UI — VM state exposes
  render-ready values.
- Tests: kotlin.test + kotlinx-coroutines-test + hand-written fakes only. No mockk/kotest/turbine.
- `suspend` end-to-end for crypto (WebCrypto provider is suspend-only; `*Blocking` throws on wasm).
- No `println`; logging via Kermit.
- Commit after every green test cycle (conventional commits).

## Version catalog pins (verified 2026-07-10; deltas from pawdex marked)

```toml
[versions]
agp = "9.2.0"
kotlin = "2.3.21"
compose-multiplatform = "1.10.3"
koin = "4.2.1"
sqldelight = "2.3.2"
kotlinx-coroutines = "1.10.2"
kotlinx-serialization = "1.11.0"
kotlinx-datetime = "0.7.1"          # kotlin.time.Instant is canonical since 0.7.0
kotlinx-collections-immutable = "0.4.0"
jetbrains-navigation = "2.9.2"       # wasmJs published — verified
jetbrains-lifecycle = "2.10.0"       # wasmJs published — verified
coil = "3.4.0"
filekit = "0.13.0"                   # DO NOT bump to 0.14.x (Kotlin 2.4-compiled, drops iosX64)
cryptography = "0.6.0"               # latest stable; needs Kotlin 2.3.0+ — verified
kermit = "2.1.0"                     # wasmJs published — verified (DELTA: pawdex uses napier/kotlin-logging inconsistently)
paparazzi = "2.0.0-alpha05"          # DELTA from pawdex alpha04 — alpha04 FAILS on Gradle 9.4.1 (cashapp/paparazzi#2227, empirically reproduced); alpha05 also adds official AGP-KMP-plugin support
androidx-datastore = "1.2.1"         # android/ios/jvm ONLY — wasm artifact is a TODO() stub that throws at runtime
androidx-activity = "1.13.0"
androidx-core = "1.16.0"
androidx-splashscreen = "1.2.0"
kotlinx-browser = "0.5.0"            # webApp only (localStorage settings impl)
detekt = "1.23.8"                    # verify latest 1.23.x at implementation time
spotless = "7.0.4"                   # verify latest at implementation time
```

SqlDelight drivers: `android-driver` (android), `native-driver` (all iOS), `sqlite-driver`
(desktop JVM + JVM-side tests, `JdbcSqliteDriver`; in-memory via `JdbcSqliteDriver.IN_MEMORY`).
`generateAsync = true` + `synchronous()` schema adapter for sync drivers (pawdex pattern).
No wasm driver in v1 (web-worker-driver 2.3.2 does publish wasmJs — future work).

## Research-verified API facts (do not re-derive; source: research workflow 2026-07-10)

### cryptography-kotlin 0.6.0
- Artifacts (commonMain): `dev.whyoleg.cryptography:cryptography-core:0.6.0` +
  `cryptography-provider-optimal:0.6.0`.
- Provider per target via optimal: jvm+android→JDK, wasmJs→WebCrypto, iOS→CryptoKit-first with
  CommonCrypto fallback. **AES-GCM: CryptoKit YES / PBKDF2: CommonCrypto YES → iOS fully covered
  by optimal; `openssl3-prebuilt` NOT needed (spec §4 delta).** CryptoKit linking requires Xcode
  installed on the mac lane.
- Default `cipher.encrypt(plaintext)` generates fresh random 12-byte IV and PREPENDS it:
  output = `IV || ciphertext || tag`; `decrypt()` splits automatically. Therefore Nyx blob =
  `salt(16) || cipherOutput` ≡ `[salt 16][nonce 12][ct+tag]` from the spec — no
  @DelicateCryptographyApi needed (spec §4 delta: don't use `encryptWithIv`).
- Canonical usage (signatures verified against 0.6.0 source):

```kotlin
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom

val provider = CryptographyProvider.Default
val salt: ByteArray = CryptographyRandom.nextBytes(16)
val keyBytes: ByteArray = provider.get(PBKDF2).secretDerivation(
    digest = SHA256, iterations = 600_000, outputSize = 256.bits, salt = salt,
).deriveSecretToByteArray(password.encodeToByteArray())
val key = provider.get(AES.GCM).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, keyBytes)
val cipher = key.cipher()                       // tagSize default 128.bits
val out = cipher.encrypt(plaintext)             // = iv(12) || ct || tag(16)
val back = cipher.decrypt(out)                  // throws on wrong key/tamper
```

### Image codec (spec §6 decision resolved: skiko + Android expect/actual, NOT korlibs)
- iOS/desktop/wasm: skiko ships with CMP at compile scope — zero added dependency. Do NOT add
  skiko to androidMain (CMP Android has no skiko).
- Skiko decode: `Image.makeFromEncoded(bytes)` → `readPixels` with
  `ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)` → BGRA bytes → ARGB ints.
  Encode: `Image.makeRaster(imageInfo, bgraBytes, rowBytes = w*4).encodeToData(EncodedImageFormat.PNG)!!.bytes`
  (returns `Data?` — null-check).
- Android: `BitmapFactory.Options().apply { inPremultiplied = false }`, `getPixels` → ARGB;
  encode `Bitmap.createBitmap(argb, w, h, ARGB_8888)` + `compress(PNG, 100, stream)`.
- **Premultiplication is the stego killer:** alpha<255 pixels get LSB-corrupted by premul
  round-trips (Android internal storage premultiplies). Contract: `ImageCodec.decode` FORCES
  every pixel opaque (`or 0xFF000000`) before returning — stego covers are always fully opaque
  (spec §5 delta: alpha normalized, not preserved).
- PNG is pixel-lossless everywhere but NOT byte-identical across platforms — never hash encoded
  PNG bytes cross-platform.

### FileKit 0.13.0
- Artifacts: `filekit-core`, `filekit-dialogs`, `filekit-dialogs-compose`, `filekit-coil`
  (all publish android/ios/jvm/wasmJs).
- Source-set traps: `FileKit.filesDir`/`openFileSaver` = nonWebMain (NOT wasm);
  `FileKit.download(bytes, fileName)` = webMain only; `FileKit.openCameraPicker(...)` +
  `rememberCameraPickerLauncher` = mobileMain (android+iOS only). None visible from commonMain
  of a 6-target module → access only from platform source sets / platform Koin modules
  (pawdex pattern: inject `vaultRoot: String` from platform module).
- Picker (commonMain-safe, all targets): `rememberFilePickerLauncher(type = FileKitType.Image,
  mode = FileKitMode.Single) { file -> ... }`; `suspend file.readBytes()`.
- JVM requires `FileKit.init(appId = "Nyx")` at desktop main() start.
- Camera: FileKit's `openCameraPicker` = the chosen camera solution (spec §6 delta: no
  CameraX/AVFoundation hand-rolling; system camera UX; iOS needs NSCameraUsageDescription).
- Bonus available: `FileKit.shareFile(file)` (android/ios), `compressImage`,
  `saveImageToGallery` (nonWeb).

### Settings
- DataStore 1.2.1 `datastore-preferences-core`: android/ios/jvm only. The published wasm
  artifact COMPILES but every factory call is `TODO()` → runtime crash. Wire DataStore through
  a non-wasm intermediate source set; wasmJs gets `LocalStorageSettingsSource` (kotlinx-browser
  localStorage + in-memory StateFlow for observe).

### CMP 1.10.3 entries
- Desktop: `fun main() = application { Window(onCloseRequest = ::exitApplication, title = "Nyx") { App() } }`.
- wasm: `@OptIn(ExperimentalComposeUiApi::class) fun main() { ComposeViewport { App() } }` —
  `CanvasBasedWindow` is `@Deprecated(level = ERROR)` (compile failure). index.html: empty body,
  `<script src="composeApp.js">` matching `wasmJs { outputModuleName = "composeApp" }`;
  styles.css MUST set `html, body { width:100%; height:100%; margin:0; overflow:hidden }`
  (ComposeViewport injects no CSS).
- `compose.resources {}` extension comes from the `org.jetbrains.compose` plugin (project-level
  block, NOT inside `kotlin {}`); `org.jetbrains.kotlin.plugin.compose` is only the compiler plugin.

### build-logic (included build)
- `build-logic/settings.gradle.kts` needs `google()` + `mavenCentral()` + `gradlePluginPortal()`.
- `build-logic/build.gradle.kts` deps (implementation, NOT compileOnly):
  `com.android.tools.build:gradle:9.2.0` (contains application/library/kotlin.multiplatform.library
  plugin ids), `org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21`,
  `org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.3.21`,
  `org.jetbrains.compose:compose-gradle-plugin:1.10.3`.
- AGP-KMP-plugin restrictions: no buildTypes/flavors, no BuildConfig, no view/data binding;
  cannot coexist with com.android.application/library in one module. Plain `com.android.library`
  modules (snapshot module) compile Kotlin WITHOUT `org.jetbrains.kotlin.android` (AGP 9 built-in).
- jvm() + wasmJs() alongside android+ios in one KMP-plugin module: empirically verified working.

### Paparazzi
- 2.0.0-alpha05 required (alpha04 = NoSuchMethodError on Gradle 9.4.1 at report generation).
- Goldens are environment-sensitive (fonts). Record on Linux (dev machine) = matches ubuntu CI.

## Module → package → namespace table

| Module | Kotlin package root | Android namespace |
|---|---|---|
| :crypto | com.slothiesmooth.nyx.crypto | com.slothiesmooth.nyx.crypto |
| :steganography | com.slothiesmooth.nyx.steganography | com.slothiesmooth.nyx.steganography |
| :shared:data | com.slothiesmooth.nyx.shared.data | com.slothiesmooth.nyx.shared.data |
| :shared:presentation | com.slothiesmooth.nyx.shared.presentation | com.slothiesmooth.nyx.shared.presentation |
| :shared:design-library | com.slothiesmooth.nyx.designlibrary | com.slothiesmooth.nyx.designlibrary |
| :shared:design-library:snapshot | com.slothiesmooth.nyx.designlibrary.snapshot | same |
| :shared:test-support | com.slothiesmooth.nyx.shared.testsupport | com.slothiesmooth.nyx.shared.testsupport |
| :shared:compose-test-support | com.slothiesmooth.nyx.shared.composetestsupport | com.slothiesmooth.nyx.shared.composetestsupport |
| :feature:common:client:api | com.slothiesmooth.nyx.feature.common.api | com.slothiesmooth.nyx.feature.common.api |
| :feature:common:client:koin | com.slothiesmooth.nyx.feature.common.koin | com.slothiesmooth.nyx.feature.common.koin |
| :feature:X:client:api | com.slothiesmooth.nyx.feature.X.api | com.slothiesmooth.nyx.feature.X.api |
| :feature:X:client:basic | com.slothiesmooth.nyx.feature.X.basic | com.slothiesmooth.nyx.feature.X.basic |
| :client | com.slothiesmooth.nyx.client | com.slothiesmooth.nyx.client |
| :androidApp | com.slothiesmooth.nyx | com.slothiesmooth.nyx (applicationId) |
| :desktopApp | com.slothiesmooth.nyx.desktop | n/a (pure jvm) |
| :webApp | com.slothiesmooth.nyx.web | n/a (pure wasmJs) |

Features X ∈ {navigation, splash, theme, vault, encrypt, decrypt, settings}.

## INTERFACE CONTRACTS — exact signatures (cross-plan source of truth)

Any plan producing/consuming these MUST use these exact names/types. Structural templates
(BaseViewModel, ViewState, UiState, FeatureProvider chain, FeatureHost, NavController
extensions) are ported from BOTH pawdex AND the same-author, wasm-targeting Baro
(`~/IdeaProjects/Baro`); pawdex source at
`/tmp/claude-1000/-home-slothie-StudioProjects-Nyx/446ab22c-565d-4c13-a434-33c246daab52/scratchpad/pawdex`
— same shape, Nyx packages, `Nx`/`Nyx` prefixes where pawdex uses `Pd`. **Where pawdex and Baro
differ on wasm behavior, Baro wins** (Baro ships wasmJs; pawdex does not).

### :steganography (`com.slothiesmooth.nyx.steganography`)

```kotlin
class PixelImage(val width: Int, val height: Int, val pixels: IntArray) // ARGB, size == width*height

sealed interface StegoEncodeResult {
    data class Success(val images: List<PixelImage>) : StegoEncodeResult
    data class CapacityExceeded(val requiredBits: Long, val availableBits: Long) : StegoEncodeResult
}

class Steganography(
    private val startMarker: String = "@!#",
    private val endMarker: String = "#!@",
) {
    suspend fun encode(images: List<PixelImage>, payload: String): StegoEncodeResult
    suspend fun decode(images: List<PixelImage>): String?   // null = no framed payload found
}
```

### :crypto (`com.slothiesmooth.nyx.crypto`)

```kotlin
sealed interface DecryptResult {
    data class Success(val plaintext: String) : DecryptResult
    data object WrongPasswordOrTampered : DecryptResult
    data class Failure(val reason: String) : DecryptResult
}

interface NyxCrypto {
    suspend fun encrypt(plaintext: String, password: String): String   // Base64 blob
    suspend fun decrypt(blob: String, password: String): DecryptResult
}

class DefaultNyxCrypto(
    private val provider: CryptographyProvider = CryptographyProvider.Default,
) : NyxCrypto
```

Blob: `Base64(salt(16) || cipher.encrypt(plaintext))` where cipher output = `iv(12)||ct||tag(16)`.
Constants: `SALT_SIZE_BYTES = 16`, `PBKDF2_ITERATIONS = 600_000`, `KEY_SIZE_BITS = 256`.
Base64: `kotlin.io.encoding.Base64.Default`.

### :shared:data (`com.slothiesmooth.nyx.shared.data`)

```kotlin
// result/
sealed interface AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>
    data class Err(val cause: AppError) : AppResult<Nothing>
}
sealed interface AppError {
    data object NotFound : AppError
    data class Validation(val message: String) : AppError
    data class Storage(val message: String, val cause: Throwable? = null) : AppError
    data object Permission : AppError
    data class Conflict(val message: String) : AppError
}

// id/
@JvmInline value class StegoImageId(val value: String)
interface IdGenerator { fun newId(): String }
class Uuid4IdGenerator : IdGenerator          // kotlin.uuid.Uuid.random()

// time/  (kotlin.time.Instant — canonical since kotlinx-datetime 0.7.0)
interface Clock {
    fun now(): Instant
    fun today(): LocalDate
    fun zone(): TimeZone
    fun nowLocal(): LocalDateTime
}
class SystemClock : Clock
class FakeClock(var fixed: Instant) : Clock   // lives in :shared:test-support

// event/
sealed interface DomainEvent {
    data class StegoImageStored(val id: StegoImageId) : DomainEvent
    data class StegoImageArchived(val id: StegoImageId) : DomainEvent
    data class StegoImageRestored(val id: StegoImageId) : DomainEvent
    data class StegoImageDeleted(val id: StegoImageId) : DomainEvent
    data object VaultWiped : DomainEvent
}
interface DomainEventBus {
    val events: SharedFlow<DomainEvent>
    suspend fun emit(event: DomainEvent)
}
class DefaultDomainEventBus : DomainEventBus   // MutableSharedFlow(replay = 0, extraBufferCapacity = 64)

// source/ — implemented in :client / platform modules, injected into feature providers
interface ImageCodec {
    suspend fun decode(bytes: ByteArray): AppResult<PixelImage>       // forces alpha opaque
    suspend fun encodePng(image: PixelImage): AppResult<ByteArray>
}
data class StegoImageRecord(
    val id: String, val name: String, val createdAt: String, val updatedAt: String,
    val deletedAt: String?, val isArchived: Boolean,
)
interface VaultSource {                                               // DB metadata
    fun observeActive(): Flow<List<StegoImageRecord>>
    fun observeArchived(): Flow<List<StegoImageRecord>>
    suspend fun getById(id: String): StegoImageRecord?
    suspend fun upsert(record: StegoImageRecord)
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: String)
    suspend fun softDelete(id: String, deletedAt: String)
    suspend fun purgeAll()
    suspend fun countActive(): Int
}
interface VaultFileStore {                                            // PNG bytes
    suspend fun write(id: String, bytes: ByteArray): AppResult<Unit>
    suspend fun read(id: String): AppResult<ByteArray>
    suspend fun delete(id: String): AppResult<Unit>
    suspend fun deleteAll(): AppResult<Unit>
}
interface SettingsSource {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
    fun observeString(key: String): Flow<String?>
}
data class PickedImage(val bytes: ByteArray, val suggestedName: String?)
interface CameraSource {
    val isAvailable: Boolean
    suspend fun capture(): PickedImage?        // null = user cancelled
}
interface ShareSource {
    suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit>   // share sheet / save dialog / download
}
data class PlatformCapabilities(val camera: Boolean, val persistentVault: Boolean)
```

Note: `ImagePicker` is NOT a shared source — FileKit's `rememberFilePickerLauncher` is
commonMain-safe and used directly in feature UI (returns bytes to the VM).

### :shared:presentation (`com.slothiesmooth.nyx.shared.presentation`)

Port pawdex verbatim (same members, same semantics), packages `...presentation.viewmodel/state/navigation`:

```kotlin
@Immutable abstract class BaseViewModel : ViewModel() {
    protected fun async(id: String, force: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job?
    protected fun ui(id: String, force: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job?
    protected fun withState(block: () -> Unit)                 // Main.immediate + Snapshot.withMutableSnapshot
    protected open fun doInit() {}
    protected open fun doBind() {}
    @Composable protected open fun DoBind() {}
    protected open fun doResume() {}
    protected open fun doPause() {}
    protected open fun doDispose() {}
    @Composable fun bind()                                     // wires lifecycle hooks
}
@Stable interface ViewState { val uiState: UiState; val uiEvent: Flow<UiEvent> }
abstract class MutableViewState : ViewState {
    override var uiState: UiState                              // by mutableStateOf(UiState.Ready)
    fun notify(event: UiEvent)
    suspend fun <T : MutableViewState> T.tryCatch(title: String, onTry: suspend T.() -> Unit)
}
sealed interface UiState {
    data object Ready : UiState
    data object Loading : UiState
    data object Blocking : UiState
    data class Error(val title: String, val cause: Throwable?, val onExit: () -> Unit) : UiState
}
interface UiEvent
// navigation/NavControllerExtensions.kt: pushDestination, popDestination, setDestination,
// restoreDestination (Baro/wasm-safe set). NO shared replaceDestination — popUpTo(Any) is
// ambiguous on wasm; FeatureHostContext inlines replace as popUpTo(currentDestination?.route: String).

// util/ByteArrayExtensions.kt — expect/actual, encoded bytes -> Compose ImageBitmap (previews/tiles):
expect fun ByteArray.toImageBitmap(): ImageBitmap
//   androidMain: BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()
//   skikoMain (intermediate source set = iosMain + jvmMain + wasmJsMain, dependsOn(commonMain)):
//     org.jetbrains.skia.Image.makeFromEncoded(this).toComposeImageBitmap()  // skiko ships with CMP
// Requires a skikoMain intermediate source set in :shared:presentation — mirrors the
// DefaultImageCodec skikoMain in :client.
```

### :feature:common:client:api (`com.slothiesmooth.nyx.feature.common.api`)

Port **Baro signatures** (all take `FeatureContext`; wasm-safe): `interface Feature`,
`interface FeatureProvider : Feature { @Composable fun provideContent(context: FeatureContext,
content: @Composable (() -> Unit)); fun provideNavigation(context: FeatureContext, builder:
NavGraphBuilder) }`. `abstract class BaseFeatureProvider : FeatureProvider` IMPLEMENTS
`provideContent` — a `LaunchedEffect(context)` collects an `Action` `MutableSharedFlow`
(`extraBufferCapacity = Int.MAX_VALUE`) into `onReceiveAction(action, context)` — then calls the
abstract template `@Composable fun onProvideContent(context: FeatureContext, content: @Composable
() -> Unit)`. Concrete providers override `onProvideContent` / `open fun onProvideNavigation(context,
builder)`, NEVER `provideContent` / `provideNavigation`. Action plumbing: `protected fun
onSendAction(action: Action)` (tryEmit), `protected open suspend fun onReceiveAction(action: Action,
context: FeatureContext)`, nested `interface Action`. `FeatureHost(...)` walks the provider chain
(each provider wraps the next via `provideContent(context) { FeatureHost(...) }`) then a `NavHost`
calls every `provideNavigation(context, builder)`; `FeatureHostContext(debug, features,
navController) : FeatureContext` is the concrete context. `FeatureContext` (wasm-safe destination
tracking):

```kotlin
@Stable interface FeatureContext {
    fun getCurrentDestinationChanges(): Flow<Int>   // navController.currentBackStackEntryFlow
                                                     //   .mapNotNull { it.destination.id }.distinctUntilChanged()
    fun getCurrentDestination(): Int?               // navController.currentBackStackEntry?.destination?.id
    fun getDestinationId(route: Any): Int           // STUBBED to 0 — no consumer; route::class.serializer()
                                                     //   .generateHashCode() is ambiguous on wasm + needs InternalSerializationApi
    fun pushDestination(route: Any)
    fun popDestination()
    fun setDestination(route: Any)
    fun replaceDestination(route: Any)              // FeatureHostContext inlines it: navigate(route) {
                                                     //   popUpTo(currentDestination?.route: String){inclusive=true}; launchSingleTop=true }
    fun restoreDestination(route: Any)
}
```

### :feature:common:client:koin (`com.slothiesmooth.nyx.feature.common.koin`)

Port pawdex verbatim: `abstract class KoinFeatureProvider : BaseFeatureProvider()` (lazy isolated
`koinApplication`, `@Composable withDI(content)`, `open fun Module.onProvideDI()`),
`@Composable inline fun <reified T : BaseViewModel> koinFeatureViewModel(): T` (resolves in
isolated context + calls `bind()`).

### Feature api modules — interfaces + routes

```kotlin
// feature.navigation.api
interface NavigationFeature : Feature { fun setItems(items: ImmutableList<NavItem>) }
data class NavItem(val route: Any, val label: String, val icon: NxIconKind, val selected: Boolean)

// feature.splash.api
interface SplashFeature : Feature
@Serializable data object SplashRoute

// feature.theme.api
interface ThemeFeature : Feature { val theme: StateFlow<ThemeConfig> ; suspend fun setMode(mode: ThemeMode); suspend fun setPalette(palette: NxPalette) }
enum class ThemeMode { System, Light, Dark }
data class ThemeConfig(val mode: ThemeMode, val darkPalette: NxPalette, val lightPalette: NxPalette)
@Serializable data object ThemeRoute

// feature.vault.api
interface VaultFeature : Feature { fun observeActiveCount(): Flow<Int> }
@Serializable data object VaultRoute
@Serializable data class VaultDetailRoute(val imageId: String)

// feature.encrypt.api
interface EncryptFeature : Feature
@Serializable data object EncryptRoute

// feature.decrypt.api
interface DecryptFeature : Feature
@Serializable data class DecryptRoute(val imageId: String? = null)

// feature.settings.api
interface SettingsFeature : Feature
@Serializable data object SettingsRoute
@Serializable data object SettingsLicensesRoute
```

### Feature basic modules — key domain types

```kotlin
// feature.vault.basic.domain
data class VaultImage(val id: StegoImageId, val name: String, val createdAt: Instant, val isArchived: Boolean)
interface VaultRepository {
    fun observeActive(): Flow<ImmutableList<VaultImage>>
    fun observeArchived(): Flow<ImmutableList<VaultImage>>
    suspend fun imageBytes(id: StegoImageId): AppResult<ByteArray>
    suspend fun archive(id: StegoImageId): AppResult<Unit>
    suspend fun restore(id: StegoImageId): AppResult<Unit>
    suspend fun softDelete(id: StegoImageId): AppResult<Unit>
}
// use cases: ObserveVaultImagesUseCase, ObserveArchivedImagesUseCase, GetImageBytesUseCase,
//            ArchiveImageUseCase, RestoreImageUseCase, DeleteImageUseCase, ShareVaultImageUseCase

// feature.encrypt.basic.domain
class EncryptMessageUseCase(
    private val crypto: NyxCrypto, private val stego: Steganography, private val codec: ImageCodec,
) { suspend operator fun invoke(coverBytes: ByteArray, message: String, password: String): AppResult<ByteArray> }
class SaveToVaultUseCase(
    private val vaultSource: VaultSource, private val fileStore: VaultFileStore,
    private val idGenerator: IdGenerator, private val clock: Clock, private val eventBus: DomainEventBus,
) { suspend operator fun invoke(pngBytes: ByteArray, name: String): AppResult<StegoImageId> }
// name: blank ("") -> auto-generate "nyx-${id.take(8)}.png"; non-blank -> used verbatim (override).
//   EncryptViewModel passes "" (always auto-generates).

// feature.decrypt.basic.domain
sealed interface DecryptOutcome {
    data class Success(val plaintext: String) : DecryptOutcome
    data object NoHiddenMessage : DecryptOutcome
    data object WrongPasswordOrTampered : DecryptOutcome
    data class Failure(val reason: String) : DecryptOutcome
}
class DecryptMessageUseCase(
    private val crypto: NyxCrypto, private val stego: Steganography, private val codec: ImageCodec,
) { suspend operator fun invoke(imageBytes: ByteArray, password: String): DecryptOutcome }

// feature.settings.basic.domain
class WipeVaultUseCase(
    private val vaultSource: VaultSource, private val fileStore: VaultFileStore,
    private val eventBus: DomainEventBus,
) { suspend operator fun invoke(): AppResult<Unit> }   // hard delete: files + rows (bypasses tombstones), then VaultWiped
```

### :shared:design-library (`com.slothiesmooth.nyx.designlibrary`)

```kotlin
// tokens/NxColors.kt — THE raw-ARGB file (27 slots)
@Immutable data class NxColors(
    val bg: Color, val bgElev1: Color, val bgElev2: Color, val bgSunken: Color, val bgInverse: Color,
    val fg: Color, val fgMuted: Color, val fgSubtle: Color, val fgFaint: Color, val fgInverse: Color, val fgOnBrand: Color,
    val brand: Color, val brandSoft: Color, val brandFg: Color,
    val accentViolet: Color, val accentCyan: Color, val accentAmber: Color, val accentRose: Color,
    val border: Color, val borderStrong: Color, val borderBrand: Color, val divider: Color,
    val success: Color, val warning: Color, val danger: Color, val info: Color,
    val codeBg: Color, val codeFg: Color,
)
enum class NxPalette(val displayName: String, val dark: Boolean) {
    Umbra("Umbra", true),        // default dark
    Eclipse("Eclipse", true),    // OLED black
    Dusk("Dusk", true),          // dark violet
    Moonlight("Moonlight", false), // default light
    Dawn("Dawn", false);         // warm light
    val colors: NxColors get() = ...
    companion object { val DefaultDark = Umbra; val DefaultLight = Moonlight }
}
object NxTokens { val colors: NxColors @Composable get; val type: NxType @Composable get;
                  val spacing = NxSpacing; val radius = NxRadius }
@Composable fun NxTheme(palette: NxPalette, content: @Composable () -> Unit)
fun NxColors.toMaterial3ColorScheme(dark: Boolean): ColorScheme    // tokens/NxMaterial3Bridge.kt
// tokens/AllThemePreview.kt: @AllThemePreview multipreview (5 palettes)
// tokens/NxPaletteProvider.kt: PreviewParameterProvider<NxPalette>
enum class NxIconKind { /* Plus, ChevronLeft, ChevronRight, Eye, EyeOff, Lock, Unlock, Image,
    Camera, Share, Trash, Archive, Restore, Copy, Check, Close, Settings, Palette, Info, Warning, Vault */ }
```

Components (each `NxX.kt` + sibling `NxXPreview.kt` with public `NxXSample()` + private
`@AllThemePreview` fun): NxText, NxButton(style: NxButtonStyle{Primary,Soft,Ghost,Danger},
size: NxButtonSize{Regular,Small}, block, leadingIcon), NxIcon/NxIconButton, NxField,
NxPasswordField(visibility toggle), NxChip, NxCard(variant{Elevated,Flat}), NxTopBar(title, sub,
leading, trailing), NxBottomNav(items), NxFab, NxEmptyState(icon, title, body, cta),
NxProgressOverlay(label), NxSectionHeader, NxImageTile(bytes/painter, selected),
NxDetailTemplate(topBar, content), NxFormTemplate, NxWizardTemplate(steps, currentStep).

### :client (`com.slothiesmooth.nyx.client`)

```kotlin
fun initKoin(platformModule: Module): KoinApplication      // DI.kt; stores global _koinApp
fun appModule(platformModule: Module): Module              // app/AppConfig.kt — registers sources,
    // engines (single<NyxCrypto>{DefaultNyxCrypto()}, single{Steganography()}), clock/id/bus,
    // every feature single<XFeature>{BasicXProvider(...)}, ordered single<List<Feature>>
@Composable fun App()                                       // ThemeProvider { FeatureHost(...) }
class AppViewModel(...) : BaseViewModel()                   // start destination + nav items
// SqlDelight: database NyxDb, package com.slothiesmooth.nyx.client.data.sqldelight
// VaultSqlSource(SqlDelightSource) : VaultSource — client implements, wired in appModule
// DefaultImageCodec: expect/actual — androidMain (BitmapFactory), skikoMain intermediate
//   (iosMain+jvmMain+wasmJsMain share skiko impl)
```

SQL schema (client/src/commonMain/sqldelight/.../StegoImage.sq):

```sql
CREATE TABLE stego_image (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    deleted_at TEXT,
    is_archived INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_stego_image_active ON stego_image(is_archived, deleted_at);
```

### Platform modules

```kotlin
// androidApp: AndroidPlatformModule — AndroidSqliteDriver, DataStoreSettingsSource,
//   FileKitVaultFileStore(filesDir), AndroidShareSource(FileProvider), FileKitCameraSource,
//   PlatformCapabilities(camera = true, persistentVault = true)
// client/iosMain: IosPlatformModule — NativeSqliteDriver, DataStore(ios path), FileKit vault,
//   IosShareSource, FileKitCameraSource, capabilities(camera = true, persistentVault = true)
// desktopApp (jvm): DesktopPlatformModule — JdbcSqliteDriver(file), DataStore(jvm path),
//   FileKit vault (after FileKit.init("Nyx")), DesktopShareSource(openFileSaver),
//   capabilities(camera = false, persistentVault = true)
// webApp (wasmJs): WebPlatformModule — InMemoryVaultSource + InMemoryVaultFileStore,
//   LocalStorageSettingsSource, WebShareSource(FileKit.download),
//   capabilities(camera = false, persistentVault = false)
```

## Deviations log (spec → plan, all research-justified)

1. Spec §4: `openssl3-prebuilt` for iOS — NOT needed (optimal covers GCM via CryptoKit).
2. Spec §4: explicit 12-byte nonce — use library default `encrypt()` (auto IV prepend);
   byte layout identical, avoids @DelicateCryptographyApi.
3. Spec §5: "alpha preserved not forced" — REVERSED: codec forces opaque (premultiplication
   corrupts LSBs of alpha<255 pixels on Android/skiko paths).
4. Spec §6 codec choice — skiko + Android expect/actual (not korlibs: single-maintainer risk,
   11-module transitive pull, coordinate migration history).
5. Spec §6 camera — FileKit `openCameraPicker`, not hand-rolled CameraX/AVFoundation.
6. Pawdex paparazzi alpha04 → alpha05 (alpha04 cannot run on Gradle 9.4.1).
7. Logging — Kermit 2.1.0 (spec left open).
8. FeatureProvider chain (Baro, wasm): `provideContent`/`provideNavigation`/`onReceiveAction`
   take `FeatureContext` — `provideContent(context, content)`, NOT pawdex's context-less
   `provideContent(content)`. `BaseFeatureProvider` implements `provideContent` (a
   `LaunchedEffect(context)` collects the `Action` flow into `onReceiveAction(action, context)`)
   and delegates to the abstract `onProvideContent(context, content)`; concrete providers override
   `onProvideContent`/`onProvideNavigation`, never the base `provideContent`/`provideNavigation`.
9. `FeatureContext` destination tracking (Baro, wasm-safe): `getDestinationId(route)` is STUBBED
   to `0` (no consumer) — `route::class.serializer().generateHashCode()` is ambiguous on wasm and
   needs `@InternalSerializationApi`; current-destination is tracked via the nav library's
   `NavDestination.id` (Int) through `getCurrentDestination()`/`getCurrentDestinationChanges()`.
   The shared `replaceDestination` NavController extension is dropped (popUpTo(Any) ambiguous on
   wasm) and inlined in `FeatureHostContext` as `popUpTo(currentDestination?.route: String)`.
10. Cross-feature api deps: narrow ROUTE-ONLY exception to the no-feature-to-feature rule —
    `vault.basic→encrypt.api`, `vault.basic→decrypt.api`, `settings.basic→theme.api` (reference a
    sibling's `@Serializable` route for navigation only). No `basic→basic`; `DomainEventBus` stays
    the only cross-feature data channel. Baro's `:basic` modules likewise depend on sibling `:api`
    modules (dashboard.basic → auth/alerts/explore/log/hosts.api); Nyx narrows this to route-only.
11. `:shared:presentation` gains a `skikoMain` intermediate source set for the expect/actual
    `ByteArray.toImageBitmap()` (androidMain BitmapFactory; skikoMain = iosMain+jvmMain+wasmJsMain
    via skiko) — mirrors the `:client` `DefaultImageCodec` skikoMain. Nyx-internal (Baro has no
    image codec / skikoMain of its own); grounded in Nyx's existing `:client` skiko decision.
12. `SaveToVaultUseCase(pngBytes, name)`: blank `name` → auto-generate `"nyx-${id.take(8)}.png"`;
    non-blank → override. `EncryptViewModel` passes `""`. Added `@Serializable data object
    SettingsLicensesRoute` to `feature.settings.api`.
