# Feature Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Boot the Nyx Android app end-to-end — splash → bottom-nav shell (Vault / Encrypt / Decrypt / Settings stubs) with live theme switching — on top of the ported pawdex-style feature-host plumbing, isolated-Koin feature providers, and the `:client` DI graph + `App()` composable.

**Architecture:** A single `NavHost` inside one `Scaffold` is built by `FeatureHost`, which recursively nests every registered `FeatureProvider`'s `provideContent` wrapper (index tracked through a `staticCompositionLocalOf`). Each feature owns an isolated `koinApplication` (Koin 4.2.1, `createEagerInstances = false`) that re-registers the outer deps it received plus its own repositories/use-cases/ViewModels; screens resolve via `koinFeatureViewModel()`. Cross-feature navigation never crosses feature-api boundaries — the `:client` app module (which alone imports every feature api) injects target routes into providers as opaque `Any` values, and the navigation feature highlights the active tab from a client-computed `selected` flag (wasm-safe; no runtime route serialization).

**Tech Stack:** Compose Multiplatform 1.10.3, JetBrains navigation-compose 2.9.2 + lifecycle 2.10.0, Koin 4.2.1 (`koin-core`, `koin-compose`, `koin-compose-viewmodel-navigation`), kotlinx-serialization 1.11.0 (type-safe routes), kotlinx-coroutines 1.10.2, kotlinx-collections-immutable 0.4.0, SqlDelight 2.3.2 (android driver), FileKit 0.13.0, androidx-datastore 1.2.1, androidx-activity 1.13.0 + core-splashscreen 1.2.0, skiko (bundled with CMP, non-android codec).

## Global Constraints — copy verbatim from 00-INDEX + phase-specific

These apply to every task (from `00-INDEX.md` § "Global constraints", verbatim):

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

- **Koin isolation:** every feature provider extends `KoinFeatureProvider`; its `koinApplication(createEagerInstances = false)` is lazy and re-registers each outer dependency it was constructed with. Screens resolve ViewModels only through `koinFeatureViewModel()` inside `withDI { }`. The outer graph is started once by `initKoin(platformModule)` (`startKoin`, idempotent for wasm reloads).
- **No cross-feature api imports for navigation.** A feature that must navigate to another feature's route receives that route as an `Any` constructor parameter injected by the `:client` `appModule` (the only module allowed to depend on every feature api). Never add `api(projects.feature.<other>.client.api)` to a feature module.
- **wasm-safe route matching.** Do not compute a destination id or route name from a `route: Any` value at runtime (`route::class.serializer()` is `InternalSerializationApi` and is ambiguous on wasmJs — the reason the reference architecture abandoned it). Tab selection is a `Boolean` on `NavItem`, computed in `:client` where the concrete route types are known at compile time via `routeNameOf<T>()`.
- **detekt maxIssues = 0.** No magic numbers outside `NxColors.kt`; extract `private const val` (constant declarations are exempt from `MagicNumber`). Keep functions small and single-purpose.
- **Assume plans 01–04 already executed.** Every module directory + `build.gradle.kts` skeleton (convention plugins `nyx.kmp.library` / `nyx.feature.api` / `nyx.feature.basic` / `nyx.compose`, KMP targets, detekt) exists and compiles; `settings.gradle.kts` already `include`s every module below. This plan adds source and the module-specific dependency lines each task names.

### Cross-plan dependency note (read once)

This plan consumes, by exact 00-INDEX signature, types delivered by earlier plans:

- **Plan 02 (`:crypto`, `:steganography`):** `interface NyxCrypto`, `class DefaultNyxCrypto()`, `class Steganography()`, `class PixelImage(width, height, pixels: IntArray)`.
- **Plan 03 (`:shared:data`, `:shared:presentation`, `:shared:test-support`, `:client` SqlDelight):** `AppResult`/`AppError`, `StegoImageId`, `IdGenerator`/`Uuid4IdGenerator`, `Clock`/`SystemClock`/`FakeClock`, `DomainEvent`/`DomainEventBus`/`DefaultDomainEventBus`, `ImageCodec`, `StegoImageRecord`, `VaultSource`, `VaultFileStore`, `SettingsSource`, `PickedImage`, `CameraSource`, `ShareSource`, `PlatformCapabilities`, `DeterministicIdGenerator`, in-memory `TestSqlDriver`; `BaseViewModel`, `ViewState`, `MutableViewState`, `UiState`, `UiEvent`; `NavController` extensions `pushDestination`/`popDestination`/`setDestination`/`restoreDestination`; SqlDelight `NyxDb`, `SqlDelightSource`, `VaultSqlSource(SqlDelightSource) : VaultSource` in `:client`.
- **Plan 04 (`:shared:design-library`):** `NxColors`, `NxPalette` (`Umbra`/`Eclipse`/`Dusk`/`Moonlight`/`Dawn`, `DefaultDark = Umbra`, `DefaultLight = Moonlight`, `.colors`, `.displayName`, `.dark`), `NxTokens` (`colors`/`type`/`spacing`/`radius`), `NxTheme(palette, content)`, `NxSpacing.s0..s10`, `NxRadius.xs..pill`, `NxShadow`, `NxTextStyle`, `NxIconKind` (incl. `Vault`, `Lock`, `Unlock`, `Settings`, `Palette`, `Image`, `Check`), `NxText`, `NxIcon`, `NxIconButton`, `NxButton`, `NxCard`/`NxCardVariant`, `NxChip`, `AllThemePreview`, `NxPaletteProvider`. **The two shell components beyond `NxCard` — `NxBottomNav` (+ `NxBottomNavItem`) and `NxEmptyState` (with `ctaText`/`onCta`) — are delivered by Plan 04 Task 12 and Task 16 respectively, in the same `:shared:design-library` module and preview conventions. This plan does NOT re-create them (Task 3 is a SKIP); it consumes them: `com.slothiesmooth.nyx.designlibrary.models.NxBottomNavItem`, `...organisms.NxBottomNav`, `...molecules.NxEmptyState`.**

The single deviation from the abbreviated 00-INDEX prose for the common-api plumbing: the ported `FeatureProvider.provideContent` keeps its `context: FeatureContext` parameter (the recursive host cannot thread navigation/actions without it — the 00-INDEX one-line summary omitted it for brevity; the "ported from pawdex source — same shape" clause governs the exact signature). `FeatureContext.getDestinationId` is dropped (the reference stubbed it to `0`; it is dead once selection is a client-computed flag), and `getCurrentDestinationChanges()` returns route-name `String?`s instead of `Int` ids (wasm-safe). Both are recorded in Open Questions.

---

### Task 1: `:feature:common:client:api` — Feature host plumbing

**Files:**
- Create: `feature/common/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/common/api/Feature.kt`
- Create: `feature/common/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/common/api/FeatureContext.kt`
- Create: `feature/common/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/common/api/FeatureProvider.kt`
- Create: `feature/common/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/common/api/BaseFeatureProvider.kt`
- Create: `feature/common/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/common/api/RouteNames.kt`
- Create: `feature/common/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/common/api/FeatureHost.kt`
- Test: `feature/common/client/api/src/commonTest/kotlin/com/slothiesmooth/nyx/feature/common/api/BaseFeatureProviderTest.kt`

**Interfaces:**
- Consumes (Plan 03): `NavController` extensions `pushDestination(route: Any)`, `popDestination()`, `setDestination(route: Any)`, `restoreDestination(route: Any)` from `com.slothiesmooth.nyx.shared.presentation.navigation`.
- Produces (later tasks + Plan 06 rely on these exact signatures):
  - `interface Feature`
  - `interface FeatureContext { getCurrentDestinationChanges(): Flow<String?>; getCurrentDestination(): String?; pushDestination(route: Any); setDestination(route: Any); replaceDestination(route: Any); restoreDestination(route: Any); popDestination() }`
  - `interface FeatureProvider : Feature { @Composable fun provideContent(context: FeatureContext, content: @Composable () -> Unit); fun provideNavigation(context: FeatureContext, builder: NavGraphBuilder) }`
  - `abstract class BaseFeatureProvider : FeatureProvider` with `@Composable abstract fun onProvideContent(context, content)`, `open fun onProvideNavigation(context, builder)`, `protected open suspend fun onReceiveAction(action: Action, context: FeatureContext)`, `protected fun onSendAction(action: Action): Boolean`, nested `interface Action`.
  - `inline fun <reified T : Any> routeNameOf(): String`
  - `@Composable fun FeatureHost(context: FeatureHostContext, startDestinationProvider: () -> Any?)`
  - `data class FeatureHostContext(debug: Boolean, features: List<Feature>, navController: NavHostController) : FeatureContext`
  - `val LocalFeatureBottomBar: ProvidableCompositionLocal<@Composable () -> Unit>`

**build.gradle.kts** (Plan 01 skeleton applies `nyx.feature.api`; ensure `commonMain.dependencies` include):
```kotlin
api(projects.shared.presentation)
implementation(libs.compose.runtime)
implementation(libs.compose.foundation)
implementation(libs.compose.material3)
implementation(libs.androidx.navigation.compose)
implementation(libs.kotlinx.serialization.core)
implementation(libs.kotlinx.collections.immutable)
```
and `commonTest.dependencies`: `implementation(libs.kotlin.test)`, `implementation(libs.kotlinx.coroutines.test)`.

**Steps:**

- [ ] **Step 1: Write `Feature.kt` and `FeatureContext.kt` and `FeatureProvider.kt`.**

`Feature.kt`:
```kotlin
package com.slothiesmooth.nyx.feature.common.api

import androidx.compose.runtime.Stable

/** Marker for a shell-hosted feature. Bound to its cross-feature interface in the app module. */
@Stable
interface Feature
```

`FeatureContext.kt`:
```kotlin
package com.slothiesmooth.nyx.feature.common.api

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow

/**
 * The navigation surface handed to every feature. Route matching is by serialized route name
 * (String) rather than a destination id — wasm-safe, since `route::class.serializer()` is
 * `InternalSerializationApi` and ambiguous on wasmJs.
 */
@Stable
interface FeatureContext {
    /** Emits the current destination's serialized route name on every back-stack change. */
    fun getCurrentDestinationChanges(): Flow<String?>

    /** The current destination's serialized route name, or null before the graph is ready. */
    fun getCurrentDestination(): String?

    fun pushDestination(route: Any)
    fun setDestination(route: Any)
    fun replaceDestination(route: Any)
    fun restoreDestination(route: Any)
    fun popDestination()
}
```

`FeatureProvider.kt`:
```kotlin
package com.slothiesmooth.nyx.feature.common.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder

interface FeatureProvider : Feature {

    /** Wraps the recursive host content; the outermost providers get to decorate the whole shell. */
    @Composable
    fun provideContent(context: FeatureContext, content: @Composable () -> Unit)

    /** Contributes this feature's type-safe routes to the single shell NavHost. */
    fun provideNavigation(context: FeatureContext, builder: NavGraphBuilder)
}
```

- [ ] **Step 2: Write the failing test `BaseFeatureProviderTest.kt`** (drives the exact `extraBufferCapacity = Int.MAX_VALUE` design decision — with the default 64 buffer and no collector, `tryEmit` starts returning `false`; with an unbounded buffer it never drops):
```kotlin
package com.slothiesmooth.nyx.feature.common.api

import androidx.compose.runtime.Composable
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

private const val FLOOD_COUNT = 10_000

class BaseFeatureProviderTest {

    private class TestProvider : BaseFeatureProvider() {
        @Composable
        override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = Unit

        fun emit(action: Action): Boolean = onSendAction(action)

        object Ping : Action
    }

    @Test
    fun `onSendAction never drops actions thanks to an unbounded buffer`() = runTest {
        val provider = TestProvider()
        repeat(FLOOD_COUNT) { assertTrue(provider.emit(TestProvider.Ping)) }
    }
}
```

- [ ] **Step 3: Run the test — expect COMPILE failure** (`BaseFeatureProvider` does not exist yet):
```bash
./gradlew :feature:common:client:api:compileTestKotlinJvm
```
Expected: `Unresolved reference: BaseFeatureProvider`.

- [ ] **Step 4: Write `BaseFeatureProvider.kt`** (unbounded action bus; `tryEmit` returns whether the emit was accepted):
```kotlin
package com.slothiesmooth.nyx.feature.common.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import kotlinx.coroutines.flow.MutableSharedFlow

abstract class BaseFeatureProvider : FeatureProvider {

    private val actionFlow = MutableSharedFlow<Action>(extraBufferCapacity = Int.MAX_VALUE)

    @Composable
    final override fun provideContent(context: FeatureContext, content: @Composable () -> Unit) {
        LaunchedEffect(context) { actionFlow.collect { action -> onReceiveAction(action, context) } }
        onProvideContent(context, content)
    }

    @Composable
    abstract fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit)

    final override fun provideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        onProvideNavigation(context, builder)
    }

    open fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) = Unit

    protected open suspend fun onReceiveAction(action: Action, context: FeatureContext) = Unit

    /** Enqueues an action for [onReceiveAction]; always accepted (unbounded buffer). */
    protected fun onSendAction(action: Action): Boolean = actionFlow.tryEmit(action)

    interface Action
}
```

- [ ] **Step 5: Run the test — expect PASS:**
```bash
./gradlew :feature:common:client:api:jvmTest
```
Expected: `BUILD SUCCESSFUL`, `BaseFeatureProviderTest` green.

- [ ] **Step 6: Write `RouteNames.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.common.api

import kotlinx.serialization.serializer

/**
 * The serialized route name the navigation library stores in `NavDestination.route` for a
 * no-argument type-safe route. Computed via the reified serializer at the call site (compile-time
 * type known), never from a `route: Any` value at runtime — the latter needs InternalSerializationApi
 * and is ambiguous on wasmJs.
 */
inline fun <reified T : Any> routeNameOf(): String = serializer<T>().descriptor.serialName
```

- [ ] **Step 7: Write `FeatureHost.kt`** (recursive nesting through the index local; single `Scaffold` + `NavHost`; the bottom bar is whatever composable the navigation feature published into `LocalFeatureBottomBar`):
```kotlin
package com.slothiesmooth.nyx.feature.common.api

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.slothiesmooth.nyx.shared.presentation.navigation.popDestination
import com.slothiesmooth.nyx.shared.presentation.navigation.pushDestination
import com.slothiesmooth.nyx.shared.presentation.navigation.restoreDestination
import com.slothiesmooth.nyx.shared.presentation.navigation.setDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val nextFeatureIndex = staticCompositionLocalOf { 0 }

/**
 * The bottom-bar slot for the single shell Scaffold. A feature (the navigation feature) publishes a
 * composable here from inside its [FeatureProvider.provideContent] wrapper; the terminal host reads it.
 * Default is empty, so a shell with no navigation feature (e.g. a splash-only test) has no bottom bar.
 */
val LocalFeatureBottomBar = staticCompositionLocalOf<@Composable () -> Unit> { {} }

/**
 * Recursively nests each registered [FeatureProvider]'s content wrapper (index advanced through
 * [nextFeatureIndex]); once every feature has wrapped, the terminal [FeatureHostContent] draws the
 * one Scaffold + NavHost from all features' `provideNavigation` contributions.
 */
@Composable
fun FeatureHost(context: FeatureHostContext, startDestinationProvider: () -> Any?) {
    val index = nextFeatureIndex.current
    val feature = remember(index) { context.features.getOrNull(index) as? FeatureProvider }
    if (feature != null) {
        CompositionLocalProvider(nextFeatureIndex provides index + 1) {
            feature.provideContent(context) {
                FeatureHost(context, startDestinationProvider)
            }
        }
    } else {
        FeatureHostContent(context, startDestinationProvider)
    }
}

@Composable
private fun FeatureHostContent(context: FeatureHostContext, startDestinationProvider: () -> Any?) {
    val startDestination = startDestinationProvider() ?: return
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = LocalFeatureBottomBar.current,
    ) { paddings ->
        NavHost(
            modifier = Modifier.fillMaxSize().padding(paddings).consumeWindowInsets(paddings),
            startDestination = startDestination,
            contentAlignment = Alignment.Center,
            navController = context.navController,
        ) {
            context.features.forEach { registered ->
                (registered as? FeatureProvider)?.provideNavigation(context, this)
            }
        }
    }
}

/**
 * The concrete [FeatureContext]. `replaceDestination` is inlined (not a shared nav verb) because its
 * `popUpTo(route: Any)` was ambiguous on wasmJs; `currentDestination?.route` is a String, so
 * `popUpTo(String)` is safe.
 */
data class FeatureHostContext(
    internal val debug: Boolean,
    internal val features: List<Feature>,
    internal val navController: NavHostController,
) : FeatureContext {

    override fun getCurrentDestinationChanges(): Flow<String?> = navController.currentBackStackEntryFlow
        .map { entry -> entry.destination.route }
        .distinctUntilChanged()

    override fun getCurrentDestination(): String? = navController.currentBackStackEntry?.destination?.route

    override fun replaceDestination(route: Any) {
        navController.navigate(route) {
            navController.currentDestination?.route?.let { current -> popUpTo(current) { inclusive = true } }
            launchSingleTop = true
        }
    }

    override fun restoreDestination(route: Any) = navController.restoreDestination(route)

    override fun setDestination(route: Any) = navController.setDestination(route)

    override fun pushDestination(route: Any) = navController.pushDestination(route)

    override fun popDestination() = navController.popDestination()
}
```

- [ ] **Step 8: Compile all targets that build on Linux:**
```bash
./gradlew :feature:common:client:api:compileKotlinJvm :feature:common:client:api:compileKotlinWasmJs
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit:**
```bash
git add feature/common/client/api/src && git commit -m "feat(feature-common): port feature host plumbing (Feature/Provider/Host/Context)"
```

---

### Task 2: `:feature:common:client:koin` — isolated-Koin provider base

**Files:**
- Create: `feature/common/client/koin/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/common/koin/KoinFeatureProvider.kt`
- Create: `feature/common/client/koin/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/common/koin/KoinFeatureViewModel.kt`

**Interfaces:**
- Consumes: `BaseFeatureProvider` (Task 1), `BaseViewModel` (Plan 03, `com.slothiesmooth.nyx.shared.presentation.viewmodel`).
- Produces:
  - `abstract class KoinFeatureProvider : BaseFeatureProvider()` with `protected val koinApp: KoinApplication` (lazy, `createEagerInstances = false`), `@Composable protected fun withDI(context: KoinApplication? = null, content: @Composable () -> Unit)`, `protected open fun Module.onProvideDI()`.
  - `@Composable inline fun <reified T : BaseViewModel> koinFeatureViewModel(key: String? = null): T`.

**build.gradle.kts** (`nyx.feature.api`; `commonMain.dependencies`):
```kotlin
api(projects.feature.common.client.api)
api(projects.shared.presentation)
implementation(libs.compose.runtime)
implementation(libs.koin.core)
implementation(libs.koin.compose)
implementation(libs.koin.compose.viewmodel.navigation)
```

**Steps:**

- [ ] **Step 1: Write `KoinFeatureProvider.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.common.koin

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import org.koin.compose.KoinIsolatedContext
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * A feature provider with its own isolated Koin graph. Subclasses re-register (in [onProvideDI]) each
 * outer dependency they received in their constructor plus their own repositories/use-cases/ViewModels;
 * screens run inside [withDI] and resolve through [koinFeatureViewModel].
 */
abstract class KoinFeatureProvider : BaseFeatureProvider() {

    protected val koinApp: KoinApplication by lazy {
        koinApplication(createEagerInstances = false) { modules(module { onProvideDI() }) }
    }

    @Composable
    protected fun withDI(context: KoinApplication? = null, content: @Composable () -> Unit) {
        KoinIsolatedContext(context ?: koinApp, content)
    }

    protected open fun Module.onProvideDI() = Unit
}
```

- [ ] **Step 2: Write `KoinFeatureViewModel.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.common.koin

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Resolves a feature ViewModel from the isolated graph and wires its lifecycle via [BaseViewModel.bind]. */
@Composable
inline fun <reified T : BaseViewModel> koinFeatureViewModel(key: String? = null): T {
    val viewModel = koinViewModel<T>(key = key)
    viewModel.bind()
    return viewModel
}
```

- [ ] **Step 3: Compile:**
```bash
./gradlew :feature:common:client:koin:compileKotlinJvm :feature:common:client:koin:compileKotlinWasmJs
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit:**
```bash
git add feature/common/client/koin/src && git commit -m "feat(feature-common): KoinFeatureProvider + koinFeatureViewModel"
```

---

### Task 3: `:shared:design-library` — SKIP (NxBottomNav/NxEmptyState delivered by Plan 04)

**SKIP — create no files in this task.** `NxBottomNav` and `NxBottomNavItem` are delivered by Plan 04 Task 12; `NxEmptyState` (with `ctaText`/`onCta`) is delivered by Plan 04 Task 16. Do not create these files here. This plan runs *after* Plan 04, so re-creating them would overwrite Plan 04's `NxEmptyState` — whose CTA parameter is `ctaText`, not `ctaLabel` — and break Plan 06's `ctaText =` call sites. Downstream consumers in this plan already resolve against Plan 04's packages: Task 7 (`BasicNavigationProvider`) imports `com.slothiesmooth.nyx.designlibrary.models.NxBottomNavItem` and `...organisms.NxBottomNav`; Task 10 stubs import `...molecules.NxEmptyState` and pass `ctaText`.

---

### Task 4: `:shared:test-support` — FakeSettingsSource

**Files:**
- Create: `shared/test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/FakeSettingsSource.kt`

**Interfaces:**
- Consumes (Plan 03): `interface SettingsSource { suspend fun getString(key: String): String?; suspend fun putString(key: String, value: String); fun observeString(key: String): Flow<String?> }`.
- Produces: `class FakeSettingsSource(initial: Map<String, String> = emptyMap()) : SettingsSource` — in-memory, `StateFlow`-backed so `observeString` emits live changes.

**build.gradle.kts** (`nyx.kmp.library`; `commonMain.dependencies`): `api(projects.shared.data)`, `implementation(libs.kotlinx.coroutines.core)`. (It is a *main* source set — feature `commonTest`s consume it as a normal fake.)

**Steps:**

- [ ] **Step 1: Write `FakeSettingsSource.kt`:**
```kotlin
package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.source.SettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [SettingsSource] whose [observeString] emits live changes. Test doubles only. */
class FakeSettingsSource(initial: Map<String, String> = emptyMap()) : SettingsSource {

    private val store = MutableStateFlow(initial.toMap())

    override suspend fun getString(key: String): String? = store.value[key]

    override suspend fun putString(key: String, value: String) {
        store.update { current -> current + (key to value) }
    }

    override fun observeString(key: String): Flow<String?> =
        store.map { current -> current[key] }.distinctUntilChanged()
}
```
> If Plan 03 placed `SettingsSource` in a different sub-package than `...shared.data.source`, correct the import to match the delivered contract.

- [ ] **Step 2: Compile:**
```bash
./gradlew :shared:test-support:compileKotlinJvm :shared:test-support:compileKotlinWasmJs
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit:**
```bash
git add shared/test-support/src && git commit -m "feat(test-support): FakeSettingsSource with live observe"
```

---

### Task 5: `:feature:theme:client:api` — ThemeFeature contract + route

**Files:**
- Create: `feature/theme/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/theme/api/ThemeFeature.kt`
- Create: `feature/theme/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/theme/api/ThemeRoute.kt`

**Interfaces:**
- Consumes: `Feature` (Task 1), `NxPalette` (Plan 04).
- Produces (00-INDEX contract — exact):
  - `interface ThemeFeature : Feature { val theme: StateFlow<ThemeConfig>; suspend fun setMode(mode: ThemeMode); suspend fun setPalette(palette: NxPalette) }`
  - `enum class ThemeMode { System, Light, Dark }`
  - `data class ThemeConfig(val mode: ThemeMode, val darkPalette: NxPalette, val lightPalette: NxPalette)`
  - `@Serializable data object ThemeRoute`

**build.gradle.kts** (`nyx.feature.api`; `commonMain.dependencies`):
```kotlin
api(projects.feature.common.client.api)
api(projects.shared.designLibrary)
implementation(libs.kotlinx.serialization.core)
implementation(libs.kotlinx.coroutines.core)
```
> `NxPalette` appears in the public `ThemeConfig`/`ThemeFeature` surface, so `:shared:design-library` is an `api` dependency here (the one design-library reference from a feature *api* module in the whole tree).

**Steps:**

- [ ] **Step 1: Write `ThemeFeature.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.theme.api

import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.common.api.Feature
import kotlinx.coroutines.flow.StateFlow

/** How the effective palette is chosen: follow the OS, or force one side. */
enum class ThemeMode { System, Light, Dark }

/** The persisted theme selection: the [mode] plus the chosen palette for each side. */
data class ThemeConfig(
    val mode: ThemeMode,
    val darkPalette: NxPalette,
    val lightPalette: NxPalette,
)

/**
 * Cross-feature handle to the app theme. The shell observes [theme] to wrap content in `NxTheme`;
 * the settings/change-theme screen calls [setMode]/[setPalette]. [setPalette] routes the palette to
 * its own side (dark palette → dark slot, light palette → light slot).
 */
interface ThemeFeature : Feature {
    val theme: StateFlow<ThemeConfig>
    suspend fun setMode(mode: ThemeMode)
    suspend fun setPalette(palette: NxPalette)
}
```

- [ ] **Step 2: Write `ThemeRoute.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.theme.api

import kotlinx.serialization.Serializable

/** The change-theme screen route. */
@Serializable
data object ThemeRoute
```

- [ ] **Step 3: Compile:**
```bash
./gradlew :feature:theme:client:api:compileKotlinJvm :feature:theme:client:api:compileKotlinWasmJs
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit:**
```bash
git add feature/theme/client/api/src && git commit -m "feat(theme): ThemeFeature contract, ThemeConfig/ThemeMode, ThemeRoute"
```

---

### Task 6: `:feature:theme:client:basic` — repository, provider, ThemeProvider, screen, VM

**Files:**
- Create: `feature/theme/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/theme/basic/ThemeRepository.kt`
- Create: `feature/theme/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/theme/basic/BasicThemeProvider.kt`
- Create: `feature/theme/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/theme/basic/ThemeProvider.kt`
- Create: `feature/theme/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/theme/basic/ChangeThemeState.kt`
- Create: `feature/theme/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/theme/basic/ThemeViewModel.kt`
- Create: `feature/theme/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/theme/basic/ChangeThemeScreen.kt`
- Create: `feature/theme/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/theme/basic/previews/ChangeThemePreviews.kt`
- Test: `feature/theme/client/basic/src/commonTest/kotlin/com/slothiesmooth/nyx/feature/theme/basic/ThemeRepositoryTest.kt`
- Test: `feature/theme/client/basic/src/commonTest/kotlin/com/slothiesmooth/nyx/feature/theme/basic/ThemeViewModelTest.kt`

**Interfaces:**
- Consumes: `SettingsSource` (Plan 03), `NxPalette`/`NxTokens`/`NxTheme`/`NxCard`/`NxCardVariant`/`NxChip`/`NxText`/`NxTextStyle`/`NxSpacing` (Plan 04), `ThemeFeature`/`ThemeConfig`/`ThemeMode`/`ThemeRoute` (Task 5), `KoinFeatureProvider`/`koinFeatureViewModel` (Task 2), `BaseFeatureProvider`/`FeatureContext` (Task 1), `BaseViewModel`/`ViewState`/`MutableViewState` (Plan 03), `FakeSettingsSource` (Task 4, tests only).
- Produces:
  - `class ThemeRepository(settings: SettingsSource)` with `fun observeConfig(): Flow<ThemeConfig>`, `suspend fun setMode(mode: ThemeMode)`, `suspend fun setPalette(palette: NxPalette)`.
  - `class BasicThemeProvider(repository: ThemeRepository, scope: CoroutineScope) : KoinFeatureProvider(), ThemeFeature`.
  - `@Composable fun ThemeProvider(themeFeature: ThemeFeature, content: @Composable () -> Unit)`.
  - `interface ChangeThemeState : ViewState` + `class ChangeThemeMutableState`, `class ThemeViewModel`.

**build.gradle.kts** (`nyx.feature.basic`; `commonMain.dependencies`):
```kotlin
api(projects.feature.theme.client.api)
api(projects.feature.common.client.api)
api(projects.feature.common.client.koin)
api(projects.shared.presentation)
api(projects.shared.data)
implementation(projects.shared.designLibrary)
implementation(libs.compose.runtime)
implementation(libs.compose.foundation)
implementation(libs.compose.material3)
implementation(libs.compose.preview)
implementation(libs.androidx.navigation.compose)
implementation(libs.koin.core)
implementation(libs.koin.compose.viewmodel.navigation)
implementation(libs.kotlinx.collections.immutable)
```
and `commonTest.dependencies`: `implementation(libs.kotlin.test)`, `implementation(libs.kotlinx.coroutines.test)`, `implementation(projects.shared.testSupport)`.

**Steps:**

- [ ] **Step 1: Write the failing test `ThemeRepositoryTest.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.theme.basic

import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.shared.testsupport.FakeSettingsSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeRepositoryTest {

    @Test
    fun `defaults to System mode with Umbra dark and Moonlight light`() = runTest {
        val repository = ThemeRepository(FakeSettingsSource())
        val config = repository.observeConfig().first()
        assertEquals(ThemeMode.System, config.mode)
        assertEquals(NxPalette.Umbra, config.darkPalette)
        assertEquals(NxPalette.Moonlight, config.lightPalette)
    }

    @Test
    fun `setMode persists and is observed`() = runTest {
        val repository = ThemeRepository(FakeSettingsSource())
        repository.setMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, repository.observeConfig().first().mode)
    }

    @Test
    fun `setPalette routes a dark palette to the dark slot only`() = runTest {
        val repository = ThemeRepository(FakeSettingsSource())
        repository.setPalette(NxPalette.Eclipse)
        val config = repository.observeConfig().first()
        assertEquals(NxPalette.Eclipse, config.darkPalette)
        assertEquals(NxPalette.Moonlight, config.lightPalette)
    }

    @Test
    fun `setPalette routes a light palette to the light slot only`() = runTest {
        val repository = ThemeRepository(FakeSettingsSource())
        repository.setPalette(NxPalette.Dawn)
        val config = repository.observeConfig().first()
        assertEquals(NxPalette.Dawn, config.lightPalette)
        assertEquals(NxPalette.Umbra, config.darkPalette)
    }
}
```

- [ ] **Step 2: Run — expect COMPILE failure:**
```bash
./gradlew :feature:theme:client:basic:compileTestKotlinJvm
```
Expected: `Unresolved reference: ThemeRepository`.

- [ ] **Step 3: Write `ThemeRepository.kt`** (keys `theme.mode`/`theme.dark`/`theme.light`; an id in the wrong slot or an unknown id falls back to the default for that slot):
```kotlin
package com.slothiesmooth.nyx.feature.theme.basic

import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.theme.api.ThemeConfig
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.shared.data.source.SettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val KEY_MODE = "theme.mode"
private const val KEY_DARK = "theme.dark"
private const val KEY_LIGHT = "theme.light"

/** Persists and observes the [ThemeConfig] behind a [SettingsSource]. */
class ThemeRepository(private val settings: SettingsSource) {

    fun observeConfig(): Flow<ThemeConfig> = combine(
        settings.observeString(KEY_MODE),
        settings.observeString(KEY_DARK),
        settings.observeString(KEY_LIGHT),
    ) { mode, dark, light ->
        ThemeConfig(
            mode = parseMode(mode),
            darkPalette = parsePalette(dark, dark = true),
            lightPalette = parsePalette(light, dark = false),
        )
    }

    suspend fun setMode(mode: ThemeMode) = settings.putString(KEY_MODE, mode.name)

    suspend fun setPalette(palette: NxPalette) =
        settings.putString(if (palette.dark) KEY_DARK else KEY_LIGHT, palette.name)

    private fun parseMode(raw: String?): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.System

    private fun parsePalette(raw: String?, dark: Boolean): NxPalette {
        val fallback = if (dark) NxPalette.DefaultDark else NxPalette.DefaultLight
        val parsed = NxPalette.entries.firstOrNull { it.name == raw } ?: return fallback
        return if (parsed.dark == dark) parsed else fallback
    }
}
```
> If Plan 03 placed `SettingsSource` outside `...shared.data.source`, adjust the import.

- [ ] **Step 4: Run — expect PASS:**
```bash
./gradlew :feature:theme:client:basic:jvmTest
```
Expected: `ThemeRepositoryTest` green (`ThemeViewModelTest` not written yet — its file does not exist, so it will not fail the run).

- [ ] **Step 5: Write `ChangeThemeState.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.theme.basic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.state.ViewState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Read-only change-theme state the screen observes. */
interface ChangeThemeState : ViewState {
    val mode: ThemeMode
    val darkPalette: NxPalette
    val lightPalette: NxPalette
    val palettes: ImmutableList<NxPalette>
}

/** Mutable backing state, owned by [ThemeViewModel] and supplied via Koin. */
class ChangeThemeMutableState : MutableViewState(), ChangeThemeState {
    override var mode: ThemeMode by mutableStateOf(ThemeMode.System)
    override var darkPalette: NxPalette by mutableStateOf(NxPalette.DefaultDark)
    override var lightPalette: NxPalette by mutableStateOf(NxPalette.DefaultLight)
    override val palettes: ImmutableList<NxPalette> = NxPalette.entries.toImmutableList()
}
```

- [ ] **Step 6: Write the failing test `ThemeViewModelTest.kt`** (a fake `ThemeFeature` over the repository, so the VM's observe + delegate paths are exercised without Compose):
```kotlin
package com.slothiesmooth.nyx.feature.theme.basic

import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.theme.api.ThemeConfig
import com.slothiesmooth.nyx.feature.theme.api.ThemeFeature
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeThemeFeature : ThemeFeature {
    private val config = MutableStateFlow(ThemeConfig(ThemeMode.System, NxPalette.Umbra, NxPalette.Moonlight))
    override val theme: StateFlow<ThemeConfig> = config
    override suspend fun setMode(mode: ThemeMode) {
        config.value = config.value.copy(mode = mode)
    }
    override suspend fun setPalette(palette: NxPalette) {
        config.value = if (palette.dark) {
            config.value.copy(darkPalette = palette)
        } else {
            config.value.copy(lightPalette = palette)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `selecting a mode delegates to the feature and reflects in state`() = runTest {
        val feature = FakeThemeFeature()
        val viewModel = ThemeViewModel(ChangeThemeMutableState(), feature)
        viewModel.observeTheme()
        viewModel.onModeSelected(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, feature.theme.value.mode)
        assertEquals(ThemeMode.Dark, viewModel.state.mode)
    }

    @Test
    fun `selecting a palette delegates to the feature`() = runTest {
        val feature = FakeThemeFeature()
        val viewModel = ThemeViewModel(ChangeThemeMutableState(), feature)
        viewModel.observeTheme()
        viewModel.onPaletteSelected(NxPalette.Eclipse)
        assertEquals(NxPalette.Eclipse, feature.theme.value.darkPalette)
        assertEquals(NxPalette.Eclipse, viewModel.state.darkPalette)
    }
}
```

- [ ] **Step 7: Run — expect COMPILE failure** (`ThemeViewModel` missing):
```bash
./gradlew :feature:theme:client:basic:compileTestKotlinJvm
```
Expected: `Unresolved reference: ThemeViewModel`.

- [ ] **Step 8: Write `ThemeViewModel.kt`** (`observeTheme()` is the directly-testable seam `doBind` calls — the pattern the reference uses to keep VM logic unit-testable off the Compose lifecycle):
```kotlin
package com.slothiesmooth.nyx.feature.theme.basic

import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.theme.api.ThemeFeature
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel

/** Drives the change-theme screen: mirrors the feature's [ThemeFeature.theme] and forwards choices. */
class ThemeViewModel(
    private val mutableState: ChangeThemeMutableState,
    private val themeFeature: ThemeFeature,
) : BaseViewModel() {

    val state: ChangeThemeState = mutableState

    override fun doBind() {
        observeTheme()
    }

    /** Collects the theme into state. Exposed (not private) so unit tests drive it without Compose. */
    fun observeTheme() = async("observeTheme") {
        themeFeature.theme.collect { config ->
            withState {
                mutableState.mode = config.mode
                mutableState.darkPalette = config.darkPalette
                mutableState.lightPalette = config.lightPalette
            }
        }
    }

    fun onModeSelected(mode: ThemeMode) = async("setMode", force = true) { themeFeature.setMode(mode) }

    fun onPaletteSelected(palette: NxPalette) =
        async("setPalette", force = true) { themeFeature.setPalette(palette) }
}
```

- [ ] **Step 9: Run — expect PASS:**
```bash
./gradlew :feature:theme:client:basic:jvmTest
```
Expected: `ThemeRepositoryTest` + `ThemeViewModelTest` green.

- [ ] **Step 10: Write `ThemeProvider.kt`** (the shell theme wrapper — picks the effective palette and applies `NxTheme`):
```kotlin
package com.slothiesmooth.nyx.feature.theme.basic

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.theme.api.ThemeConfig
import com.slothiesmooth.nyx.feature.theme.api.ThemeFeature
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode

/** Wraps [content] in `NxTheme` using the palette chosen by the live [ThemeFeature.theme]. */
@Composable
fun ThemeProvider(themeFeature: ThemeFeature, content: @Composable () -> Unit) {
    val config by themeFeature.theme.collectAsState()
    val systemDark = isSystemInDarkTheme()
    NxTheme(palette = effectivePalette(config, systemDark), content = content)
}

private fun effectivePalette(config: ThemeConfig, systemDark: Boolean): NxPalette = when (config.mode) {
    ThemeMode.System -> if (systemDark) config.darkPalette else config.lightPalette
    ThemeMode.Dark -> config.darkPalette
    ThemeMode.Light -> config.lightPalette
}
```

- [ ] **Step 11: Write `ChangeThemeScreen.kt`** (mode chips + palette grid of `NxCard`s; all render-ready values come from state — the composable does no logic):
```kotlin
package com.slothiesmooth.nyx.feature.theme.basic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.molecules.NxCard
import com.slothiesmooth.nyx.designlibrary.molecules.NxCardVariant
import com.slothiesmooth.nyx.designlibrary.atoms.NxChip
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode

/** The palette + mode picker. Selection state and every choice callback come from [ThemeViewModel]. */
@Composable
fun ChangeThemeScreen(viewModel: ThemeViewModel) {
    val state = viewModel.state
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(NxSpacing.s4),
        verticalArrangement = Arrangement.spacedBy(NxSpacing.s4),
    ) {
        NxText(text = "Appearance", style = NxTextStyle.Title)

        NxText(text = "Mode", style = NxTextStyle.Kicker, color = NxTokens.colors.fgSubtle)
        Row(horizontalArrangement = Arrangement.spacedBy(NxSpacing.s2)) {
            ThemeMode.entries.forEach { mode ->
                NxChip(
                    text = mode.name,
                    selected = state.mode == mode,
                    onClick = { viewModel.onModeSelected(mode) },
                )
            }
        }

        NxText(text = "Palette", style = NxTextStyle.Kicker, color = NxTokens.colors.fgSubtle)
        state.palettes.forEach { palette ->
            val selected = palette == state.darkPalette || palette == state.lightPalette
            NxCard(
                modifier = Modifier.fillMaxWidth(),
                variant = if (selected) NxCardVariant.Elevated else NxCardVariant.Flat,
                onClick = { viewModel.onPaletteSelected(palette) },
            ) {
                NxText(text = palette.displayName, style = NxTextStyle.BodyStrong)
                NxText(
                    text = if (palette.dark) "Dark" else "Light",
                    style = NxTextStyle.Caption,
                    color = NxTokens.colors.fgSubtle,
                )
            }
        }
    }
}
```

- [ ] **Step 12: Write `BasicThemeProvider.kt`** (implements `ThemeFeature`; exposes the shared `theme` StateFlow via `stateIn` on the injected app scope; registers itself + its VM in the isolated graph and hosts `ThemeRoute`):
```kotlin
package com.slothiesmooth.nyx.feature.theme.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import com.slothiesmooth.nyx.feature.theme.api.ThemeConfig
import com.slothiesmooth.nyx.feature.theme.api.ThemeFeature
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.feature.theme.api.ThemeRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

private val DefaultConfig = ThemeConfig(ThemeMode.System, NxPalette.DefaultDark, NxPalette.DefaultLight)

/** The theme feature: persists selections through [ThemeRepository] and hosts the change-theme route. */
class BasicThemeProvider(
    private val repository: ThemeRepository,
    scope: CoroutineScope,
) : KoinFeatureProvider(), ThemeFeature {

    override val theme: StateFlow<ThemeConfig> =
        repository.observeConfig().stateIn(scope, SharingStarted.Eagerly, DefaultConfig)

    override suspend fun setMode(mode: ThemeMode) = repository.setMode(mode)

    override suspend fun setPalette(palette: NxPalette) = repository.setPalette(palette)

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) {
        withDI { content() }
    }

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<ThemeRoute> {
            withDI { ChangeThemeScreen(viewModel = koinFeatureViewModel()) }
        }
    }

    override fun Module.onProvideDI() {
        single<ThemeFeature> { this@BasicThemeProvider }
        factoryOf(::ChangeThemeMutableState)
        viewModelOf(::ThemeViewModel)
    }
}
```

- [ ] **Step 13: Write `previews/ChangeThemePreviews.kt`** (covers default and dark-forced states — feature screens get previews per the user's global rule):
```kotlin
package com.slothiesmooth.nyx.feature.theme.basic.previews

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.feature.theme.basic.ChangeThemeMutableState
import com.slothiesmooth.nyx.feature.theme.basic.ChangeThemeState
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

private fun sampleState(mode: ThemeMode): ChangeThemeState =
    ChangeThemeMutableState().apply { this.mode = mode }

// A render-only mirror of ChangeThemeScreen bound to fixed state (the real screen needs a ViewModel).
@Composable
private fun ChangeThemePreviewBody(state: ChangeThemeState) {
    com.slothiesmooth.nyx.feature.theme.basic.ChangeThemeScreenStateless(state)
}

@AllThemePreview
@Composable
private fun ChangeThemeSystemAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { ChangeThemePreviewBody(sampleState(ThemeMode.System)) }
}

@AllThemePreview
@Composable
private fun ChangeThemeDarkAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { ChangeThemePreviewBody(sampleState(ThemeMode.Dark)) }
}
```
Then refactor `ChangeThemeScreen.kt` to split a stateless body the preview and the screen share — replace the `Column(...) { ... }` body with a call to a new `ChangeThemeScreenStateless(state, onMode = viewModel::onModeSelected, onPalette = viewModel::onPaletteSelected)` and move the current layout into that internal function (defaulting `onMode`/`onPalette` to no-ops so previews pass only `state`):
```kotlin
// ChangeThemeScreen.kt, replacing the body:
@Composable
fun ChangeThemeScreen(viewModel: ThemeViewModel) {
    ChangeThemeScreenStateless(
        state = viewModel.state,
        onMode = viewModel::onModeSelected,
        onPalette = viewModel::onPaletteSelected,
    )
}

@Composable
internal fun ChangeThemeScreenStateless(
    state: ChangeThemeState,
    onMode: (ThemeMode) -> Unit = {},
    onPalette: (NxPalette) -> Unit = {},
) {
    // ...the Column/Row/NxChip/NxCard layout from Step 11, calling onMode(mode) / onPalette(palette)...
}
```
(Add the `com.slothiesmooth.nyx.designlibrary.tokens.NxPalette` import for the `onPalette` parameter type.)

- [ ] **Step 14: Compile + rerun tests:**
```bash
./gradlew :feature:theme:client:basic:compileKotlinJvm :feature:theme:client:basic:compileKotlinWasmJs :feature:theme:client:basic:jvmTest
```
Expected: `BUILD SUCCESSFUL`, both test classes green.

- [ ] **Step 15: Commit:**
```bash
git add feature/theme/client/basic/src && git commit -m "feat(theme): repository, provider, ThemeProvider, change-theme screen + VM"
```

---

### Task 7: `:feature:navigation` — NavigationFeature + bottom-nav provider

**Files:**
- Create: `feature/navigation/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/navigation/api/NavigationFeature.kt`
- Create: `feature/navigation/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/navigation/basic/BasicNavigationProvider.kt`

**Interfaces:**
- Consumes: `Feature`/`FeatureContext`/`BaseFeatureProvider`/`LocalFeatureBottomBar` (Task 1), `NxIconKind`/`NxBottomNav`/`NxBottomNavItem` (Plan 04, Tasks 12 & 16).
- Produces (00-INDEX contract — exact):
  - `interface NavigationFeature : Feature { fun setItems(items: ImmutableList<NavItem>) }`
  - `data class NavItem(val route: Any, val label: String, val icon: NxIconKind, val selected: Boolean)`
  - `class BasicNavigationProvider : BaseFeatureProvider(), NavigationFeature`

**build.gradle.kts (api)** (`nyx.feature.api`; `commonMain.dependencies`):
```kotlin
api(projects.feature.common.client.api)
api(projects.shared.designLibrary)
implementation(libs.kotlinx.collections.immutable)
```
**build.gradle.kts (basic)** (`nyx.feature.basic`; `commonMain.dependencies`):
```kotlin
api(projects.feature.navigation.client.api)
api(projects.feature.common.client.api)
implementation(projects.shared.designLibrary)
implementation(libs.compose.runtime)
implementation(libs.koin.compose)
implementation(libs.kotlinx.collections.immutable)
```

**Steps:**

- [ ] **Step 1: Write `NavigationFeature.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.navigation.api

import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.Feature
import kotlinx.collections.immutable.ImmutableList

/**
 * One bottom-nav destination. [route] is the opaque target the app module supplies (never matched at
 * runtime — wasm-safe); [selected] is computed in `:client`, where the concrete route types are known.
 */
data class NavItem(
    val route: Any,
    val label: String,
    val icon: NxIconKind,
    val selected: Boolean,
)

/** Cross-feature handle to the shell bottom navigation. The app module pushes the current items. */
interface NavigationFeature : Feature {
    fun setItems(items: ImmutableList<NavItem>)
}
```

- [ ] **Step 2: Write `BasicNavigationProvider.kt`** (publishes the bottom bar into `LocalFeatureBottomBar` from its content wrapper; the bar hides on any non-tab route, where nothing is `selected`):
```kotlin
package com.slothiesmooth.nyx.feature.navigation.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.slothiesmooth.nyx.designlibrary.models.NxBottomNavItem
import com.slothiesmooth.nyx.designlibrary.organisms.NxBottomNav
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.api.LocalFeatureBottomBar
import com.slothiesmooth.nyx.feature.navigation.api.NavItem
import com.slothiesmooth.nyx.feature.navigation.api.NavigationFeature
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow

/** Renders the shell bottom navigation from client-supplied [NavItem]s. Has no route of its own. */
class BasicNavigationProvider : BaseFeatureProvider(), NavigationFeature {

    private val itemsState = MutableStateFlow<ImmutableList<NavItem>>(persistentListOf())

    override fun setItems(items: ImmutableList<NavItem>) {
        itemsState.value = items
    }

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalFeatureBottomBar provides { BottomBar(context) }) {
            content()
        }
    }

    @Composable
    private fun BottomBar(context: FeatureContext) {
        val items by itemsState.collectAsState()
        val selectedIndex = items.indexOfFirst { it.selected }
        if (selectedIndex < 0) return
        val display = remember(items) {
            items.map { NxBottomNavItem(icon = it.icon, label = it.label) }.toImmutableList()
        }
        NxBottomNav(
            items = display,
            selectedIndex = selectedIndex,
            onSelect = { index -> context.setDestination(items[index].route) },
        )
    }
}
```

- [ ] **Step 3: Compile:**
```bash
./gradlew :feature:navigation:client:api:compileKotlinJvm :feature:navigation:client:basic:compileKotlinJvm :feature:navigation:client:basic:compileKotlinWasmJs
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit:**
```bash
git add feature/navigation/client/api/src feature/navigation/client/basic/src && git commit -m "feat(navigation): NavigationFeature + bottom-nav provider"
```

---

### Task 8: `:feature:splash` — SplashFeature, auto-advance provider

**Files:**
- Create: `feature/splash/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/splash/api/SplashFeature.kt`
- Create: `feature/splash/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/splash/api/SplashRoute.kt`
- Create: `feature/splash/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/splash/basic/SplashState.kt`
- Create: `feature/splash/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/splash/basic/SplashViewModel.kt`
- Create: `feature/splash/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/splash/basic/SplashScreen.kt`
- Create: `feature/splash/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/splash/basic/BasicSplashProvider.kt`
- Create: `feature/splash/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/splash/basic/previews/SplashPreviews.kt`
- Test: `feature/splash/client/basic/src/commonTest/kotlin/com/slothiesmooth/nyx/feature/splash/basic/SplashViewModelTest.kt`

**Interfaces:**
- Consumes: `Feature` (Task 1), `KoinFeatureProvider`/`koinFeatureViewModel` (Task 2), `BaseFeatureProvider`/`FeatureContext` (Task 1), `BaseViewModel`/`ViewState`/`MutableViewState` (Plan 03), `NxIcon`/`NxIconKind`/`NxText`/`NxTextStyle`/`NxTokens`/`NxSpacing` (Plan 04).
- Produces:
  - `interface SplashFeature : Feature`
  - `@Serializable data object SplashRoute`
  - `class BasicSplashProvider(afterSplashRoute: Any) : KoinFeatureProvider(), SplashFeature` — auto-advances via the action bus to the injected route.

**build.gradle.kts (api)** (`nyx.feature.api`): `api(projects.feature.common.client.api)`, `implementation(libs.kotlinx.serialization.core)`.
**build.gradle.kts (basic)** (`nyx.feature.basic`; `commonMain.dependencies`):
```kotlin
api(projects.feature.splash.client.api)
api(projects.feature.common.client.api)
api(projects.feature.common.client.koin)
api(projects.shared.presentation)
implementation(projects.shared.designLibrary)
implementation(libs.compose.runtime)
implementation(libs.compose.foundation)
implementation(libs.compose.preview)
implementation(libs.androidx.navigation.compose)
implementation(libs.koin.core)
implementation(libs.koin.compose.viewmodel.navigation)
implementation(libs.kotlinx.coroutines.core)
```
and `commonTest.dependencies`: `implementation(libs.kotlin.test)`, `implementation(libs.kotlinx.coroutines.test)`.

**Steps:**

- [ ] **Step 1: Write `SplashFeature.kt` and `SplashRoute.kt`.**

`SplashFeature.kt`:
```kotlin
package com.slothiesmooth.nyx.feature.splash.api

import com.slothiesmooth.nyx.feature.common.api.Feature

/** The brand splash. Auto-advances to the app start destination the app module injects. */
interface SplashFeature : Feature
```
`SplashRoute.kt`:
```kotlin
package com.slothiesmooth.nyx.feature.splash.api

import kotlinx.serialization.Serializable

/** The splash screen route — the shell start destination. */
@Serializable
data object SplashRoute
```

- [ ] **Step 2: Write `SplashState.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.splash.basic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.state.ViewState

/** Read-only splash state: [ready] flips true once the brand dwell elapses. */
interface SplashState : ViewState {
    val ready: Boolean
}

class SplashMutableState : MutableViewState(), SplashState {
    override var ready: Boolean by mutableStateOf(false)
}
```

- [ ] **Step 3: Write the failing test `SplashViewModelTest.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.splash.basic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val AFTER_DWELL_MS = 2_000L

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `becomes ready only after the brand dwell elapses`() = runTest {
        val viewModel = SplashViewModel(SplashMutableState())
        viewModel.startDwell()
        runCurrent()
        assertFalse(viewModel.state.ready)
        advanceTimeBy(AFTER_DWELL_MS)
        runCurrent()
        assertTrue(viewModel.state.ready)
    }
}
```

- [ ] **Step 4: Run — expect COMPILE failure:**
```bash
./gradlew :feature:splash:client:basic:compileTestKotlinJvm
```
Expected: `Unresolved reference: SplashViewModel`.

- [ ] **Step 5: Write `SplashViewModel.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.splash.basic

import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.delay

private const val SPLASH_DWELL_MS = 1_100L

/** Holds the splash on-screen for a short brand dwell, then flips [SplashState.ready]. */
class SplashViewModel(
    private val mutableState: SplashMutableState,
) : BaseViewModel() {

    val state: SplashState = mutableState

    override fun doBind() {
        startDwell()
    }

    /** The dwell timer. Exposed so unit tests drive it on a test dispatcher. */
    fun startDwell() = async("splashDwell") {
        delay(SPLASH_DWELL_MS)
        withState { mutableState.ready = true }
    }
}
```

- [ ] **Step 6: Run — expect PASS:**
```bash
./gradlew :feature:splash:client:basic:jvmTest
```
Expected: `SplashViewModelTest` green.

- [ ] **Step 7: Write `SplashScreen.kt`** (brand mark; fires `onReady` once state flips):
```kotlin
package com.slothiesmooth.nyx.feature.splash.basic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

private val BrandMarkSize: Dp = 72.dp

/** The brand splash. Calls [onReady] exactly once, when the ViewModel signals the dwell is done. */
@Composable
fun SplashScreen(viewModel: SplashViewModel, onReady: () -> Unit) {
    val ready = viewModel.state.ready
    LaunchedEffect(ready) { if (ready) onReady() }
    SplashScreenStateless()
}

@Composable
internal fun SplashScreenStateless() {
    val colors = NxTokens.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NxSpacing.s4, Alignment.CenterVertically),
    ) {
        NxIcon(kind = NxIconKind.Vault, tint = colors.brand, size = BrandMarkSize)
        NxText(text = "Nyx", style = NxTextStyle.Display, color = colors.fg)
    }
}
```

- [ ] **Step 8: Write `BasicSplashProvider.kt`** (auto-advances through the action bus to the injected `afterSplashRoute` — the nav-action pattern; the target is `Any`, so splash imports no other feature api):
```kotlin
package com.slothiesmooth.nyx.feature.splash.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import com.slothiesmooth.nyx.feature.splash.api.SplashFeature
import com.slothiesmooth.nyx.feature.splash.api.SplashRoute
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

/** Hosts [SplashRoute] and, when its dwell elapses, replaces it with the injected start destination. */
class BasicSplashProvider(
    private val afterSplashRoute: Any,
) : KoinFeatureProvider(), SplashFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) {
        withDI { content() }
    }

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<SplashRoute> {
            withDI {
                SplashScreen(viewModel = koinFeatureViewModel(), onReady = { onSendAction(Advance) })
            }
        }
    }

    override suspend fun onReceiveAction(action: BaseFeatureProvider.Action, context: FeatureContext) {
        if (action is Advance) context.setDestination(afterSplashRoute)
    }

    override fun Module.onProvideDI() {
        factoryOf(::SplashMutableState)
        viewModelOf(::SplashViewModel)
    }

    private data object Advance : BaseFeatureProvider.Action
}
```

- [ ] **Step 9: Write `previews/SplashPreviews.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.splash.basic.previews

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.splash.basic.SplashScreenStateless
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@AllThemePreview
@Composable
private fun SplashAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { SplashScreenStateless() }
}
```

- [ ] **Step 10: Compile + test:**
```bash
./gradlew :feature:splash:client:basic:compileKotlinJvm :feature:splash:client:basic:compileKotlinWasmJs :feature:splash:client:basic:jvmTest
```
Expected: `BUILD SUCCESSFUL`, `SplashViewModelTest` green.

- [ ] **Step 11: Commit:**
```bash
git add feature/splash/client/api/src feature/splash/client/basic/src && git commit -m "feat(splash): brand splash with action-bus auto-advance"
```

---

### Task 9: Product feature api modules — interfaces + routes (vault, encrypt, decrypt, settings)

These four api modules give the shell its tab routes and cross-feature interfaces. Plan 06 fills their `basic` modules with real screens; this task creates only the api surface (00-INDEX contracts, exact).

**Files:**
- Create: `feature/vault/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/vault/api/VaultFeature.kt`
- Create: `feature/encrypt/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/encrypt/api/EncryptFeature.kt`
- Create: `feature/decrypt/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/decrypt/api/DecryptFeature.kt`
- Create: `feature/settings/client/api/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/settings/api/SettingsFeature.kt`

**Interfaces:**
- Consumes: `Feature` (Task 1).
- Produces (00-INDEX contract — exact):
  - `interface VaultFeature : Feature { fun observeActiveCount(): Flow<Int> }`, `@Serializable data object VaultRoute`, `@Serializable data class VaultDetailRoute(val imageId: String)`
  - `interface EncryptFeature : Feature`, `@Serializable data object EncryptRoute`
  - `interface DecryptFeature : Feature`, `@Serializable data class DecryptRoute(val imageId: String? = null)`
  - `interface SettingsFeature : Feature`, `@Serializable data object SettingsRoute`

**build.gradle.kts** (each `nyx.feature.api`; `commonMain.dependencies`): `api(projects.feature.common.client.api)`, `implementation(libs.kotlinx.serialization.core)`, and for vault also `implementation(libs.kotlinx.coroutines.core)` (its `Flow<Int>` surface).

**Steps:**

- [ ] **Step 1: Write `VaultFeature.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.vault.api

import com.slothiesmooth.nyx.feature.common.api.Feature
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Cross-feature handle to the vault. The active-count feed drives the empty-state gate elsewhere. */
interface VaultFeature : Feature {
    fun observeActiveCount(): Flow<Int>
}

/** The vault grid route (a bottom-nav tab). */
@Serializable
data object VaultRoute

/** The vault image detail route. */
@Serializable
data class VaultDetailRoute(val imageId: String)
```

- [ ] **Step 2: Write `EncryptFeature.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.encrypt.api

import com.slothiesmooth.nyx.feature.common.api.Feature
import kotlinx.serialization.Serializable

interface EncryptFeature : Feature

/** The encrypt wizard route (a bottom-nav tab). */
@Serializable
data object EncryptRoute
```

- [ ] **Step 3: Write `DecryptFeature.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.decrypt.api

import com.slothiesmooth.nyx.feature.common.api.Feature
import kotlinx.serialization.Serializable

interface DecryptFeature : Feature

/** The decrypt route (a bottom-nav tab); [imageId] preselects a vault image, null = fresh pick. */
@Serializable
data class DecryptRoute(val imageId: String? = null)
```

- [ ] **Step 4: Write `SettingsFeature.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.settings.api

import com.slothiesmooth.nyx.feature.common.api.Feature
import kotlinx.serialization.Serializable

interface SettingsFeature : Feature

/** The settings route (a bottom-nav tab). */
@Serializable
data object SettingsRoute
```

- [ ] **Step 5: Compile all four:**
```bash
./gradlew :feature:vault:client:api:compileKotlinJvm :feature:encrypt:client:api:compileKotlinJvm :feature:decrypt:client:api:compileKotlinJvm :feature:settings:client:api:compileKotlinJvm
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit:**
```bash
git add feature/vault/client/api/src feature/encrypt/client/api/src feature/decrypt/client/api/src feature/settings/client/api/src && git commit -m "feat(features): vault/encrypt/decrypt/settings api interfaces + routes"
```

---

### Task 10: Product feature basic stub providers ("coming soon")

Each stub provider makes the shell compile and run with a placeholder screen; Plan 06 replaces the screen bodies (the provider wiring stays). Settings additionally exposes a "Change theme" entry, routed through an app-injected `Any` route, so theme switching is reachable end-to-end in the running shell.

**Files:**
- Create: `feature/vault/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/vault/basic/BasicVaultProvider.kt`
- Create: `feature/encrypt/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/encrypt/basic/BasicEncryptProvider.kt`
- Create: `feature/decrypt/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/decrypt/basic/BasicDecryptProvider.kt`
- Create: `feature/settings/client/basic/src/commonMain/kotlin/com/slothiesmooth/nyx/feature/settings/basic/BasicSettingsProvider.kt`

**Interfaces:**
- Consumes: the matching `XFeature`/`XRoute` (Task 9), `BaseFeatureProvider`/`FeatureContext` (Task 1), `NxEmptyState`/`NxIconKind` (Plan 04 Task 16).
- Produces:
  - `class BasicVaultProvider : BaseFeatureProvider(), VaultFeature`
  - `class BasicEncryptProvider : BaseFeatureProvider(), EncryptFeature`
  - `class BasicDecryptProvider : BaseFeatureProvider(), DecryptFeature`
  - `class BasicSettingsProvider(changeThemeRoute: Any) : BaseFeatureProvider(), SettingsFeature`

**build.gradle.kts** (each `nyx.feature.basic`; `commonMain.dependencies`):
```kotlin
api(projects.feature.<name>.client.api)
api(projects.feature.common.client.api)
implementation(projects.shared.designLibrary)
implementation(libs.compose.runtime)
implementation(libs.androidx.navigation.compose)
```
Vault also: `implementation(libs.kotlinx.coroutines.core)` (its `observeActiveCount` returns `flowOf(0)` in the stub).

**Steps:**

- [ ] **Step 1: Write `BasicVaultProvider.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.vault.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyState
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.vault.api.VaultFeature
import com.slothiesmooth.nyx.feature.vault.api.VaultRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Placeholder vault provider; Plan 06 replaces the screen with the real stego-image grid. */
class BasicVaultProvider : BaseFeatureProvider(), VaultFeature {

    override fun observeActiveCount(): Flow<Int> = flowOf(0)

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<VaultRoute> {
            NxEmptyState(
                icon = NxIconKind.Vault,
                title = "Vault",
                body = "Your saved hidden messages will live here. Coming soon.",
            )
        }
    }
}
```

- [ ] **Step 2: Write `BasicEncryptProvider.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.encrypt.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyState
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptFeature
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptRoute

/** Placeholder encrypt provider; Plan 06 replaces the screen with the real encrypt wizard. */
class BasicEncryptProvider : BaseFeatureProvider(), EncryptFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<EncryptRoute> {
            NxEmptyState(
                icon = NxIconKind.Lock,
                title = "Encrypt",
                body = "Hide an encrypted message inside an image. Coming soon.",
            )
        }
    }
}
```

- [ ] **Step 3: Write `BasicDecryptProvider.kt`:**
```kotlin
package com.slothiesmooth.nyx.feature.decrypt.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyState
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptFeature
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptRoute

/** Placeholder decrypt provider; Plan 06 replaces the screen with the real reveal flow. */
class BasicDecryptProvider : BaseFeatureProvider(), DecryptFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<DecryptRoute> {
            NxEmptyState(
                icon = NxIconKind.Unlock,
                title = "Decrypt",
                body = "Reveal a hidden message from an image. Coming soon.",
            )
        }
    }
}
```

- [ ] **Step 4: Write `BasicSettingsProvider.kt`** (the one stub with an action — a CTA that navigates to the app-injected theme route through the action bus):
```kotlin
package com.slothiesmooth.nyx.feature.settings.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyState
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.settings.api.SettingsFeature
import com.slothiesmooth.nyx.feature.settings.api.SettingsRoute

/**
 * Placeholder settings provider. Its one live action opens the change-theme screen (route injected as
 * `Any` by the app module — settings imports no other feature api). Plan 06 replaces the screen body.
 */
class BasicSettingsProvider(
    private val changeThemeRoute: Any,
) : BaseFeatureProvider(), SettingsFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<SettingsRoute> {
            NxEmptyState(
                icon = NxIconKind.Settings,
                title = "Settings",
                body = "About, licenses, and vault controls are coming soon.",
                ctaText = "Change theme",
                onCta = { onSendAction(OpenTheme) },
            )
        }
    }

    override suspend fun onReceiveAction(action: BaseFeatureProvider.Action, context: FeatureContext) {
        if (action is OpenTheme) context.pushDestination(changeThemeRoute)
    }

    private data object OpenTheme : BaseFeatureProvider.Action
}
```

- [ ] **Step 5: Compile all four basic modules:**
```bash
./gradlew :feature:vault:client:basic:compileKotlinJvm :feature:encrypt:client:basic:compileKotlinJvm :feature:decrypt:client:basic:compileKotlinJvm :feature:settings:client:basic:compileKotlinJvm
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit:**
```bash
git add feature/vault/client/basic/src feature/encrypt/client/basic/src feature/decrypt/client/basic/src feature/settings/client/basic/src && git commit -m "feat(features): coming-soon stub providers for vault/encrypt/decrypt/settings"
```

---

### Task 11: `:client` — `DefaultImageCodec` expect/actual (Android + skiko intermediate)

The DI graph must resolve `ImageCodec`. The shell never exercises it (stubs), but Plan 06 does, so real impls ship now: Android via `BitmapFactory`, everything else via skiko in one `skikoMain` intermediate source set (`iosMain` + `jvmMain` + `wasmJsMain`). Realized as an `expect`/`actual` factory function (avoids the `expect class`-implements-interface member-actualization trap). Both paths force every decoded pixel opaque (`or 0xFF000000`) — premultiplication of `alpha < 255` pixels corrupts LSBs, so stego covers are always fully opaque (00-INDEX research fact / spec §5 delta).

**Files:**
- Create: `client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/data/codec/ImageCodecFactory.kt`
- Create: `client/src/androidMain/kotlin/com/slothiesmooth/nyx/client/data/codec/ImageCodecFactory.android.kt`
- Create: `client/src/skikoMain/kotlin/com/slothiesmooth/nyx/client/data/codec/ImageCodecFactory.skiko.kt`
- Modify: `client/build.gradle.kts` — declare the `skikoMain` intermediate source set.

**Interfaces:**
- Consumes (Plan 03): `interface ImageCodec { suspend fun decode(bytes: ByteArray): AppResult<PixelImage>; suspend fun encodePng(image: PixelImage): AppResult<ByteArray> }`, `AppResult`/`AppError`; (Plan 02) `PixelImage`.
- Produces: `expect fun defaultImageCodec(): ImageCodec` (the 00-INDEX `DefaultImageCodec` expect/actual, as a factory).

**Steps:**

- [ ] **Step 1: Modify `client/build.gradle.kts`** — add the intermediate source set inside `kotlin { ... }` (after `applyDefaultHierarchyTemplate()`). skiko is on the compile classpath of every non-android target via CMP, so no dependency is added; it must NOT reach `androidMain`:
```kotlin
applyDefaultHierarchyTemplate()
sourceSets {
    val skikoMain by creating { dependsOn(commonMain.get()) }
    iosMain.get().dependsOn(skikoMain)
    jvmMain.get().dependsOn(skikoMain)
    wasmJsMain.get().dependsOn(skikoMain)
    // ...existing commonMain.dependencies { } block stays...
}
```

- [ ] **Step 2: Write `commonMain` `ImageCodecFactory.kt`:**
```kotlin
package com.slothiesmooth.nyx.client.data.codec

import com.slothiesmooth.nyx.shared.data.source.ImageCodec

/** Platform image codec: Android `BitmapFactory`, everything else skiko. The 00-INDEX expect/actual. */
expect fun defaultImageCodec(): ImageCodec
```
> If Plan 03 placed `ImageCodec`/`PixelImage`/`AppResult` in different sub-packages, correct the imports across this task to match the delivered contracts (`ImageCodec`, `AppResult`, `AppError` from `:shared:data`; `PixelImage` from `:steganography`).

- [ ] **Step 3: Write `androidMain` `ImageCodecFactory.android.kt`:**
```kotlin
package com.slothiesmooth.nyx.client.data.codec

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.steganography.PixelImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val OPAQUE_ALPHA: Int = 0xFF shl 24
private const val PNG_QUALITY = 100

actual fun defaultImageCodec(): ImageCodec = AndroidImageCodec()

private class AndroidImageCodec : ImageCodec {

    override suspend fun decode(bytes: ByteArray): AppResult<PixelImage> = withContext(Dispatchers.Default) {
        runCatching {
            val options = BitmapFactory.Options().apply { inPremultiplied = false }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: return@withContext AppResult.Err(AppError.Validation("Not a decodable image"))
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.recycle()
            for (index in pixels.indices) pixels[index] = pixels[index] or OPAQUE_ALPHA
            AppResult.Ok(PixelImage(width, height, pixels))
        }.getOrElse { failure -> AppResult.Err(AppError.Storage("Image decode failed", failure)) }
    }

    override suspend fun encodePng(image: PixelImage): AppResult<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = Bitmap.createBitmap(image.pixels, image.width, image.height, Bitmap.Config.ARGB_8888)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)
            bitmap.recycle()
            AppResult.Ok(stream.toByteArray())
        }.getOrElse { failure -> AppResult.Err(AppError.Storage("Image encode failed", failure)) }
    }
}
```

- [ ] **Step 4: Write `skikoMain` `ImageCodecFactory.skiko.kt`** (BGRA_8888 / UNPREMUL round-trip; PNG encode null-checks the `Data`):
```kotlin
package com.slothiesmooth.nyx.client.data.codec

import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.steganography.PixelImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

private const val BYTES_PER_PIXEL = 4
private const val BLUE_INDEX = 0
private const val GREEN_INDEX = 1
private const val RED_INDEX = 2
private const val ALPHA_INDEX = 3
private const val BYTE_MASK = 0xFF
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val ALPHA_SHIFT = 24
private const val OPAQUE_ALPHA: Int = BYTE_MASK shl ALPHA_SHIFT

actual fun defaultImageCodec(): ImageCodec = SkikoImageCodec()

private class SkikoImageCodec : ImageCodec {

    override suspend fun decode(bytes: ByteArray): AppResult<PixelImage> = withContext(Dispatchers.Default) {
        runCatching {
            val image = Image.makeFromEncoded(bytes)
            val width = image.width
            val height = image.height
            val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
            val bitmap = Bitmap().apply { allocPixels(info) }
            image.readPixels(bitmap, 0, 0)
            image.close()
            val bgra = bitmap.readPixels(info, (width * BYTES_PER_PIXEL).toLong(), 0, 0)
            bitmap.close()
            if (bgra == null) {
                AppResult.Err(AppError.Validation("Could not read image pixels"))
            } else {
                AppResult.Ok(PixelImage(width, height, bgraToArgb(bgra, width * height)))
            }
        }.getOrElse { failure -> AppResult.Err(AppError.Storage("Image decode failed", failure)) }
    }

    override suspend fun encodePng(image: PixelImage): AppResult<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val info = ImageInfo(image.width, image.height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
            val raster = Image.makeRaster(info, argbToBgra(image.pixels), image.width * BYTES_PER_PIXEL)
            val data = raster.encodeToData(EncodedImageFormat.PNG)
            raster.close()
            if (data == null) {
                AppResult.Err(AppError.Storage("PNG encode returned no data"))
            } else {
                AppResult.Ok(data.bytes)
            }
        }.getOrElse { failure -> AppResult.Err(AppError.Storage("Image encode failed", failure)) }
    }

    private fun bgraToArgb(bgra: ByteArray, pixelCount: Int): IntArray {
        val argb = IntArray(pixelCount)
        var offset = 0
        for (index in 0 until pixelCount) {
            val blue = bgra[offset + BLUE_INDEX].toInt() and BYTE_MASK
            val green = bgra[offset + GREEN_INDEX].toInt() and BYTE_MASK
            val red = bgra[offset + RED_INDEX].toInt() and BYTE_MASK
            argb[index] = OPAQUE_ALPHA or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or blue
            offset += BYTES_PER_PIXEL
        }
        return argb
    }

    private fun argbToBgra(argb: IntArray): ByteArray {
        val bgra = ByteArray(argb.size * BYTES_PER_PIXEL)
        var offset = 0
        for (pixel in argb) {
            bgra[offset + BLUE_INDEX] = (pixel and BYTE_MASK).toByte()
            bgra[offset + GREEN_INDEX] = ((pixel shr GREEN_SHIFT) and BYTE_MASK).toByte()
            bgra[offset + RED_INDEX] = ((pixel shr RED_SHIFT) and BYTE_MASK).toByte()
            bgra[offset + ALPHA_INDEX] = BYTE_MASK.toByte()
            offset += BYTES_PER_PIXEL
        }
        return bgra
    }
}
```
> The skiko method shapes used — `Image.makeFromEncoded`, `Image.readPixels(bitmap, srcX, srcY)`, `Bitmap.readPixels(info, rowBytes, srcX, srcY): ByteArray?`, `Image.makeRaster(info, bytes, rowBytes)`, `Image.encodeToData(format): Data?`, `Data.bytes` — match the skiko bundled with CMP 1.10.3. If a signature differs at build time, adjust the call (the algorithm above is authoritative). Recorded in Open Questions.

- [ ] **Step 5: Compile the client on JVM and wasm** (Android + iOS codec compile in their own steps / on CI):
```bash
./gradlew :client:compileKotlinJvm :client:compileKotlinWasmJs
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit:**
```bash
git add client/build.gradle.kts client/src && git commit -m "feat(client): DefaultImageCodec expect/actual (BitmapFactory + skiko)"
```

---

### Task 12: `:client` — DI graph, appModule, AppViewModel, App composable

**Files:**
- Create: `client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/DI.kt`
- Create: `client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/app/AppConfig.kt`
- Create: `client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/app/presentation/AppViewModel.kt`
- Create: `client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/app/presentation/App.kt`
- Modify: `client/build.gradle.kts` — add feature + engine project deps (list below).

**Interfaces:**
- Consumes: every feature api + basic (Tasks 5–10), `defaultImageCodec()` (Task 11), engines (Plan 02: `NyxCrypto`/`DefaultNyxCrypto`, `Steganography`), infra (Plan 03: `Uuid4IdGenerator`/`IdGenerator`, `SystemClock`/`Clock`, `DefaultDomainEventBus`/`DomainEventBus`, `SqlDelightSource`, `VaultSqlSource`/`VaultSource`, `NyxDb`, source interfaces), `FeatureHost`/`FeatureHostContext`/`Feature`/`routeNameOf` (Task 1), `BaseViewModel` (Plan 03), `ThemeProvider`/`ThemeRoute` (Tasks 5–6), `NavigationFeature`/`NavItem` (Task 7), `SplashRoute` (Task 8).
- Produces (00-INDEX contract):
  - `fun initKoin(platformModule: Module): KoinApplication`
  - `fun appModule(platformModule: Module): Module`
  - `class AppViewModel(navigationFeature: NavigationFeature) : BaseViewModel()`
  - `@Composable fun App()`

**build.gradle.kts** — the existing `client/build.gradle.kts` `commonMain.dependencies` must `api`/`implementation` all shell modules. Add:
```kotlin
api(projects.crypto)
api(projects.steganography)
api(projects.shared.data)
api(projects.shared.presentation)
api(projects.shared.designLibrary)
api(projects.feature.common.client.api)
api(projects.feature.common.client.koin)
api(projects.feature.theme.client.api)
implementation(projects.feature.theme.client.basic)
api(projects.feature.navigation.client.api)
implementation(projects.feature.navigation.client.basic)
api(projects.feature.splash.client.api)
implementation(projects.feature.splash.client.basic)
api(projects.feature.vault.client.api)
implementation(projects.feature.vault.client.basic)
api(projects.feature.encrypt.client.api)
implementation(projects.feature.encrypt.client.basic)
api(projects.feature.decrypt.client.api)
implementation(projects.feature.decrypt.client.basic)
api(projects.feature.settings.client.api)
implementation(projects.feature.settings.client.basic)
implementation(libs.koin.core)
implementation(libs.koin.compose)
implementation(libs.koin.compose.viewmodel.navigation)
implementation(libs.androidx.navigation.compose)
implementation(libs.compose.runtime)
implementation(libs.compose.foundation)
implementation(libs.kotlinx.collections.immutable)
implementation(libs.sqldelight.runtime)
```

**Steps:**

- [ ] **Step 1: Write `DI.kt`** (idempotent global start — safe on wasm reload):
```kotlin
package com.slothiesmooth.nyx.client

import com.slothiesmooth.nyx.client.app.appModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

private var started: KoinApplication? = null

/** Starts the global Koin graph once, layering the platform module under the app graph. */
fun initKoin(platformModule: org.koin.core.module.Module): KoinApplication {
    started?.let { return it }
    return startKoin { modules(appModule(platformModule)) }.also { started = it }
}
```

- [ ] **Step 2: Write `AppConfig.kt`** (the whole graph: engines, infra, sources from the platform module, the app scope, SqlDelight-backed vault source, each feature bound to its interface, and the ordered feature list the host nests). Cross-feature nav targets are injected as `Any`:
```kotlin
package com.slothiesmooth.nyx.client.app

import com.slothiesmooth.nyx.client.data.codec.defaultImageCodec
import com.slothiesmooth.nyx.client.app.presentation.AppViewModel
import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.crypto.NyxCrypto
import com.slothiesmooth.nyx.feature.common.api.Feature
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptFeature
import com.slothiesmooth.nyx.feature.decrypt.basic.BasicDecryptProvider
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptFeature
import com.slothiesmooth.nyx.feature.encrypt.basic.BasicEncryptProvider
import com.slothiesmooth.nyx.feature.navigation.api.NavigationFeature
import com.slothiesmooth.nyx.feature.navigation.basic.BasicNavigationProvider
import com.slothiesmooth.nyx.feature.settings.api.SettingsFeature
import com.slothiesmooth.nyx.feature.settings.basic.BasicSettingsProvider
import com.slothiesmooth.nyx.feature.splash.api.SplashFeature
import com.slothiesmooth.nyx.feature.splash.basic.BasicSplashProvider
import com.slothiesmooth.nyx.feature.theme.api.ThemeFeature
import com.slothiesmooth.nyx.feature.theme.api.ThemeRoute
import com.slothiesmooth.nyx.feature.theme.basic.BasicThemeProvider
import com.slothiesmooth.nyx.feature.theme.basic.ThemeRepository
import com.slothiesmooth.nyx.feature.vault.api.VaultFeature
import com.slothiesmooth.nyx.feature.vault.api.VaultRoute
import com.slothiesmooth.nyx.feature.vault.basic.BasicVaultProvider
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.id.IdGenerator
import com.slothiesmooth.nyx.shared.data.id.Uuid4IdGenerator
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.shared.data.time.SystemClock
import com.slothiesmooth.nyx.steganography.Steganography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The application graph. Layers the [platformModule] (drivers, settings, file/share/camera sources,
 * capabilities) under the shared engines/infra, one binding per feature interface, and the ordered
 * feature list the [com.slothiesmooth.nyx.feature.common.api.FeatureHost] decorates. Navigation is a
 * one-off through each provider, so splash/settings get their cross-feature targets as `Any` routes.
 */
fun appModule(platformModule: Module): Module = module {
    includes(platformModule)

    // Engines (Plan 02) and infrastructure (Plan 03).
    single<NyxCrypto> { DefaultNyxCrypto() }
    single { Steganography() }
    single<ImageCodec> { defaultImageCodec() }
    single<IdGenerator> { Uuid4IdGenerator() }
    single<Clock> { SystemClock() }
    single<DomainEventBus> { DefaultDomainEventBus() }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // SqlDelight-backed vault metadata source (schema + wrappers delivered by Plan 03).
    single { com.slothiesmooth.nyx.client.data.source.database.sqldelight.SqlDelightSource(get(), get()) }
    single<VaultSource> { com.slothiesmooth.nyx.client.data.source.database.vault.VaultSqlSource(get()) }

    // Features (each bound to its cross-feature interface).
    single<ThemeFeature> { BasicThemeProvider(ThemeRepository(get()), get()) }
    single<NavigationFeature> { BasicNavigationProvider() }
    single<SplashFeature> { BasicSplashProvider(afterSplashRoute = VaultRoute) }
    single<VaultFeature> { BasicVaultProvider() }
    single<EncryptFeature> { BasicEncryptProvider() }
    single<DecryptFeature> { BasicDecryptProvider() }
    single<SettingsFeature> { BasicSettingsProvider(changeThemeRoute = ThemeRoute) }

    // The nested order: splash + navigation wrap first, then the tab features contribute routes.
    single<List<Feature>> {
        listOf(
            get<SplashFeature>(),
            get<NavigationFeature>(),
            get<ThemeFeature>(),
            get<VaultFeature>(),
            get<EncryptFeature>(),
            get<DecryptFeature>(),
            get<SettingsFeature>(),
        )
    }

    viewModelOf(::AppViewModel)
}
```
> Both `com.slothiesmooth.nyx.client.data.source.database.*` references are Plan 03 deliverables, aligned here to their delivered constructors/packages: `SqlDelightSource(driver: SqlDriver, scope: CoroutineScope)` (package `...client.data.source.database.sqldelight`) builds `NyxDb` internally from the injected `SqlDriver` (Task 13's `single<SqlDriver>`) and the shared `CoroutineScope`; `VaultSqlSource(source: SqlDelightSource, ioContext: CoroutineContext = Dispatchers.Default)` (package `...client.data.source.database.vault`) binds `VaultSource`. Both are lazy `single`s the shell never instantiates, so any residual shape drift surfaces only at compile time.

- [ ] **Step 3: Write `AppViewModel.kt`** (owns the start destination and the tab set; `selected` is computed here where route types are known — wasm-safe, no runtime serialization). Route names are stripped of any argument suffix before comparison so a route with args (Decrypt) still matches:
```kotlin
package com.slothiesmooth.nyx.client.app.presentation

import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.routeNameOf
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptRoute
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptRoute
import com.slothiesmooth.nyx.feature.navigation.api.NavItem
import com.slothiesmooth.nyx.feature.navigation.api.NavigationFeature
import com.slothiesmooth.nyx.feature.settings.api.SettingsRoute
import com.slothiesmooth.nyx.feature.splash.api.SplashRoute
import com.slothiesmooth.nyx.feature.vault.api.VaultRoute
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private data class Tab(val route: Any, val routeName: String, val label: String, val icon: NxIconKind)

/** The shell view model: fixed start destination and the bottom-nav tab set with live selection. */
class AppViewModel(
    private val navigationFeature: NavigationFeature,
) : BaseViewModel() {

    val startDestination: Any = SplashRoute

    private val tabs: List<Tab> = listOf(
        Tab(VaultRoute, routeNameOf<VaultRoute>(), "Vault", NxIconKind.Vault),
        Tab(EncryptRoute, routeNameOf<EncryptRoute>(), "Encrypt", NxIconKind.Lock),
        Tab(DecryptRoute(), routeNameOf<DecryptRoute>(), "Decrypt", NxIconKind.Unlock),
        Tab(SettingsRoute, routeNameOf<SettingsRoute>(), "Settings", NxIconKind.Settings),
    )

    override fun doInit() {
        refreshNavItems(currentRouteName = null)
    }

    /** Rebuilds the tab items with [currentRouteName] highlighted, and hands them to the nav feature. */
    fun refreshNavItems(currentRouteName: String?) {
        val active = currentRouteName?.substringBefore('/')?.substringBefore('?')
        navigationFeature.setItems(navItems(active))
    }

    private fun navItems(activeRouteName: String?): ImmutableList<NavItem> = tabs.map { tab ->
        NavItem(
            route = tab.route,
            label = tab.label,
            icon = tab.icon,
            selected = tab.routeName == activeRouteName,
        )
    }.toImmutableList()
}
```

- [ ] **Step 4: Write `App.kt`** (resolves the graph, wraps the host in `ThemeProvider`, forwards route changes to the VM for tab selection):
```kotlin
package com.slothiesmooth.nyx.client.app.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.slothiesmooth.nyx.feature.common.api.Feature
import com.slothiesmooth.nyx.feature.common.api.FeatureHost
import com.slothiesmooth.nyx.feature.common.api.FeatureHostContext
import com.slothiesmooth.nyx.feature.theme.api.ThemeFeature
import com.slothiesmooth.nyx.feature.theme.basic.ThemeProvider
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Root composable. Applies the persisted theme via [ThemeProvider], builds the single-NavHost
 * [FeatureHost] over the ordered feature list, and keeps the bottom-nav selection in sync with the
 * live back-stack entry. The Koin graph is established by [com.slothiesmooth.nyx.client.initKoin].
 */
@Composable
fun App() {
    val themeFeature = koinInject<ThemeFeature>()
    val features = koinInject<List<Feature>>()
    val appViewModel = koinViewModel<AppViewModel>()
    appViewModel.bind()

    val navController = rememberNavController()
    val context = remember(navController, features) {
        FeatureHostContext(debug = false, features = features, navController = navController)
    }

    val entry by navController.currentBackStackEntryAsState()
    LaunchedEffect(entry) { appViewModel.refreshNavItems(entry?.destination?.route) }

    ThemeProvider(themeFeature) {
        FeatureHost(context = context, startDestinationProvider = { appViewModel.startDestination })
    }
}
```

- [ ] **Step 5: Compile the client on JVM and wasm:**
```bash
./gradlew :client:compileKotlinJvm :client:compileKotlinWasmJs
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit:**
```bash
git add client/build.gradle.kts client/src && git commit -m "feat(client): initKoin, appModule, AppViewModel, App composable"
```

---

### Task 13: `:androidApp` — platform module, Application, Activity, manifest, resources, run

The Android platform module supplies the `single`s the app graph layers under it. The shell only exercises `SettingsSource` (theme persistence) and the feature graph; the DB driver / vault store / share / camera sources are registered (lazy, `createEagerInstances = false`) for Plan 06 and must compile now.

**Files:**
- Create: `androidApp/src/main/kotlin/com/slothiesmooth/nyx/androidapp/AndroidPlatformModule.kt`
- Create: `androidApp/src/main/kotlin/com/slothiesmooth/nyx/androidapp/DataStoreSettingsSource.kt`
- Create: `androidApp/src/main/kotlin/com/slothiesmooth/nyx/androidapp/FileKitVaultFileStore.kt`
- Create: `androidApp/src/main/kotlin/com/slothiesmooth/nyx/androidapp/AndroidShareSource.kt`
- Create: `androidApp/src/main/kotlin/com/slothiesmooth/nyx/androidapp/FileKitCameraSource.kt`
- Create: `androidApp/src/main/kotlin/com/slothiesmooth/nyx/androidapp/NyxApplication.kt`
- Create: `androidApp/src/main/kotlin/com/slothiesmooth/nyx/androidapp/MainActivity.kt`
- Create: `androidApp/src/main/AndroidManifest.xml`
- Create: `androidApp/src/main/res/values/strings.xml`
- Create: `androidApp/src/main/res/values/colors.xml`
- Create: `androidApp/src/main/res/values/themes.xml`
- Create: `androidApp/src/main/res/xml/file_paths.xml`
- Create: `androidApp/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Reuse: `androidApp/src/main/res/drawable/nyx_launcher_foreground.xml` (adaptive foreground — see Step 9).

**Interfaces:**
- Consumes: `initKoin` (Task 12), `App` (Task 12), source interfaces + `PlatformCapabilities` (Plan 03), SqlDelight `NyxDb.Schema` (Plan 03), FileKit 0.13.0, DataStore 1.2.1, `AndroidSqliteDriver` (SqlDelight android driver).
- Produces: `fun androidPlatformModule(context: Context): Module`, `class NyxApplication : Application`, `class MainActivity : ComponentActivity`.

**build.gradle.kts** (`com.android.application` + compose; per Plan 01 skeleton — ensure `dependencies`):
```kotlin
implementation(projects.client)
implementation(libs.androidx.activity.compose)
implementation(libs.androidx.core.splashscreen)
implementation(libs.kotlinx.coroutines.android)
implementation(libs.koin.core)
implementation(libs.koin.android)
implementation(libs.sqldelight.android.driver)
implementation(libs.androidx.datastore.preferences.core)
implementation(libs.filekit.core)
implementation(libs.filekit.dialogs)
```
and `android { namespace = "com.slothiesmooth.nyx"; defaultConfig { applicationId = "com.slothiesmooth.nyx" }; buildFeatures { compose = true } }` (from the skeleton).

**Steps:**

- [ ] **Step 1: Write `DataStoreSettingsSource.kt`** (path `filesDir/nyx.preferences_pb`):
```kotlin
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
```

- [ ] **Step 2: Write `FileKitVaultFileStore.kt`** (PNG files under `<files>/stego_vault`, named by id). FileKit `PlatformFile` API — adjust method names if the 0.13.0 build differs:
```kotlin
package com.slothiesmooth.nyx.androidapp

import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write

/** Stores stego PNG bytes as `<root>/<id>.png`. [root] is the platform vault directory. */
class FileKitVaultFileStore(private val root: PlatformFile) : VaultFileStore {

    private fun fileFor(id: String): PlatformFile = PlatformFile(root, "$id.png")

    override suspend fun write(id: String, bytes: ByteArray): AppResult<Unit> = runCatching {
        fileFor(id).write(bytes)
        AppResult.Ok(Unit)
    }.getOrElse { failure -> AppResult.Err(AppError.Storage("Vault write failed", failure)) }

    override suspend fun read(id: String): AppResult<ByteArray> = runCatching {
        AppResult.Ok(fileFor(id).readBytes())
    }.getOrElse { failure -> AppResult.Err(AppError.Storage("Vault read failed", failure)) }

    override suspend fun delete(id: String): AppResult<Unit> = runCatching {
        fileFor(id).delete()
        AppResult.Ok(Unit)
    }.getOrElse { failure -> AppResult.Err(AppError.Storage("Vault delete failed", failure)) }

    override suspend fun deleteAll(): AppResult<Unit> = runCatching {
        root.list().forEach { file -> file.delete() }
        AppResult.Ok(Unit)
    }.getOrElse { failure -> AppResult.Err(AppError.Storage("Vault wipe failed", failure)) }
}
```

- [ ] **Step 3: Write `AndroidShareSource.kt`** (FileProvider + `ACTION_SEND`):
```kotlin
package com.slothiesmooth.nyx.androidapp

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val SHARE_SUBDIR = "shared"
private const val MIME_PNG = "image/png"

/** Shares stego PNG bytes through a system chooser via a FileProvider content URI. */
class AndroidShareSource(private val context: Context) : ShareSource {

    override suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, SHARE_SUBDIR).apply { mkdirs() }
                val file = File(dir, fileName).apply { writeBytes(bytes) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = MIME_PNG
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                AppResult.Ok(Unit)
            }.getOrElse { failure -> AppResult.Err(AppError.Storage("Share failed", failure)) }
        }
}
```

- [ ] **Step 4: Write `FileKitCameraSource.kt`** (FileKit system camera; API name per 0.13.0):
```kotlin
package com.slothiesmooth.nyx.androidapp

import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.PickedImage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openCameraPicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes

/** System-camera capture via FileKit (mobile only). */
class FileKitCameraSource : CameraSource {

    override val isAvailable: Boolean = true

    override suspend fun capture(): PickedImage? {
        val file = FileKit.openCameraPicker() ?: return null
        return PickedImage(bytes = file.readBytes(), suggestedName = file.name)
    }
}
```
> `FileKit.openCameraPicker()` and the `PlatformFile` extensions above are the FileKit 0.13.0 `filekit-dialogs`/`filekit-core` APIs (mobileMain). If a name differs at build time, adjust — none of this runs in the shell. Recorded in Open Questions.

- [ ] **Step 5: Write `AndroidPlatformModule.kt`** (builds the DataStore, vault dir, driver, and binds every platform source + capabilities):
```kotlin
package com.slothiesmooth.nyx.androidapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.SettingsSource
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
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
}
```
> `NyxDb.Schema.synchronous()` bridges the async schema (`generateAsync = true`) to the synchronous Android driver (00-INDEX). If Plan 03 exposes the schema differently, align the first `single`. The DataStore `createWithPath` producer returns an `okio.Path`.

- [ ] **Step 6: Write `NyxApplication.kt`:**
```kotlin
package com.slothiesmooth.nyx.androidapp

import android.app.Application
import com.slothiesmooth.nyx.client.initKoin

/** Starts the Koin graph with the Android platform module before any Activity or ViewModel resolves. */
class NyxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidPlatformModule(this))
    }
}
```

- [ ] **Step 7: Write `MainActivity.kt`** (splashscreen + edge-to-edge; holds the splash until the first frame draws):
```kotlin
package com.slothiesmooth.nyx.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.slothiesmooth.nyx.client.app.presentation.App

/** Single-activity host. Keeps the OS splash on-screen until the shared [App] draws its first frame. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var firstFrameDrawn = false
        splashScreen.setKeepOnScreenCondition { !firstFrameDrawn }

        enableEdgeToEdge()
        setContent {
            App()
            LaunchedEffect(Unit) { firstFrameDrawn = true }
        }
    }
}
```

- [ ] **Step 8: Write the manifest + resources.**

`AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".NyxApplication"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Nyx">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|density|uiMode"
            android:theme="@style/Theme.Nyx.Splash"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>
```

`res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Nyx</string>
</resources>
```

`res/values/colors.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Umbra background (NxPalette.Umbra.bg) — matches the adaptive icon body and the splash field. -->
    <color name="nyx_splash_background">#14121F</color>
    <color name="ic_launcher_background">#14121F</color>
</resources>
```

`res/values/themes.xml` (parent `Theme.SplashScreen` from core-splashscreen):
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Nyx" parent="@android:style/Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/nyx_splash_background</item>
    </style>

    <style name="Theme.Nyx.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/nyx_splash_background</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/nyx_launcher_foreground</item>
        <item name="postSplashScreenTheme">@style/Theme.Nyx</item>
    </style>
</resources>
```

`res/xml/file_paths.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="shared" path="shared/" />
</paths>
```

`res/mipmap-anydpi-v26/ic_launcher.xml` (adaptive icon):
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/nyx_launcher_foreground" />
</adaptive-icon>
```

- [ ] **Step 9: Provide the adaptive foreground drawable.** Copy the existing brand asset into a foreground drawable so the launcher and splash share a mark (the old app logo is fine as a placeholder until a real icon lands):
```bash
mkdir -p androidApp/src/main/res/drawable
cp images/applogo.png androidApp/src/main/res/drawable/nyx_launcher_foreground.png 2>/dev/null || \
  echo "images/applogo.png not found — add any square PNG at androidApp/src/main/res/drawable/nyx_launcher_foreground.png"
```
If a PNG is used, reference `@drawable/nyx_launcher_foreground` (drop the `.png`); if a vector is preferred, create `nyx_launcher_foreground.xml` instead. Ensure the referenced name in `themes.xml` and `ic_launcher.xml` matches.

- [ ] **Step 10: Assemble the debug APK:**
```bash
./gradlew :androidApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`. (If the icon reference is unresolved, fix the drawable name from Step 9.)

- [ ] **Step 11: Install + launch on a device/emulator and verify manually:**
```bash
./gradlew :androidApp:installDebug
adb shell am start -n com.slothiesmooth.nyx/.MainActivity
```
Manual checks (the phase acceptance): the OS splash shows the brand mark, then the in-app splash briefly, then the Vault "coming soon" screen with a bottom nav (Vault / Encrypt / Decrypt / Settings), Vault selected; tapping tabs switches screens and highlights the tapped tab; Settings → "Change theme" opens the palette picker; picking a different palette / mode re-themes the whole app immediately and survives an app restart.

- [ ] **Step 12: Commit:**
```bash
git add androidApp/src androidApp/build.gradle.kts && git commit -m "feat(androidApp): platform module, Application, Activity, manifest, splash theme"
```

---

### Task 14: `:shared:compose-test-support` — runFeatureUiTest harness + theme-switch UI test

The harness boots the full app under an isolated Koin graph wired to in-memory test infrastructure (fresh in-memory DB, `FakeClock` @ 2026-07-01T12:00Z, `DeterministicIdGenerator`, `DefaultDomainEventBus`, `FakeSettingsSource`), for Plan 06's flow tests. The first UI test is focused (drives `ThemeProvider` + a probe) so it is robust on Linux (`jvmTest`); iOS placement mirrors it in Plan 06, compile-gated on macOS.

**Files:**
- Create: `shared/compose-test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/composetestsupport/TestInfrastructureModule.kt`
- Create: `shared/compose-test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/composetestsupport/FeatureUiTest.kt`
- Test: `feature/theme/client/basic/src/jvmTest/kotlin/com/slothiesmooth/nyx/feature/theme/basic/ThemeSwitchUiTest.kt`

**Interfaces:**
- Consumes: `appModule` (Task 12) — no; the harness builds its own graph from `TestInfrastructureModule` + the feature modules via `initKoin`-free isolated Koin; `App` (Task 12), `FakeClock`/`DeterministicIdGenerator`/in-memory `TestSqlDriver` (Plan 03), `FakeSettingsSource` (Task 4), `NyxDb`/`SqlDelightSource`/`VaultSqlSource` (Plan 03), source interfaces (Plan 03), `ThemeProvider`/`ThemeRepository`/`BasicThemeProvider` + `ThemeFeature` (Tasks 5–6), `NxTokens`/`NxPalette` (Plan 04), `ThemeMode` (Task 5).
- Produces: `fun runFeatureUiTest(extraModules: List<Module> = emptyList(), body: ComposeUiTest.() -> Unit)`, `fun testInfrastructureModule(): Module`.

**build.gradle.kts** (`nyx.kmp.library` + compose test; `commonMain.dependencies`):
```kotlin
api(projects.client)
api(projects.shared.data)
api(projects.shared.testSupport)
implementation(projects.shared.designLibrary)
implementation(compose.uiTest)
implementation(libs.koin.core)
implementation(libs.koin.compose)
implementation(libs.kotlinx.coroutines.core)
```
(`compose.uiTest` is the CMP multiplatform UI-test artifact from the `org.jetbrains.compose` plugin.) The theme UI test lives in `:feature:theme:client:basic` `jvmTest`; add there: `jvmTest.dependencies { implementation(compose.uiTest); implementation(compose.desktop.uiTestJUnit4); implementation(libs.kotlin.test); implementation(projects.shared.testSupport) }`.

**Steps:**

- [ ] **Step 1: Write `TestInfrastructureModule.kt`** (the deterministic infra graph; the in-memory `TestSqlDriver` factory + `FakeClock`/`DeterministicIdGenerator` come from `:shared:test-support`):
```kotlin
package com.slothiesmooth.nyx.shared.composetestsupport

import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.id.IdGenerator
import com.slothiesmooth.nyx.shared.data.source.SettingsSource
import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.shared.testsupport.DeterministicIdGenerator
import com.slothiesmooth.nyx.shared.testsupport.FakeClock
import com.slothiesmooth.nyx.shared.testsupport.FakeSettingsSource
import kotlin.time.Instant
import org.koin.core.module.Module
import org.koin.dsl.module

private val FixedInstant = Instant.parse("2026-07-01T12:00:00Z")

/** Deterministic infrastructure for UI/flow tests: fixed clock/ids, in-memory settings + event bus. */
fun testInfrastructureModule(): Module = module {
    single<Clock> { FakeClock(FixedInstant) }
    single<IdGenerator> { DeterministicIdGenerator() }
    single<DomainEventBus> { DefaultDomainEventBus() }
    single<SettingsSource> { FakeSettingsSource() }
}
```
> If Plan 03 named the deterministic id generator or its constructor differently (e.g. a seed parameter), align the `DeterministicIdGenerator()` call. `kotlin.time.Instant` is canonical (kotlinx-datetime 0.7.x).

- [ ] **Step 2: Write `FeatureUiTest.kt`** (the general harness for Plan 06 — isolated Koin over test infra + the app graph, then `App()`):
```kotlin
package com.slothiesmooth.nyx.shared.composetestsupport

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.slothiesmooth.nyx.client.app.presentation.App
import org.koin.compose.KoinIsolatedContext
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

/**
 * Boots the full [App] under an isolated Koin graph wired to deterministic test infrastructure.
 * [extraModules] override or extend the graph (e.g. a stub platform source). Runs on any host with a
 * Compose test runtime (jvm on Linux; androidHostTest / iosTest where available).
 */
@OptIn(ExperimentalTestApi::class)
fun runFeatureUiTest(extraModules: List<Module> = emptyList(), body: ComposeUiTest.() -> Unit) =
    runComposeUiTest {
        val graph = koinApplication {
            modules(listOf(testInfrastructureModule()) + extraModules)
        }
        setContent {
            KoinIsolatedContext(graph) { App() }
        }
        body()
    }
```
> The harness graph must also carry the app `appModule` bindings for `App()` to resolve features; wire that in Plan 06 when the real platform test module and feature registration are exercised (pass the app graph via `extraModules`). For this phase the deliverable is the focused theme test in Step 3, which does not boot the full `App`.

- [ ] **Step 3: Write the failing UI test `ThemeSwitchUiTest.kt`** (drives `ThemeProvider` and a probe that captures `NxTokens.colors.bg`; switching palette must change it — the end-to-end theme proof). In `jvmTest`, `runBlocking` is available for the suspend `setMode`/`setPalette`:
```kotlin
package com.slothiesmooth.nyx.feature.theme.basic

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.shared.testsupport.FakeSettingsSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class ThemeSwitchUiTest {

    @Test
    fun `switching palette changes the effective NxTokens colors`() = runComposeUiTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val theme = BasicThemeProvider(ThemeRepository(FakeSettingsSource()), scope)
        var latestBg: Color? = null

        setContent {
            ThemeProvider(theme) {
                val bg = NxTokens.colors.bg
                SideEffect { latestBg = bg }
            }
        }

        runBlocking {
            theme.setMode(ThemeMode.Dark)
            theme.setPalette(NxPalette.Umbra)
        }
        waitForIdle()
        val umbraBg = latestBg
        assertEquals(NxPalette.Umbra.colors.bg, umbraBg)

        runBlocking { theme.setPalette(NxPalette.Eclipse) }
        waitForIdle()
        val eclipseBg = latestBg
        assertEquals(NxPalette.Eclipse.colors.bg, eclipseBg)
        assertNotEquals(umbraBg, eclipseBg)
    }
}
```

- [ ] **Step 4: Run — expect the harness + test to compile, then PASS on JVM:**
```bash
./gradlew :shared:compose-test-support:compileKotlinJvm :feature:theme:client:basic:jvmTest
```
Expected: `BUILD SUCCESSFUL`, `ThemeSwitchUiTest` green. (On Linux only the JVM/host-runnable subset runs; the iOS UI-test source set compiles on macOS/CI — never gate Linux progress on it.)

- [ ] **Step 5: Commit:**
```bash
git add shared/compose-test-support/src feature/theme/client/basic/src && git commit -m "test(shell): runFeatureUiTest harness + theme-switch UI test"
```

---

## Phase completion checklist

- [ ] `./gradlew :androidApp:assembleDebug` → `BUILD SUCCESSFUL`.
- [ ] `./gradlew :androidApp:installDebug` and manual launch shows splash → bottom-nav shell; theme switching (Settings → Change theme) re-themes live and persists across restart.
- [ ] `./gradlew :feature:theme:client:basic:jvmTest :feature:splash:client:basic:jvmTest :feature:common:client:api:jvmTest` → all green.
- [ ] `./gradlew detektMain` (or the project's detekt aggregate) → 0 issues, no new `@Suppress`.
- [ ] Every new module compiles on `jvm` and `wasmJs`; iOS deferred to the macOS lane.

## Deviations recorded in this plan (see Open Questions for the ones needing confirmation)

1. `FeatureProvider.provideContent` keeps its `context` parameter (the 00-INDEX one-line summary omitted it; the recursive host requires it — governed by the "ported from source — same shape" clause).
2. `FeatureContext.getDestinationId` dropped; `getCurrentDestinationChanges()` returns route-name `String?` (wasm-safe; the reference architecture abandoned the serializer-hash id for the same reason — Nyx targets wasmJs).
3. Bottom-nav tab selection is a client-computed `NavItem.selected` flag (no runtime route serialization), not a destination-id match.
4. Cross-feature navigation targets are injected as `Any` routes by `:client` (no feature→feature api deps), rather than the reference's direct api import.
5. `NxBottomNav` + `NxEmptyState` are delivered by Plan 04 (Task 12 and Task 16); this plan consumes them (Task 3 is a SKIP to avoid overwriting Plan 04's `ctaText`-based `NxEmptyState`).
6. `DefaultImageCodec` is realized as an `expect`/`actual` factory function `defaultImageCodec()` (avoids the `expect class`-implements-interface actualization trap); same contract intent.
