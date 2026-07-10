# Nyx Rewrite — Design Spec

**Date:** 2026-07-10
**Status:** Approved by user (all four sections)
**Strategy:** Big-bang greenfield rewrite in-repo, on a feature branch off `develop`. Old modules deleted once the new app builds green. No legacy data compatibility (clean break — confirmed: no active users).

## 1. What Nyx is

Steganography app: hide an AES-encrypted text message inside an image via LSB encoding,
store/share the stego image, decrypt later with the password. Rebuilt from a dead 2020-era
Android/XML app into a Compose Multiplatform app mirroring the pawdex architecture
(https://github.com/daksh7011/pawdex, cloned and fully mapped during design).

### Decisions locked with the user

| Decision | Choice |
|---|---|
| Targets | android + iosX64/iosArm64/iosSimulatorArm64 + jvm (desktop: linux/mac/win) + wasmJs |
| Features | vault, encrypt, decrypt, settings, theme, splash, navigation. No Unsplash. |
| Image sources | FileKit picker (all targets) + camera (CameraX android / AVFoundation iOS only) |
| Namespace | `com.slothiesmooth.nyx` (drops `in.technowolf.nyx` and its keyword-backtick pain) |
| Legacy data | Clean break. New GCM format only. Old CBC blobs unreadable by design. |
| Build tooling | Pawdex architecture + convention plugins + detekt/spotless + GitHub Actions CI (deviation from pawdex, which has none of these) |
| Design system | Full pawdex-style atomic library, `Nx*` prefixed |
| Execution | Big-bang greenfield in-repo (toolchain conflict makes strangler infeasible) |

## 2. Module map

```
:androidApp                          Android entry (Application, single Activity, splashscreen API)
:desktopApp                          Compose Desktop entry (jvm) — Nyx addition vs pawdex
:webApp                              wasmJs browser entry (CanvasBasedWindow) — Nyx addition vs pawdex
:client                              KMP umbrella: global DI (initKoin), AppViewModel, SqlDelight
                                     schema + Sql sources, iOS static framework "App", iosApp/ scaffold
:crypto                              NyxCrypto — cryptography-kotlin AES-GCM + PBKDF2 (see §4)
:steganography                       Pure-Kotlin LSB core, PixelImage in/out, stdlib-only (see §5)
:shared:data                         AppResult/AppError, DomainEventBus, value-class IDs + IdGenerator,
                                     Clock (injected, never Clock.System), source interfaces
                                     (ImageCodec, VaultSource, SettingsSource, CacheSource,
                                     ImageSource, ShareSource), projections
:shared:presentation                 BaseViewModel (snapshot state), ViewState/MutableViewState,
                                     UiState/UiEvent, NavController extensions
:shared:design-library               Nx* atomic design system (see §8)
:shared:design-library:snapshot      Paparazzi golden tests (plain Android library module)
:shared:test-support                 TestSqlDriver expect/actual, FakeClock, DeterministicIdGenerator
:shared:compose-test-support        runFeatureUiTest harness (isolated Koin + fresh in-memory DB)
:feature:common:client:api           Feature, FeatureProvider, BaseFeatureProvider, FeatureHost,
                                     FeatureContext/FeatureHostContext
:feature:common:client:koin          KoinFeatureProvider, koinFeatureViewModel()
:feature:navigation:client:{api,basic}   bottom-nav shell (Vault / Encrypt / Decrypt / Settings)
:feature:splash:client:{api,basic}       logo + start-destination decision
:feature:theme:client:{api,basic}        palette picker, persisted via SettingsSource
:feature:vault:client:{api,basic}        stego image grid, detail, share/export, archive/delete
:feature:encrypt:client:{api,basic}      pick image → message + password → stego PNG → save/share
:feature:decrypt:client:{api,basic}      vault image or fresh pick + password → plaintext reveal
:feature:settings:client:{api,basic}     about, licenses, wipe vault, version
```

Module-graph rules (pawdex hard rules, kept):
- No feature-to-feature `api` dependencies. `DomainEventBus` is the only cross-feature channel;
  cross-feature reads go through projection interfaces in `shared:data` implemented by the owning
  feature and wired in the app module.
- Feature source inversion: features declare source interfaces (e.g. `VaultSource`); `:client`
  owns the single SqlDelight DB and implements them (`VaultSqlSource`), injected into providers
  via the outer app module.
- `:crypto` and `:steganography` are pure engines; features consume them through use cases.

## 3. Build system

- **Versions (pawdex parity):** Gradle 9.4.1, AGP 9.2.0, Kotlin 2.3.21, Compose Multiplatform
  1.10.3, JVM target 21, compileSdk/targetSdk 36, minSdk 24, Koin 4.2.1, SqlDelight 2.3.2
  (generateAsync, sqlite-3-38), Ktor 3.4.3 (catalog presence; no networking feature at launch),
  Coil 3.4.0, kotlinx: coroutines 1.10.2 / serialization 1.11.0 / datetime 0.7.1 /
  collections-immutable 0.4.0, JetBrains navigation-compose 2.9.2 + lifecycle 2.10.0,
  FileKit 0.13.0, Paparazzi 2.0.0-alpha04, cryptography-kotlin 0.6.0. Bump individual entries
  only where a Nyx target (jvm/wasmJs) demands a newer version — verify at plan time.
- **KMP plugin:** `com.android.kotlin.multiplatform.library` (AGP 9) for KMP modules,
  `android {}` configured inside `kotlin {}`, `withHostTestBuilder`/`withDeviceTestBuilder`.
  All KMP modules declare: androidTarget, iosX64, iosArm64, iosSimulatorArm64, jvm, wasmJs;
  `applyDefaultHierarchyTemplate()`. `kotlin.native.ignoreDisabledTargets=true` so Linux dev
  works; iOS compiles only on macOS/CI.
- **Convention plugins** in `build-logic/` (included build) — deliberate deviation from pawdex's
  39 hand-copied build files: `nyx.kmp.library` (targets, hierarchy, opt-ins),
  `nyx.feature.api`, `nyx.feature.basic`, `nyx.compose` (CMP + resources config:
  publicResClass=true, generateResClass=always).
- **Quality gates** (deviation from pawdex-zero): detekt (maxIssues 0, buildUponDefaultConfig,
  NO suppressions as gate-passers — suppression allowed only where the language offers no fix,
  with justification; the single NxColors raw-ARGB file may suppress MagicNumber) + spotless/ktlint
  wired into `check`. `.editorconfig` kept.
- **CI:** GitHub Actions on PR + push to develop: assemble, host tests (`allTests` for
  android host + jvm + wasm where runnable on Linux runners), paparazzi `verifyPaparazzi`,
  detektCheck, spotlessCheck. macOS lane for iOS compile + iosTest added later.
- Root pawdex quirk kept: `subprojects` block adding `-lsqlite3` linkerOpts to native
  TestExecutable binaries (SqlDelight native driver).
- Version catalog is the single version source; typesafe project accessors on.
- Old tree (`app/`, `feature_base/`, `steganography/`, `utils/`, `buildSrc/`, GitLab CI file,
  old workflows) deleted in the final phase after the new app builds and tests green.

## 4. :crypto (from crypto_migration_plan.md, targets widened)

- Library: `dev.whyoleg.cryptography:cryptography-core` + `cryptography-provider-optimal` 0.6.0.
  Providers: JDK (android + desktop jvm), WebCrypto (wasmJs), Apple/OpenSSL3 for iOS.
  Known gotcha from the plan: AES-GCM is NOT guaranteed on the Apple provider — force
  `cryptography-provider-openssl3-prebuilt` in iosMain; prove on macOS before shipping iOS.
- Public API (commonMain, package `com.slothiesmooth.nyx.crypto`):

```kotlin
sealed interface DecryptResult {
    data class Success(val plaintext: String) : DecryptResult
    data object WrongPasswordOrTampered : DecryptResult
    data class Failure(val reason: String) : DecryptResult
}

interface NyxCrypto {
    suspend fun encrypt(plaintext: String, password: String): String
    suspend fun decrypt(blob: String, password: String): DecryptResult
}
```

- Spec: PBKDF2-HMAC-SHA256, 600,000 iterations, 256-bit key; 16-byte fresh salt per encrypt
  (CryptographyRandom); AES-256-GCM, 12-byte fresh nonce; blob = `[salt 16][nonce 12][ct+tag]`;
  `kotlin.io.encoding.Base64`. No AAD. No hand-rolled primitives, no CBC+HMAC composition,
  no `android.util.Base64`/`javax.crypto` in commonMain, no module-level CoroutineScope,
  never return null on failure.
- Acceptance tests (commonTest): round-trip; wrong password → WrongPasswordOrTampered;
  single-byte tamper → WrongPasswordOrTampered; same input twice → different blobs;
  Base64-decoded blob length ≥ 44; cross-target green (android host, jvm, wasm on Linux;
  iOS on macOS later); end-to-end integration with :steganography.
- Open question from plan (carried): PBKDF2 600k latency in-browser via WebCrypto — measure on
  wasm during implementation; if slow, raise with user; never silently drop below OWASP floor.

## 5. :steganography

- Full rewrite, pure Kotlin stdlib, zero platform deps. `PixelImage(width, height,
  pixels: IntArray)` (ARGB ints). LSB 2-bit encoding across R/G/B channels, marker-framed
  payload (UTF-8), multi-image spanning support.
- API: `suspend fun encode(images: List<PixelImage>, payload: String): List<PixelImage>`,
  `suspend fun decode(images: List<PixelImage>): String?`.
- Fixes over old code (required, testable): up-front capacity check with typed error instead
  of silent truncation; no `System.gc()`/`runFinalization`; no class-level CoroutineScope
  (caller owns scope); no detekt suppressions; alpha channel preserved not forced.
- commonTest: round-trip across sizes, capacity-exceeded error, marker-absent → null,
  multi-image spanning, pixel-delta bound (only LSBs change).

## 6. Data layer

- **shared:data types:** `AppResult<T> { Ok, Err(AppError) }`; `AppError { NotFound, Validation,
  Storage, Permission, Conflict }`. Repository writes return `AppResult`, reads return
  `Flow<ImmutableList<T>>`. `@JvmInline value class StegoImageId`; `IdGenerator` (uuid4);
  `Clock` interface + SystemClock/FakeClock — direct `Clock.System` banned.
- **DomainEvents:** `StegoImageStored`, `StegoImageArchived`, `StegoImageDeleted`,
  `VaultWiped`, `ThemeChanged` (extend as needed). `DefaultDomainEventBus(replay=0,
  extraBufferCapacity=64)` outer singleton.
- **SqlDelight** `NyxDb` in :client. Table `stego_image`: `id TEXT PK` (uuid), `name TEXT`,
  `created_at TEXT` (ISO-8601), `updated_at TEXT`, `deleted_at TEXT` (tombstone),
  `is_archived INTEGER` flag, index on `(is_archived, deleted_at)`. Upserts via
  `ON CONFLICT(id) DO UPDATE`. No stored derived values. Drivers: AndroidSqliteDriver /
  NativeSqliteDriver / JVM SqliteDriver; **wasm: none in v1** (see below).
- **Image bytes:** FileKit vault directory (`<files>/stego_vault`), PNG files named by id;
  DB stores metadata only (pawdex FileVault pattern).
- **SettingsSource:** DataStore preferences (android/iOS/desktop), localStorage (wasm).
- **wasm v1 capability cut:** no persistent vault — encrypt/decrypt/export (FileKit download)
  fully functional; vault list is session-only (in-memory VaultSource impl). Capability flag
  drives UI. Revisit with web-worker driver/OPFS later.
- **ImageCodec** interface: `decode(bytes: ByteArray): PixelImage`,
  `encodePng(image: PixelImage): ByteArray` (always PNG out — lossless required; JPEG/PNG in).
  Implementation: expect/actual (android Bitmap/BitmapFactory, jvm ImageIO, iOS CoreGraphics,
  wasm canvas) OR korlibs-image single-dep if it verifies on all five targets at plan time.
  Behind the interface either way.
- **ImageSource** (acquisition): FileKit picker everywhere; camera expect/actual — CameraX
  (android), AVFoundation (iOS); desktop/wasm report `cameraAvailable=false`, UI hides button.
- **ShareSource:** Android share sheet, iOS UIActivityViewController, desktop save dialog,
  wasm browser download.

## 7. Features (DI, navigation, ViewModels)

- **Plumbing, pawdex verbatim:** `Feature` marker → `FeatureProvider { provideContent;
  provideNavigation(context, NavGraphBuilder) }` → `BaseFeatureProvider` (internal nav-action
  `MutableSharedFlow`, `onSendAction`/`onReceiveAction` collected in provideContent
  LaunchedEffect) → `KoinFeatureProvider` (lazy isolated `koinApplication`,
  `withDI { KoinIsolatedContext }`, `Module.onProvideDI()`). `FeatureHost` nests every
  feature's provideContent recursively, renders one Scaffold + NavHost built from all
  provideNavigation calls. `FeatureHostContext` implements push/pop/set/replace/restore
  destination semantics (NavController extensions copied from pawdex shared:presentation).
- **DI, two layers:** outer `initKoin(platformModule)` in :client — registers sources, engines
  (NyxCrypto, Steganography, ImageCodec), Clock/IdGenerator/EventBus, every feature as
  `single<XFeature> { BasicXProvider(...) }`, and ordered `single<List<Feature>>`. Inner:
  each provider re-registers constructor-received outer deps + its own repos/use-cases/VMs in
  `onProvideDI`. Screens resolve via `koinFeatureViewModel()`.
- **Routes:** `@Serializable` classes/objects in feature api `route/` packages; no string routes.
- **VM pattern, pawdex verbatim:** `BaseViewModel` — named-job dedup map, `async(id, force)`
  (Dispatchers.Default) / `ui(id)` (Main), `withState { Snapshot.withMutableSnapshot }`,
  lifecycle hooks doInit/doBind/doResume/doPause/doDispose driven by `@Composable bind()`.
  Per screen: `@Stable interface XState : ViewState` (read-only, ImmutableList) +
  `class XMutableState : MutableViewState(), XState` (`by mutableStateOf`). VM exposes
  `state: XState`; plain public methods, no sealed intents. `UiState`
  Ready/Loading/Blocking/Error + `tryCatch`; one-shot events via `notify(UiEvent)`.
- **Deviation (user's global rule):** composables are genuinely dumb — no filtering/sorting/
  mapping/pluralization in UI; VM state exposes render-ready values (pawdex leaks small logic
  into composables; Nyx does not).
- **Feature specifics:**
  - *navigation:* bottom tabs Vault/Encrypt/Decrypt/Settings; items derived in AppViewModel.
  - *splash:* brief brand screen; start destination = vault (empty-state CTA → encrypt).
  - *theme:* palette list + mode (system/light/dark override), persisted; `ThemeProvider`
    wraps app content in `NxTheme(palette)`.
  - *vault:* grid of stego images (Coil over vault files), detail (share, archive, delete,
    decrypt shortcut), archived section, empty state.
  - *encrypt:* wizard — source pick (picker/camera) → preview → message + password (+ confirm)
    → progress (Blocking UiState) → result (auto-saved to vault on non-wasm, share/export CTA).
    Use cases: `EncryptMessageUseCase` (crypto.encrypt → codec.decode → stego.encode →
    codec.encodePng), `SaveToVaultUseCase`, `ExportImageUseCase`.
  - *decrypt:* source = vault id (route arg) or fresh pick → password → `DecryptUseCase`
    (codec.decode → stego.decode → crypto.decrypt). UX maps DecryptResult: Success → reveal +
    copy; WrongPasswordOrTampered → distinct honest error; Failure → malformed-image error.
  - *settings:* about, OSS licenses, wipe vault (confirmation dialog; hard-delete all vault
    files AND all `stego_image` rows — wipe is the one operation that bypasses tombstones —
    then emit `VaultWiped`), app version.

## 8. Design system — :shared:design-library

- **Tokens:** `NxColors` @Immutable data class (~27 slots mirroring PdColors: bg/bgElev1/
  bgElev2/bgSunken/bgInverse, fg scale, brand triplet, 3-4 accents, border/divider set,
  success/warning/danger/info, code pair). NxColors.kt is the ONLY runtime file with raw ARGB
  literals (annotation args in the multipreview + expected values in token tests are the
  documented exceptions, same as pawdex). `NxSpacing` (s0..s10), `NxRadius` (xs..pill),
  `NxType` (JetBrains Mono, 5 weights via compose resources), `NxShadow`.
- **Palettes:** `NxPalette` enum, dark-first identity (Nyx = goddess of night): Umbra (default
  dark), Eclipse (OLED black), Dusk (dark violet), Moonlight (default light), Dawn (warm
  light). Exact values tuned during implementation; hex table locked in the design-library
  phase, not guessed mid-feature.
- **Delivery:** `staticCompositionLocalOf` LocalNxColors/LocalNxFontFamily; `NxTokens` accessor
  (colors/type/spacing/radius); `NxTheme(palette, content)` provides locals AND wraps
  `MaterialTheme(colorScheme = colors.toMaterial3ColorScheme(isDark))` + Surface — M3 bridge
  file so stock M3 widgets (AlertDialog etc.) match. Composables consume `NxTokens.*` or
  MaterialTheme slots only.
- **Components** (atoms → templates, built as screens demand, ~15 to start): NxText, NxButton
  (Primary/Soft/Ghost/Danger × sizes), NxIcon(+set), NxField + NxPasswordField, NxChip,
  NxCard, NxAvatar/NxImageTile, NxTopBar, NxBottomNav, NxFab, NxEmptyState, NxProgressOverlay,
  NxSectionHeader, NxDetailTemplate, NxFormTemplate, NxWizardTemplate. Style/size enums live
  beside their component. Deeply immutable params; kotlinx ImmutableCollections for all
  collection params.
- **Previews:** every component `NxX.kt` has sibling `NxXPreview.kt`: public `NxXSample()`
  enumerating all visual states + private `@AllThemePreview` fun over
  `@PreviewParameter(NxPaletteProvider)`. `@AllThemePreview` = multipreview stacking one
  @Preview per palette with matching backgroundColor. Feature screens ALSO get previews
  covering their UiState variants (loading/error/success/empty) — deviation from pawdex's
  zero feature previews, per user's global rule.
- **:snapshot module:** plain Android library + Paparazzi (PIXEL_5, SHRINK), one parameterized
  test class per component over `NxPalette.entries`, snapshotting the same `NxXSample()`
  composables. Golden PNGs committed; `verifyPaparazzi` in CI.

## 9. Testing strategy

- Frameworks: kotlin.test assertions + kotlinx-coroutines-test + hand-written fakes ONLY
  (no mockk/kotest/turbine — pawdex parity). Backtick test names.
- :crypto — acceptance suite from §4 (the highest-value tests in the project).
- :steganography — suite from §5 + integration with :crypto (encrypt→encode→decode→decrypt).
- Features — commonTest per feature: use cases + repositories against fake sources; VM tests
  constructing real use cases over fakes (pawdex DogListViewModelTest shape).
- Compose UI tests — `runFeatureUiTest` harness (fresh Koin + in-memory SqlDelight +
  FakeClock @fixed instant + DeterministicIdGenerator) in iosTest source sets (pawdex
  placement) — compile-gated on macOS; critical flows also as androidDeviceTest where worth it.
- Paparazzi snapshots for the design library (visual regression).
- Tests serve purpose, not count: group encrypt/decrypt matrix cases, delete noise.

## 10. Entry apps

- `:androidApp` — Application (`initKoin(androidPlatformModule)`), single ComponentActivity,
  androidx splashscreen, edge-to-edge. Release: minify + proguard. Debug keystore committed
  (pawdex practice).
- `:desktopApp` — `fun main() = application { Window(title = "Nyx") { App() } }`; jvm platform
  module: ImageIO codec, user-home vault dir, DataStore file, no camera.
- `:webApp` — wasmJs `CanvasBasedWindow`; browser platform module: canvas codec, localStorage
  settings, in-memory vault, download export, no camera.
- iOS — :client umbrella exports static framework "App" (MainViewController); `iosApp/`
  Xcode project scaffold committed, unbuildable on Linux, validated on macOS later.
- Logging: real KMP logger — kermit vs kotlin-logging decided at plan time (one line in
  catalog); no println, prod-sanitizable.

## 11. Risks / open items (carried into planning)

1. iOS unverifiable on Linux — targets declared, compile proof deferred to macOS/CI.
   Includes the crypto OpenSSL3-on-iOS proof from the migration plan.
2. wasm: PBKDF2 600k latency (measure, escalate — never silently lower); SqlDelight persistent
   driver punted (v1 export-only vault); CMP wasm maturity.
3. korlibs-image vs expect/actual codec — verify korlibs-image on all 5 targets at plan step 1;
   fall back to expect/actual (interface unchanged either way).
4. AGP 9.2 KMP-library plugin is bleeding-edge (pawdex-proven pattern; pin exact versions,
   expect sharp edges on jvm+wasm targets pawdex doesn't exercise).
5. cryptography-kotlin 0.6.0 API — verify exact signatures against docs before coding
   (migration-plan step 1); do not trust remembered API shapes.
6. Palette hex values — locked during design-library phase with token tests, not invented
   ad hoc per component.

## 12. Deletion list (final phase)

`app/`, `feature_base/`, `steganography/` (old), `utils/`, `buildSrc/`, `.gitlab-ci.yml`,
old `.github/workflows/check.yml`, `detekt.yml` (replaced by new config), old root build
files, `crypto_migration_plan.md` (superseded by this spec + implementation). Entire list
confirmed with user at the final phase before deleting; stray `.ai`/image design assets
touched only with explicit approval. README/CONTRIBUTING rewritten for the new architecture.
