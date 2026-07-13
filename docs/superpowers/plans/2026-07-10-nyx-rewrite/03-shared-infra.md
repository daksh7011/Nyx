# Shared Infrastructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the four shared infrastructure modules — `:shared:data` (result/error types, injected `Clock`, id generation, `DomainEventBus`, source-interface contracts), `:shared:presentation` (pawdex-ported `BaseViewModel`, `ViewState`/`MutableViewState`, `UiState`/`UiEvent`, `NavController` extensions), `:shared:test-support` (`FakeClock`, `DeterministicIdGenerator`, `createTestSqlDriver` expect/actual), `:shared:compose-test-support` (`testInfrastructureModule` + `runFeatureUiTest` harness) — plus the `:client` SqlDelight schema (`NyxDb`), `SqlDelightSource`, and `VaultSqlSource : VaultSource` verified against an in-memory `JdbcSqliteDriver` in `jvmTest`.

**Architecture:** `:shared:data` is a plain 6-target KMP library (no Compose) that `api`-depends on `:steganography` for `PixelImage`; every downstream feature and the app depend on its contracts. `:shared:presentation` is a 6-target KMP-Compose library ported verbatim from the pawdex/Baro `shared:presentation` (same members, same semantics, Nyx packages). `:shared:test-support` and `:shared:compose-test-support` are test-only libraries; the SqlDelight database and its `VaultSource` implementation live in `:client` (source-inversion: features declare `VaultSource`, `:client` owns the single DB). SqlDelight runs in `generateAsync = true` mode (suspend queries) and adapts its async schema onto synchronous JDBC/Native/Android drivers via the `synchronous()` extension.

**Tech Stack:** Kotlin 2.3.21, Compose Multiplatform 1.10.3 (runtime/foundation/material3/ui/ui-test), Koin 4.2.1 (`koin-core`), SqlDelight 2.3.2 (`runtime`, `coroutines-extensions`, `async-extensions`, `sqlite-driver`, `native-driver`, `android-driver`), kotlinx-coroutines 1.10.2 (core + test), kotlinx-datetime 0.7.1 (with `kotlin.time.Instant` canonical), JetBrains navigation-compose 2.9.2 + lifecycle-viewmodel-compose 2.10.0, `kotlin.uuid` (stdlib), kotlin.test, AGP 9.2.0 (`com.android.kotlin.multiplatform.library`).

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
- Injected `Clock` only — direct `kotlin.time.Clock.System`/`kotlinx.datetime` system access banned
  outside `SystemClock`.
- No feature-to-feature api dependencies. `DomainEventBus` = only cross-feature channel.
- Repository writes return `AppResult`, reads return `Flow`. Use cases = single-purpose classes
  with `operator fun invoke`, `factoryOf`-registered.
- Composables are dumb: no filtering/sorting/mapping/pluralization in UI — VM state exposes
  render-ready values.
- Tests: kotlin.test + kotlinx-coroutines-test + hand-written fakes only. No mockk/kotest/turbine.
- No `println`; logging via Kermit.
- Commit after every green test cycle (conventional commits).

Phase-specific constraints:

- **SqlDelight async/sync bridge (research-verified):** the database is generated with
  `generateAsync = true`, so all `.sq` mutation queries become `suspend` and `SELECT` queries
  return `Query<T>` whose `awaitAsList()` / `awaitAsOne()` are `suspend`. `NyxDb.Schema` is a
  `SqlSchema<QueryResult.AsyncValue<Unit>>`; to create it on a synchronous driver
  (`JdbcSqliteDriver`, `AndroidSqliteDriver`, `NativeSqliteDriver`) call
  `NyxDb.Schema.synchronous()` (from `app.cash.sqldelight.async.coroutines`, artifact
  `async-extensions`) which yields `SqlSchema<QueryResult.Value<Unit>>`. Query observation uses
  `app.cash.sqldelight.coroutines` `Query<T>.asFlow()` + `Flow<Query<T>>.mapToList(context)`
  (artifact `coroutines-extensions`), which are async-generation compatible.
- **Source interfaces are interface-only here.** `ImageCodec`, `VaultSource`, `VaultFileStore`,
  `SettingsSource`, `CameraSource`, `ShareSource` are declared in `:shared:data` with NO
  implementations — impls (`DefaultImageCodec`, `VaultSqlSource`, platform stores) live in
  `:client` and platform modules (this plan implements only `VaultSqlSource` in `:client`;
  the rest are later plans 05/07).
- **`:shared:data` api-depends on `:steganography`** for `PixelImage`
  (`class PixelImage(val width: Int, val height: Int, val pixels: IntArray)` — 00-INDEX
  §steganography). Plan 02 delivers the real class; plan 01's skeleton must at minimum expose
  this class for `:shared:data` to compile. If executing 03 before 02, verify `:steganography`
  exposes `PixelImage` first.
- **Time:** `kotlin.time.Instant` is the canonical instant type (kotlinx-datetime ≥ 0.7.0).
  Import `kotlin.time.Instant`; use `kotlin.time.Clock.System.now()` (only inside `SystemClock`)
  and kotlinx-datetime's `TimeZone` / `LocalDate` / `LocalDateTime` / `toLocalDateTime`. Opt in
  to `kotlin.time.ExperimentalTime` and `kotlin.uuid.ExperimentalUuidApi` at module level.
- **`:shared:presentation` is ported verbatim** from the pawdex/Baro `shared:presentation`
  (same members/semantics), with two contract-mandated adaptations documented in this plan:
  (1) `async`/`ui` take `force` and return `Job?` (00-INDEX contract), (2) `UiState.Error`'s
  throwable field is named `cause: Throwable?` (00-INDEX contract), not `th: Throwable`.
- **Interface contracts:** every public signature in this plan matches the `:shared:data`,
  `:shared:presentation`, and `:client` sections of `00-INDEX.md` exactly. Downstream plans
  (05, 06, 07) import them as written here.

## Context you need before starting (read once, facts baked in)

- **Reference source (working code this plan ports).** The pawdex clone the 00-INDEX cites
  (`/tmp/.../scratchpad/pawdex`) may be absent. The pawdex-derived project `Baro`
  (`/home/slothie/IdeaProjects/Baro`) carries the identical `shared:presentation` (same Kotlin
  2.3.21 / CMP 1.10.3 / lifecycle 2.10.0 / nav 2.9.2 / koin 4.2.1 versions) and was the source
  for the verbatim ports below. Everything needed is inlined in this plan — the reference is
  optional.
- **Module Gradle paths and packages** (00-INDEX module table):

  | Module | Gradle path | `projects.` accessor | Kotlin package root | Android namespace |
  |---|---|---|---|---|
  | data | `:shared:data` | `projects.shared.data` | `com.slothiesmooth.nyx.shared.data` | `com.slothiesmooth.nyx.shared.data` |
  | presentation | `:shared:presentation` | `projects.shared.presentation` | `com.slothiesmooth.nyx.shared.presentation` | `com.slothiesmooth.nyx.shared.presentation` |
  | test-support | `:shared:test-support` | `projects.shared.testSupport` | `com.slothiesmooth.nyx.shared.testsupport` | `com.slothiesmooth.nyx.shared.testsupport` |
  | compose-test-support | `:shared:compose-test-support` | `projects.shared.composeTestSupport` | `com.slothiesmooth.nyx.shared.composetestsupport` | `com.slothiesmooth.nyx.shared.composetestsupport` |
  | client | `:client` | `projects.client` | `com.slothiesmooth.nyx.client` | `com.slothiesmooth.nyx.client` |
  | steganography | `:steganography` | `projects.steganography` | `com.slothiesmooth.nyx.steganography` | — |

  Folder layout on disk: `shared/data/`, `shared/presentation/`, `shared/test-support/`,
  `shared/compose-test-support/`, `client/`.
- **Build-file style (from plan 04):** modules declare their plugins with catalog aliases and
  write the `kotlin { android { … } iosX64() … jvm() wasmJs { browser() } }` block directly.
  If plan 01 provides a convention plugin (`nyx.kmp.library` / `nyx.compose`) producing the
  identical configuration, applying it instead is fine — the *effective* config shown is what
  matters. Plan 01 owns `detekt`/`spotless` wiring globally; do NOT add per-module detekt blocks.
- **`android {}` block quirks (AGP 9 KMP plugin):** no `buildTypes`/flavors/BuildConfig;
  `withHostTestBuilder { }` is required so `commonTest` also runs as an Android host (JVM) test.
- **Catalog ownership:** plan 01 owns `gradle/libs.versions.toml`. Task 1 reconciles it — the
  alias names in plan 01 win; if plan 01 chose different names for the same coordinates, use
  plan 01's names throughout this plan.
- **Version-catalog aliases used in this plan:** `libs.plugins.kotlin.multiplatform`,
  `libs.plugins.compose.compiler`, `libs.plugins.compose.multiplatform`,
  `libs.plugins.android.kmp.library`, `libs.plugins.sqldelight`, `libs.kotlin.test`,
  `libs.kotlinx.coroutines.core`, `libs.kotlinx.coroutines.test`, `libs.kotlinx.datetime`,
  `libs.kotlinx.collections.immutable`, `libs.androidx.navigation.compose`,
  `libs.androidx.lifecycle.viewmodel.compose`, `libs.koin.core`, `libs.sqldelight.runtime`,
  `libs.sqldelight.coroutines.extensions`, `libs.sqldelight.async.extensions`,
  `libs.sqldelight.sqlite.driver`, `libs.sqldelight.native.driver`,
  `libs.sqldelight.android.driver`, `libs.versions.android.compileSdk`,
  `libs.versions.android.minSdk`, `libs.versions.sqldelight`.

---

### Task 1: Version-catalog reconciliation, module registration, and `:shared:data` build file

**Files:**
- Modify: `gradle/libs.versions.toml` (verify/add the entries below)
- Modify: `settings.gradle.kts` (verify the four `:shared:*` includes and `:client`)
- Create/Overwrite: `shared/data/build.gradle.kts`

**Interfaces:**
- Consumes: plan 01 module skeletons, Gradle 9.4.1 wrapper, `:steganography` `PixelImage`.
- Produces: a compiling empty `:shared:data` module every later data task builds on.

**Steps:**

- [ ] **Step 1: Reconcile the version catalog.** Open `gradle/libs.versions.toml`. Verify the
  entries below exist (plan 01 owns this file; if it named an alias differently for the same
  coordinate, keep plan 01's name and use it everywhere in this plan). Add whatever is missing:

```toml
[versions]
kotlin = "2.3.21"
agp = "9.2.0"
compose-multiplatform = "1.10.3"
koin = "4.2.1"
sqldelight = "2.3.2"
kotlinx-coroutines = "1.10.2"
kotlinx-datetime = "0.7.1"
kotlinx-collections-immutable = "0.4.0"
jetbrains-navigation = "2.9.2"
jetbrains-lifecycle = "2.10.0"
android-compileSdk = "36"
android-minSdk = "24"

[libraries]
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
kotlinx-collections-immutable = { module = "org.jetbrains.kotlinx:kotlinx-collections-immutable", version.ref = "kotlinx-collections-immutable" }
androidx-navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "jetbrains-navigation" }
androidx-lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "jetbrains-lifecycle" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines-extensions = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-async-extensions = { module = "app.cash.sqldelight:async-extensions", version.ref = "sqldelight" }
sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
android-kmp-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

- [ ] **Step 2: Verify module registration.** Run:

```bash
grep -nE ':shared:data|:shared:presentation|:shared:test-support|:shared:compose-test-support|:client' settings.gradle.kts
```

Expected: `include(...)` lines covering `:shared:data`, `:shared:presentation`,
`:shared:test-support`, `:shared:compose-test-support`, and `:client`. If any is missing, add it
to the `include(...)` list (plan 01 should already have registered them).

- [ ] **Step 3: Write the `:shared:data` build file.** Overwrite `shared/data/build.gradle.kts`
  with exactly (this is a plain KMP library — NO Compose plugins):

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.slothiesmooth.nyx.shared.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTestBuilder { }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm()
    wasmJs { browser() }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlin.uuid.ExperimentalUuidApi")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
        commonMain.dependencies {
            api(projects.steganography)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

- [ ] **Step 4: Verify the empty module compiles.** Run:

```bash
./gradlew :shared:data:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. (If it fails with `Unresolved reference` for `PixelImage`, the
`:steganography` skeleton does not yet expose `class PixelImage(width, height, pixels: IntArray)`
— add it there per 00-INDEX §steganography before proceeding.)

- [ ] **Step 5: Commit.**

```bash
git add gradle/libs.versions.toml settings.gradle.kts shared/data/build.gradle.kts
git commit -m "build(shared-data): module build wiring and catalog reconciliation"
```

---

### Task 2: `AppResult` / `AppError` + `map` / `getOrNull` helpers (TDD)

**Files:**
- Test: `shared/data/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/data/result/AppResultTest.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/result/AppResult.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/result/AppError.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/result/AppResultExtensions.kt`

**Interfaces:**
- Consumes: Task 1 module.
- Produces (00-INDEX §shared:data contract):
  - `sealed interface AppResult<out T> { data class Ok<T>(val value: T); data class Err(val cause: AppError) : AppResult<Nothing> }`
  - `sealed interface AppError { NotFound; Validation(message); Storage(message, cause); Permission; Conflict(message) }`
  - `inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R>`
  - `fun <T> AppResult<T>.getOrNull(): T?`

**Steps:**

- [ ] **Step 1: Write the failing test.** Create `AppResultTest.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class AppResultTest {

    @Test
    fun `map transforms the ok value`() {
        val result: AppResult<Int> = AppResult.Ok(21)
        assertEquals(AppResult.Ok(42), result.map { it * 2 })
    }

    @Test
    fun `map passes an error through unchanged`() {
        val error = AppResult.Err(AppError.NotFound)
        val mapped = error.map { "never" }
        assertSame(error, mapped)
    }

    @Test
    fun `getOrNull returns the value for ok`() {
        assertEquals("secret", AppResult.Ok("secret").getOrNull())
    }

    @Test
    fun `getOrNull returns null for err`() {
        assertNull(AppResult.Err(AppError.Permission).getOrNull())
    }

    @Test
    fun `errors carry their payloads`() {
        assertEquals("bad name", (AppError.Validation("bad name") as AppError.Validation).message)
        val cause = IllegalStateException("disk full")
        val storage = AppError.Storage("write failed", cause)
        assertEquals("write failed", storage.message)
        assertSame(cause, storage.cause)
    }
}
```

- [ ] **Step 2: Run it (red).** Run:

```bash
./gradlew :shared:data:jvmTest --tests "com.slothiesmooth.nyx.shared.data.result.AppResultTest"
```

Expected failure: compilation error in `commonTest`, `Unresolved reference 'AppResult'` (and
`AppError`, `map`, `getOrNull`). That is the red state.

- [ ] **Step 3: Write `AppError`.** Create `AppError.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.result

/**
 * Closed set of failure reasons returned by repository writes and source operations.
 */
sealed interface AppError {
    data object NotFound : AppError
    data class Validation(val message: String) : AppError
    data class Storage(val message: String, val cause: Throwable? = null) : AppError
    data object Permission : AppError
    data class Conflict(val message: String) : AppError
}
```

- [ ] **Step 4: Write `AppResult`.** Create `AppResult.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.result

/**
 * Result of an operation that can fail with a typed [AppError]. Repository/source writes return
 * this; reads return `Flow`.
 */
sealed interface AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>
    data class Err(val cause: AppError) : AppResult<Nothing>
}
```

- [ ] **Step 5: Write the helpers.** Create `AppResultExtensions.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.result

/**
 * Maps the success value, leaving an [AppResult.Err] untouched (returned as-is, so identity is
 * preserved for the error branch).
 */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Ok -> AppResult.Ok(transform(value))
    is AppResult.Err -> this
}

/**
 * Returns the success value, or `null` if this is an [AppResult.Err].
 */
fun <T> AppResult<T>.getOrNull(): T? = when (this) {
    is AppResult.Ok -> value
    is AppResult.Err -> null
}
```

- [ ] **Step 6: Run it (green).** Run:

```bash
./gradlew :shared:data:jvmTest --tests "com.slothiesmooth.nyx.shared.data.result.AppResultTest"
```

Expected: `BUILD SUCCESSFUL`, all 5 tests pass.

- [ ] **Step 7: Commit.**

```bash
git add shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/result shared/data/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/data/result
git commit -m "feat(shared-data): AppResult and AppError with map/getOrNull helpers"
```

---

### Task 2 note on `AppResult.Err` identity

`AppResult.Err` implements `AppResult<Nothing>`; because `AppResult` is declared `out T`
(covariant), an `Err` value is assignable to `AppResult<R>` for any `R`, which is why the
`is AppResult.Err -> this` branch in `map` compiles and why `assertSame` holds in the test.

---

### Task 3: `StegoImageId`, `IdGenerator`, `Uuid4IdGenerator` (TDD)

**Files:**
- Test: `shared/data/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/data/id/IdGeneratorTest.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/id/StegoImageId.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/id/IdGenerator.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/id/Uuid4IdGenerator.kt`

**Interfaces:**
- Consumes: Task 1 module.
- Produces (00-INDEX contract): `@JvmInline value class StegoImageId(val value: String)`;
  `interface IdGenerator { fun newId(): String }`; `class Uuid4IdGenerator : IdGenerator`
  (uses `kotlin.uuid.Uuid.random()`).

**Steps:**

- [ ] **Step 1: Write the failing test.** Create `IdGeneratorTest.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.id

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IdGeneratorTest {

    private val uuidV4Regex =
        Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

    @Test
    fun `generated ids are lowercase uuid version 4`() {
        val generator = Uuid4IdGenerator()
        repeat(50) {
            val id = generator.newId()
            assertTrue(uuidV4Regex.matches(id), "not a v4 uuid: $id")
        }
    }

    @Test
    fun `generated ids are unique across a large batch`() {
        val generator = Uuid4IdGenerator()
        val ids = HashSet<String>()
        repeat(1000) { ids.add(generator.newId()) }
        assertEquals(1000, ids.size)
    }

    @Test
    fun `stego image id wraps its raw string value`() {
        assertEquals("abc-123", StegoImageId("abc-123").value)
    }
}
```

- [ ] **Step 2: Run it (red).** Run:

```bash
./gradlew :shared:data:jvmTest --tests "com.slothiesmooth.nyx.shared.data.id.IdGeneratorTest"
```

Expected failure: `Unresolved reference 'Uuid4IdGenerator'` / `'StegoImageId'`.

- [ ] **Step 3: Write `StegoImageId`.** Create `StegoImageId.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.id

import kotlin.jvm.JvmInline

/**
 * Type-safe identifier for a stored stego image. Wraps the raw uuid string.
 */
@JvmInline
value class StegoImageId(val value: String)
```

- [ ] **Step 4: Write `IdGenerator`.** Create `IdGenerator.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.id

/**
 * Produces opaque unique identifiers. The production implementation is [Uuid4IdGenerator];
 * tests use `DeterministicIdGenerator` from `:shared:test-support`.
 */
interface IdGenerator {
    fun newId(): String
}
```

- [ ] **Step 5: Write `Uuid4IdGenerator`.** Create `Uuid4IdGenerator.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.id

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * [IdGenerator] backed by a random (version 4) UUID from the Kotlin stdlib. `Uuid.random()`
 * renders as the canonical lowercase hex-dash form via `toString()`.
 */
@OptIn(ExperimentalUuidApi::class)
class Uuid4IdGenerator : IdGenerator {
    override fun newId(): String = Uuid.random().toString()
}
```

- [ ] **Step 6: Run it (green).** Run:

```bash
./gradlew :shared:data:jvmTest --tests "com.slothiesmooth.nyx.shared.data.id.IdGeneratorTest"
```

Expected: `BUILD SUCCESSFUL`, all 3 tests pass.

- [ ] **Step 7: Commit.**

```bash
git add shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/id shared/data/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/data/id
git commit -m "feat(shared-data): StegoImageId value class and uuid4 IdGenerator"
```

---

### Task 4: `Clock` + `SystemClock` (TDD)

**Files:**
- Test: `shared/data/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/data/time/SystemClockTest.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/time/Clock.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/time/SystemClock.kt`

**Interfaces:**
- Consumes: Task 1 module.
- Produces (00-INDEX contract): `interface Clock { fun now(): Instant; fun today(): LocalDate; fun zone(): TimeZone; fun nowLocal(): LocalDateTime }` and `class SystemClock : Clock`.

**Steps:**

- [ ] **Step 1: Write the failing test.** SystemClock reads the real wall clock, so the test
  asserts wiring invariants only (deterministic date-derivation is covered by `FakeClock` in
  Task 10). Create `SystemClockTest.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.time

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

class SystemClockTest {

    @Test
    fun `now is a plausible current instant`() {
        val clock = SystemClock()
        assertTrue(clock.now() > Instant.parse("2020-01-01T00:00:00Z"))
    }

    @Test
    fun `now does not go backwards across two reads`() {
        val clock = SystemClock()
        val first = clock.now()
        val second = clock.now()
        assertTrue(second >= first)
    }

    @Test
    fun `today and nowLocal derive from the reported zone without throwing`() {
        val clock = SystemClock()
        val zone = clock.zone()
        // nowLocal and today both project now() into zone(); assert they are internally consistent
        // by projecting a single captured instant the same way the clock does.
        val captured = clock.now()
        val projected = captured.toLocalDateTimeIn(zone)
        assertTrue(projected.year >= 2020)
        // today() is derived the same way from a (fresh) now(); assert it is a valid date object.
        assertTrue(clock.today().dayOfMonth in 1..31)
    }
}
```

- [ ] **Step 2: Run it (red).** Run:

```bash
./gradlew :shared:data:jvmTest --tests "com.slothiesmooth.nyx.shared.data.time.SystemClockTest"
```

Expected failure: `Unresolved reference 'SystemClock'` (and `toLocalDateTimeIn`).

- [ ] **Step 3: Write the `Clock` interface.** Create `Clock.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Injected wall clock. Direct `kotlin.time.Clock.System` / `kotlinx.datetime` system access is
 * banned everywhere except [SystemClock]; all other code depends on this interface so time is
 * controllable in tests (see `FakeClock` in `:shared:test-support`).
 */
interface Clock {
    fun now(): Instant
    fun today(): LocalDate
    fun zone(): TimeZone
    fun nowLocal(): LocalDateTime
}

/**
 * Shared projection helper so [SystemClock], `FakeClock`, and tests derive local time identically.
 */
fun Instant.toLocalDateTimeIn(zone: TimeZone): LocalDateTime = toLocalDateTime(zone)
```

- [ ] **Step 4: Write `SystemClock`.** Create `SystemClock.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock as KotlinTimeClock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Production [Clock] reading the system wall clock and default time zone. This is the ONLY place
 * `kotlin.time.Clock.System` may be referenced (global constraint).
 */
@OptIn(ExperimentalTime::class)
class SystemClock : Clock {
    override fun now(): Instant = KotlinTimeClock.System.now()
    override fun zone(): TimeZone = TimeZone.currentSystemDefault()
    override fun nowLocal(): LocalDateTime = now().toLocalDateTimeIn(zone())
    override fun today(): LocalDate = nowLocal().date
}
```

- [ ] **Step 5: Run it (green).** Run:

```bash
./gradlew :shared:data:jvmTest --tests "com.slothiesmooth.nyx.shared.data.time.SystemClockTest"
```

Expected: `BUILD SUCCESSFUL`, all 3 tests pass.

- [ ] **Step 6: Commit.**

```bash
git add shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/time shared/data/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/data/time
git commit -m "feat(shared-data): injected Clock interface and SystemClock"
```

---

### Task 5: `DomainEvent` + `DomainEventBus` + `DefaultDomainEventBus` (TDD)

**Files:**
- Test: `shared/data/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/data/event/DomainEventBusTest.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/event/DomainEvent.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/event/DomainEventBus.kt`

**Interfaces:**
- Consumes: Task 3 (`StegoImageId`).
- Produces (00-INDEX contract): `sealed interface DomainEvent { StegoImageStored(id); StegoImageArchived(id); StegoImageRestored(id); StegoImageDeleted(id); VaultWiped }`;
  `interface DomainEventBus { val events: SharedFlow<DomainEvent>; suspend fun emit(event) }`;
  `class DefaultDomainEventBus : DomainEventBus` — `MutableSharedFlow(replay = 0, extraBufferCapacity = 64)`.

**Steps:**

- [ ] **Step 1: Write the failing test.** Create `DomainEventBusTest.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.event

import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DomainEventBusTest {

    @Test
    fun `emit delivers the event to an active collector`() = runTest {
        val bus = DefaultDomainEventBus()
        val stored = StegoImageId("img-1")
        val collected = async(UnconfinedTestDispatcher(testScheduler)) { bus.events.first() }
        runCurrent() // let the collector subscribe before we emit

        bus.emit(DomainEvent.StegoImageStored(stored))

        assertEquals(DomainEvent.StegoImageStored(stored), collected.await())
    }

    @Test
    fun `emit without subscribers does not suspend`() = runTest {
        val bus = DefaultDomainEventBus()
        // replay = 0 with a 64-slot buffer means emit returns immediately when nobody listens;
        // reaching the assertion proves there was no deadlock.
        bus.emit(DomainEvent.VaultWiped)
        assertEquals(0, bus.events.replayCache.size)
    }
}
```

- [ ] **Step 2: Run it (red).** Run:

```bash
./gradlew :shared:data:jvmTest --tests "com.slothiesmooth.nyx.shared.data.event.DomainEventBusTest"
```

Expected failure: `Unresolved reference 'DefaultDomainEventBus'` / `'DomainEvent'`.

- [ ] **Step 3: Write `DomainEvent`.** Create `DomainEvent.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.event

import com.slothiesmooth.nyx.shared.data.id.StegoImageId

/**
 * Cross-feature notifications. The [DomainEventBus] is the ONLY channel features use to react to
 * each other's state changes (no feature-to-feature api dependencies).
 */
sealed interface DomainEvent {
    data class StegoImageStored(val id: StegoImageId) : DomainEvent
    data class StegoImageArchived(val id: StegoImageId) : DomainEvent
    data class StegoImageRestored(val id: StegoImageId) : DomainEvent
    data class StegoImageDeleted(val id: StegoImageId) : DomainEvent
    data object VaultWiped : DomainEvent
}
```

- [ ] **Step 4: Write `DomainEventBus`.** Create `DomainEventBus.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Application-wide bus of [DomainEvent]s.
 */
interface DomainEventBus {
    val events: SharedFlow<DomainEvent>
    suspend fun emit(event: DomainEvent)
}

private const val EVENT_BUFFER_CAPACITY = 64

/**
 * Default bus: hot [MutableSharedFlow] with no replay (events are transient) and a 64-slot buffer
 * so emitters never suspend when no one is listening. Registered as an outer app singleton.
 */
class DefaultDomainEventBus : DomainEventBus {
    private val sink = MutableSharedFlow<DomainEvent>(
        replay = 0,
        extraBufferCapacity = EVENT_BUFFER_CAPACITY,
    )
    override val events: SharedFlow<DomainEvent> = sink.asSharedFlow()
    override suspend fun emit(event: DomainEvent) = sink.emit(event)
}
```

- [ ] **Step 5: Run it (green).** Run:

```bash
./gradlew :shared:data:jvmTest --tests "com.slothiesmooth.nyx.shared.data.event.DomainEventBusTest"
```

Expected: `BUILD SUCCESSFUL`, both tests pass.

- [ ] **Step 6: Commit.**

```bash
git add shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/event shared/data/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/data/event
git commit -m "feat(shared-data): DomainEvent and DefaultDomainEventBus"
```

---

### Task 6: Source interfaces + projection data classes (compile-verified)

These are pure contracts (interfaces + immutable data classes) with no logic, so verification is
a successful compile — do not write throwaway "it constructs" tests (tests serve purpose, not
count). Implementations live in `:client`/platform modules in later plans.

**Files:**
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/source/ImageCodec.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/source/StegoImageRecord.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/source/VaultSource.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/source/VaultFileStore.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/source/SettingsSource.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/source/CameraSource.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/source/ShareSource.kt`
- Create: `shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/source/PlatformCapabilities.kt`

**Interfaces:**
- Consumes: Task 2 (`AppResult`), Task 1 (`PixelImage` from `:steganography`).
- Produces: all source interfaces + `StegoImageRecord`, `PickedImage`, `PlatformCapabilities`
  exactly per 00-INDEX §shared:data.

**Steps:**

- [ ] **Step 1: Write `ImageCodec`.** Create `ImageCodec.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.source

import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.steganography.PixelImage

/**
 * Decodes encoded image bytes (JPEG/PNG) to a [PixelImage] and re-encodes to PNG (lossless out).
 * Contract: [decode] FORCES every pixel opaque (alpha = 0xFF) before returning — premultiplication
 * on Android/skiko round-trips corrupts the LSBs of alpha<255 pixels, which is fatal to LSB
 * steganography. Implementations live in `:client` (expect/actual: Android BitmapFactory /
 * skiko for the rest).
 */
interface ImageCodec {
    suspend fun decode(bytes: ByteArray): AppResult<PixelImage>
    suspend fun encodePng(image: PixelImage): AppResult<ByteArray>
}
```

- [ ] **Step 2: Write `StegoImageRecord`.** Create `StegoImageRecord.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.source

/**
 * Database projection of a stored stego image's metadata. Timestamps are ISO-8601 strings;
 * [deletedAt] non-null marks a tombstone. Byte content lives in [VaultFileStore], not here.
 */
data class StegoImageRecord(
    val id: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val isArchived: Boolean,
)
```

- [ ] **Step 3: Write `VaultSource`.** Create `VaultSource.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.source

import kotlinx.coroutines.flow.Flow

/**
 * Metadata store for stego images (the DB seam). Reads return `Flow`; writes are suspend.
 * `:client`'s `VaultSqlSource` implements this over SqlDelight; web uses an in-memory impl.
 */
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
```

- [ ] **Step 4: Write `VaultFileStore`.** Create `VaultFileStore.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.source

import com.slothiesmooth.nyx.shared.data.result.AppResult

/**
 * PNG-byte store for stego images, keyed by id (FileKit vault directory on non-web; in-memory on
 * web). Metadata lives in [VaultSource].
 */
interface VaultFileStore {
    suspend fun write(id: String, bytes: ByteArray): AppResult<Unit>
    suspend fun read(id: String): AppResult<ByteArray>
    suspend fun delete(id: String): AppResult<Unit>
    suspend fun deleteAll(): AppResult<Unit>
}
```

- [ ] **Step 5: Write `SettingsSource`.** Create `SettingsSource.kt`:

```kotlin
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
```

- [ ] **Step 6: Write `CameraSource`.** Create `CameraSource.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.source

/**
 * A single freshly-captured image and its suggested file name (`null` name = none available).
 */
data class PickedImage(val bytes: ByteArray, val suggestedName: String?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedImage) return false
        return bytes.contentEquals(other.bytes) && suggestedName == other.suggestedName
    }

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + (suggestedName?.hashCode() ?: 0)
}

/**
 * System-camera capture (FileKit `openCameraPicker` on android/ios; unavailable on desktop/web).
 */
interface CameraSource {
    val isAvailable: Boolean
    suspend fun capture(): PickedImage?
}
```

- [ ] **Step 7: Write `ShareSource`.** Create `ShareSource.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.source

import com.slothiesmooth.nyx.shared.data.result.AppResult

/**
 * Exports a PNG: Android/iOS share sheet, desktop save dialog, web browser download.
 */
interface ShareSource {
    suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit>
}
```

- [ ] **Step 8: Write `PlatformCapabilities`.** Create `PlatformCapabilities.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.data.source

/**
 * Feature flags a platform module reports so the UI can hide unsupported affordances (e.g. no
 * camera or no persistent vault on web).
 */
data class PlatformCapabilities(val camera: Boolean, val persistentVault: Boolean)
```

- [ ] **Step 9: Verify the whole module compiles across all sources.** Run:

```bash
./gradlew :shared:data:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. (`PickedImage` overrides `equals`/`hashCode` because it holds a
`ByteArray`; detekt is satisfied and array-content equality is correct.)

- [ ] **Step 10: Commit.**

```bash
git add shared/data/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/data/source
git commit -m "feat(shared-data): source interfaces and projection data classes"
```

---

### Task 7: `:shared:presentation` build file + `UiState`/`UiEvent` + `ViewState`/`MutableViewState` + `tryCatch` (TDD)

**Files:**
- Create/Overwrite: `shared/presentation/build.gradle.kts`
- Create: `shared/presentation/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/presentation/state/UiEvent.kt`
- Create: `shared/presentation/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/presentation/state/UiState.kt`
- Create: `shared/presentation/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/presentation/state/ViewState.kt`
- Test: `shared/presentation/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/presentation/state/MutableViewStateTest.kt`

**Interfaces:**
- Consumes: plan 01 skeleton, catalog (Task 1).
- Produces (00-INDEX §shared:presentation contract):
  - `interface UiEvent`
  - `sealed interface UiState { Ready; Loading; Blocking; Error(title: String, cause: Throwable?, onExit: () -> Unit) }`
  - `@Stable interface ViewState { val uiState: UiState; val uiEvent: Flow<UiEvent> }`
  - `abstract class MutableViewState : ViewState` (`uiState by mutableStateOf`, `uiEvent = MutableSharedFlow`)
  - `suspend fun <T : MutableViewState> T.tryCatch(title, onTry, onCatch = …)` and `T.notify(event)`.

**Steps:**

- [ ] **Step 1: Write the `:shared:presentation` build file.** Overwrite
  `shared/presentation/build.gradle.kts` with exactly:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.slothiesmooth.nyx.shared.presentation"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTestBuilder { }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm()
    wasmJs { browser() }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            }
        }
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(libs.androidx.navigation.compose)
            api(libs.androidx.lifecycle.viewmodel.compose)
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

- [ ] **Step 2: Write `UiEvent`.** Create `UiEvent.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.presentation.state

import androidx.compose.runtime.Stable

/**
 * Base type for one-shot UI events (navigation, toasts) delivered through [ViewState.uiEvent].
 */
@Stable
interface UiEvent
```

- [ ] **Step 3: Write `UiState`.** Create `UiState.kt` (note the contract field name `cause`,
  nullable — this is the documented adaptation from pawdex's `th: Throwable`):

```kotlin
package com.slothiesmooth.nyx.shared.presentation.state

import androidx.compose.runtime.Stable

/**
 * Coarse rendering state a screen can be in. `Loading` overlays non-blocking progress; `Blocking`
 * shows a modal spinner; `Error` shows a dismissible dialog.
 */
@Stable
sealed interface UiState {
    data object Ready : UiState
    data object Loading : UiState
    data object Blocking : UiState

    data class Error(
        val title: String,
        val cause: Throwable?,
        val onExit: () -> Unit,
    ) : UiState
}

/**
 * Builds an [UiState.Error] from a throwable, keeping the exception as [UiState.Error.cause].
 */
fun Throwable.toErrorState(title: String, onExit: () -> Unit): UiState.Error =
    UiState.Error(title = title, cause = this, onExit = onExit)
```

- [ ] **Step 4: Write `ViewState` + `MutableViewState` + `tryCatch`/`notify`.** Create
  `ViewState.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.presentation.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Read-only view of a screen's state: coarse [uiState] plus a stream of one-shot [uiEvent]s.
 */
@Stable
interface ViewState {
    val uiState: UiState
    val uiEvent: Flow<UiEvent>
}

/**
 * Mutable [ViewState] base a screen's concrete state extends. [uiState] is Compose snapshot state
 * so writes trigger recomposition; [uiEvent] is a hot flow one-shot events are emitted onto.
 */
abstract class MutableViewState(initialState: UiState = UiState.Ready) : ViewState {
    final override var uiEvent: MutableSharedFlow<UiEvent> = MutableSharedFlow()
    final override var uiState by mutableStateOf(initialState)
}

/**
 * Runs [onTry]; on a non-cancellation throwable, maps it to [UiState.Error] whose `onExit` resets
 * the state to [UiState.Ready]. Cancellation is rethrown-safe (swallowed here, never surfaced as
 * an error dialog).
 */
suspend fun <T : MutableViewState> T.tryCatch(
    title: String,
    onTry: suspend T.() -> Unit,
    onCatch: suspend T.(Throwable) -> Unit = { throwable ->
        if (!throwable.isCancellation()) {
            uiState = throwable.toErrorState(title) { uiState = UiState.Ready }
        }
    },
) {
    runCatching { onTry() }.onFailure { onCatch(it) }
}

/**
 * Emits a one-shot [UiEvent] to subscribers of [ViewState.uiEvent].
 */
suspend fun <T : MutableViewState> T.notify(event: UiEvent) {
    uiEvent.emit(event)
}

private fun Throwable.isCancellation(): Boolean {
    var current: Throwable? = this
    while (current != null && current !is CancellationException) {
        if (current == current.cause) return false
        current = current.cause
    }
    return current is CancellationException
}
```

- [ ] **Step 5: Write the failing test.** Create `MutableViewStateTest.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.presentation.state

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class SampleState : MutableViewState()

class MutableViewStateTest {

    @Test
    fun `tryCatch maps a throwable to an error state`() = runTest {
        val state = SampleState()

        state.tryCatch(title = "Encrypt failed", onTry = { throw IllegalStateException("boom") })

        val error = state.uiState
        assertIs<UiState.Error>(error)
        assertEquals("Encrypt failed", error.title)
        assertEquals("boom", error.cause?.message)
    }

    @Test
    fun `error onExit resets state to ready`() = runTest {
        val state = SampleState()

        state.tryCatch(title = "Boom", onTry = { throw RuntimeException("x") })
        val error = state.uiState
        assertIs<UiState.Error>(error)
        error.onExit()

        assertEquals(UiState.Ready, state.uiState)
    }

    @Test
    fun `tryCatch swallows cancellation and stays ready`() = runTest {
        val state = SampleState()

        state.tryCatch(title = "Boom", onTry = { throw CancellationException("cancelled") })

        assertEquals(UiState.Ready, state.uiState)
    }
}
```

- [ ] **Step 6: Run it (red then green).** Run:

```bash
./gradlew :shared:presentation:jvmTest --tests "com.slothiesmooth.nyx.shared.presentation.state.MutableViewStateTest"
```

Expected: with the files from Steps 2-4 in place, `BUILD SUCCESSFUL`, all 3 tests pass. (If you
staged the test before the production files, the red state is `Unresolved reference
'MutableViewState'`.)

- [ ] **Step 7: Commit.**

```bash
git add shared/presentation/build.gradle.kts shared/presentation/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/presentation/state shared/presentation/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/presentation/state
git commit -m "feat(shared-presentation): UiState, ViewState, MutableViewState with tryCatch"
```

---

### Task 8: `BaseViewModel` (named-job dedup, `async`/`ui`, `withState`, lifecycle `bind()`) (TDD)

**Files:**
- Create: `shared/presentation/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/presentation/viewmodel/BaseViewModel.kt`
- Test: `shared/presentation/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/presentation/viewmodel/BaseViewModelTest.kt`

**Interfaces:**
- Consumes: `androidx.lifecycle.ViewModel`/`viewModelScope` (JetBrains `lifecycle-viewmodel-compose`
  in commonMain), Compose `Snapshot`.
- Produces (00-INDEX contract — note the two documented adaptations: `async`/`ui` take `force` and
  return `Job?`):

```kotlin
@Immutable abstract class BaseViewModel : ViewModel() {
    protected fun async(id: String, force: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job?
    protected fun ui(id: String, force: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job?
    protected fun withState(block: () -> Unit)
    protected open fun doInit() {}
    protected open fun doBind() {}
    @Composable protected open fun DoBind() {}
    protected open fun doResume() {}
    protected open fun doPause() {}
    protected open fun doDispose() {}
    @Composable fun bind()
}
```

**Steps:**

- [ ] **Step 1: Write `BaseViewModel`.** Create `BaseViewModel.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.presentation.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Lifecycle-aware ViewModel base ported from the pawdex/Baro `shared:presentation`. Coroutines are
 * launched through a named-job map so a second launch with the same [id] is skipped while the
 * first is in flight (unless `force = true`, which cancels and replaces it). `bind()` wires the
 * Compose lifecycle to the `doInit`/`doBind`/`doResume`/`doPause`/`doDispose` hooks.
 *
 * Reference: https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-lifecycle.html
 */
@Immutable
abstract class BaseViewModel : ViewModel() {

    private val jobs = mutableMapOf<String, Job>()
    private var initialized = false

    /** Takes a mutable snapshot and runs [block] within it on the main thread. */
    protected fun withState(block: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            Snapshot.withMutableSnapshot(block)
        }
    }

    /** Launches [block] on the main dispatcher under the named-job dedup keyed by [id]. */
    protected fun ui(
        id: String,
        force: Boolean = false,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? = launch(id = id, force = force, context = Dispatchers.Main, block = block)

    /** Launches [block] on the default dispatcher under the named-job dedup keyed by [id]. */
    protected fun async(
        id: String,
        force: Boolean = false,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? = launch(id = id, force = force, context = Dispatchers.Default, block = block)

    private fun launch(
        id: String,
        force: Boolean,
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? {
        val existing = jobs[id]
        return when {
            force -> {
                existing?.cancel()
                viewModelScope.launch(context = context, block = block).also { jobs[id] = it }
            }

            existing == null || existing.isCompleted -> {
                viewModelScope.launch(context = context, block = block).also { jobs[id] = it }
            }

            else -> null
        }
    }

    protected open fun doInit() = Unit

    @Composable
    protected open fun DoBind() = Unit

    protected open fun doBind() = Unit

    protected open fun doResume() = Unit

    protected open fun doPause() = Unit

    protected open fun doDispose() = Unit

    /** Binds the ViewModel to the current Composable lifecycle. */
    @Composable
    fun bind() {
        DoBind()
        val owner = LocalLifecycleOwner.current
        LaunchedEffect(owner) {
            if (!initialized) {
                initialized = true
                doInit()
            }
            doBind()
            var initialRequest = true
            owner.lifecycle.currentStateFlow.collect { state ->
                when (state) {
                    Lifecycle.State.RESUMED -> {
                        if (!initialRequest) {
                            doResume()
                        }
                        initialRequest = false
                    }

                    Lifecycle.State.STARTED -> {
                        if (!initialRequest) {
                            doPause()
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    override fun onCleared() {
        doDispose()
    }
}
```

- [ ] **Step 2: Write the failing test.** The `@Composable bind()` lifecycle path is exercised by
  feature UI tests later (plans 05/06 via `runFeatureUiTest`); here we unit-test the coroutine
  primitives directly through a test subclass, driving everything on a `StandardTestDispatcher`
  installed as `Main`. Create `BaseViewModelTest.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class ProbeViewModel : BaseViewModel() {
    var marks = 0
        private set
    var counter by mutableStateOf(0)
        private set

    fun mark() {
        marks++
    }

    fun runUi(id: String, force: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job? =
        ui(id, force, block)

    fun runAsync(id: String, force: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job? =
        async(id, force, block)

    fun bumpViaWithState() = withState { counter++ }
}

class BaseViewModelTest {

    @BeforeTest
    fun installMain() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun removeMain() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ui with the same id is deduplicated while in flight`() = runTest {
        val vm = ProbeViewModel()
        val gate = CompletableDeferred<Unit>()

        val first = vm.runUi("load") { vm.mark(); gate.await() }
        val second = vm.runUi("load") { vm.mark(); gate.await() }

        assertNotNull(first)
        assertNull(second) // skipped: first is still active
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, vm.marks) // the block ran exactly once
    }

    @Test
    fun `ui with force cancels the in-flight job and runs the replacement`() = runTest {
        val vm = ProbeViewModel()
        val gate = CompletableDeferred<Unit>()

        val first = vm.runUi("load") { gate.await(); vm.mark() }
        runCurrent() // let first start and suspend at the gate
        val second = vm.runUi("load", force = true) { vm.mark() }

        assertNotNull(second)
        assertTrue(first!!.isCancelled)
        advanceUntilIdle()
        assertEquals(1, vm.marks) // only the replacement marked; the cancelled job never reached mark()
    }

    @Test
    fun `async returns the running job and dedups by id`() = runTest {
        val vm = ProbeViewModel()
        val gate = CompletableDeferred<Unit>()

        val first = vm.runAsync("sync") { gate.await() }
        val second = vm.runAsync("sync") { gate.await() }

        assertNotNull(first)
        assertNull(second)
        gate.complete(Unit)
        first?.join()
    }

    @Test
    fun `withState mutation is visible after the dispatcher drains`() = runTest {
        val vm = ProbeViewModel()

        vm.bumpViaWithState()
        advanceUntilIdle()

        assertEquals(1, vm.counter)
    }
}
```

- [ ] **Step 3: Run it (green).** Run:

```bash
./gradlew :shared:presentation:jvmTest --tests "com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModelTest"
```

Expected: `BUILD SUCCESSFUL`, all 4 tests pass. (Dedup return values are deterministic because
`viewModelScope.launch` returns its `Job` synchronously and the in-flight job's `isCompleted` is
`false` while it awaits the gate; `ui`/`withState` run on `Main` = the installed test dispatcher,
so `advanceUntilIdle()` drives their bodies deterministically.)

- [ ] **Step 4: Commit.**

```bash
git add shared/presentation/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/presentation/viewmodel shared/presentation/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/presentation/viewmodel
git commit -m "feat(shared-presentation): BaseViewModel with named-job dedup and lifecycle bind"
```

---

### Task 9: `NavController` extensions + `ViewStateHandler` (compile-verified)

`NavController` extensions are thin wrappers whose behavior is the navigation library's; unit
testing them needs a live `NavController` graph (a feature-UI-test concern, exercised in plans
05/06). `ViewStateHandler` is a dumb renderer (no logic). Verification here is a successful
compile.

**Files:**
- Create: `shared/presentation/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/presentation/navigation/NavControllerExtensions.kt`
- Create: `shared/presentation/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/presentation/state/ViewStateHandler.kt`

**Interfaces:**
- Consumes: `androidx.navigation.NavController` (JetBrains navigation-compose), Task 7 `UiState`.
- Produces (00-INDEX contract): `NavController.pushDestination/popDestination/setDestination/restoreDestination(route: Any)`;
  `@Composable fun ViewStateHandler(...)`.

Note on `getDestinationId`: the 00-INDEX contract lists four extensions (push/pop/set/restore).
The pawdex/Baro reference contains push/pop/set/restore but NOT a `generateHashCode`-based
`getDestinationId` (the task says "port what exists"), so it is omitted. There is NO shared
`replaceDestination` — per 00-INDEX, `popUpTo(Any)` is ambiguous on wasm, so `FeatureHostContext`
(plan 05) inlines replace as `popUpTo(currentDestination?.route: String)`.

**Steps:**

- [ ] **Step 1: Write the NavController extensions.** Create `NavControllerExtensions.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.navOptions

/** Pops the back stack if there is a previous entry. */
fun NavController.popDestination() {
    if (previousBackStackEntry != null) {
        popBackStack()
    }
}

/** Navigates to a new instance of [route]. */
fun NavController.pushDestination(route: Any) {
    navigate(route)
}

/**
 * Navigates to [route] as a single top instance, popping any existing instance of it first.
 */
fun NavController.restoreDestination(route: Any) {
    navigate(
        route,
        navOptions {
            popUpTo(route) { inclusive = true }
            launchSingleTop = true
            restoreState = false
        },
    )
}

/** Clears back to the graph start (exclusive) and navigates to [route] — used for tab switches. */
fun NavController.setDestination(route: Any) {
    navigate(
        route,
        navOptions {
            graph.startDestinationRoute?.let { graphRoute ->
                popUpTo(graphRoute) { inclusive = false }
            }
        },
    )
}
```

- [ ] **Step 2: Write `ViewStateHandler`.** Create `ViewStateHandler.kt` (ported from pawdex/Baro,
  adapted to `UiState.Error.cause`):

```kotlin
package com.slothiesmooth.nyx.shared.presentation.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.filterNotNull

private val DialogPadding = 24.dp
private val CardPadding = 16.dp
private val SpinnerSize = 40.dp
private val SpinnerStroke = 3.dp

/**
 * Renders [content] and overlays the state-driven affordances (blocking spinner / loading slot /
 * error dialog) for [state], forwarding one-shot [UiEvent]s to [onEvent].
 */
@Composable
fun ViewStateHandler(
    state: ViewState,
    onEvent: suspend (UiEvent) -> Unit = {},
    blockingSlot: @Composable (UiState.Blocking) -> Unit = { ViewStateBlocking() },
    loadingSlot: @Composable (UiState.Loading) -> Unit = {},
    errorSlot: @Composable (UiState.Error) -> Unit = { ViewStateError(it) },
    content: @Composable () -> Unit,
) {
    LaunchedEffect(state) {
        state.uiEvent.filterNotNull().collect(onEvent)
    }

    content()

    StateOverlay(
        state = state,
        blockingSlot = blockingSlot,
        loadingSlot = loadingSlot,
        errorSlot = errorSlot,
    )
}

@Composable
@NonRestartableComposable
private fun StateOverlay(
    state: ViewState,
    blockingSlot: @Composable (UiState.Blocking) -> Unit,
    loadingSlot: @Composable (UiState.Loading) -> Unit,
    errorSlot: @Composable (UiState.Error) -> Unit,
) {
    when (val uiState = state.uiState) {
        is UiState.Blocking -> blockingSlot(uiState)
        is UiState.Loading -> loadingSlot(uiState)
        is UiState.Error -> errorSlot(uiState)
        else -> Unit
    }
}

/** Default modal blocking spinner. */
@Composable
fun ViewStateBlocking() {
    BasicAlertDialog(onDismissRequest = {}) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card {
                CircularProgressIndicator(
                    modifier = Modifier.padding(CardPadding).size(SpinnerSize),
                    strokeWidth = SpinnerStroke,
                )
            }
        }
    }
}

/** Default error dialog for [UiState.Error]. */
@Composable
fun ViewStateError(uiState: UiState.Error) {
    AlertDialog(
        modifier = Modifier.padding(DialogPadding),
        onDismissRequest = uiState.onExit,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
        title = { Text(text = uiState.title) },
        text = {
            Text(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                text = uiState.cause?.message
                    ?: uiState.cause?.stackTraceToString()
                    ?: uiState.title,
            )
        },
        confirmButton = {
            TextButton(onClick = uiState.onExit) { Text("OK") }
        },
    )
}
```

- [ ] **Step 3: Verify the module compiles.** Run:

```bash
./gradlew :shared:presentation:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. (dp literals are named `private val` file constants, satisfying
detekt MagicNumber via `ignorePropertyDeclaration`; no `@Suppress`.)

- [ ] **Step 4: Commit.**

```bash
git add shared/presentation/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/presentation/navigation shared/presentation/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/presentation/state/ViewStateHandler.kt
git commit -m "feat(shared-presentation): NavController extensions and ViewStateHandler"
```

---

### Task 10: `:shared:test-support` build + `FakeClock` + `DeterministicIdGenerator` (TDD)

**Files:**
- Create/Overwrite: `shared/test-support/build.gradle.kts`
- Create: `shared/test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/time/FakeClock.kt`
- Create: `shared/test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/id/DeterministicIdGenerator.kt`
- Test: `shared/test-support/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/testsupport/TestDoublesTest.kt`

**Interfaces:**
- Consumes: `:shared:data` (`Clock`, `IdGenerator`, `toLocalDateTimeIn`).
- Produces (00-INDEX contract): `class FakeClock(var fixed: Instant) : Clock` with `advance(...)`;
  `class DeterministicIdGenerator(prefix, counter) : IdGenerator`.

**Steps:**

- [ ] **Step 1: Write the build file.** Overwrite `shared/test-support/build.gradle.kts` with
  exactly (it `api`-exposes `:shared:data` and the SqlDelight runtime; the JDBC driver used by the
  jvm+android host-test `createTestSqlDriver` actual is added in Task 11 via an `androidJvmMain`
  intermediate source set):

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.slothiesmooth.nyx.shared.testsupport"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTestBuilder { }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm()
    wasmJs { browser() }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
        commonMain.dependencies {
            api(projects.shared.data)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.coroutines.core)
            api(libs.sqldelight.runtime)
            api(libs.sqldelight.async.extensions)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
```

- [ ] **Step 2: Write the failing test.** Create `TestDoublesTest.kt` (this is where the
  deterministic date-derivation logic — shared with `SystemClock` — is actually asserted):

```kotlin
package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.testsupport.id.DeterministicIdGenerator
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class TestDoublesTest {

    @Test
    fun `fake clock returns the fixed instant`() {
        val fixed = Instant.parse("2026-07-13T00:00:00Z")
        assertEquals(fixed, FakeClock(fixed).now())
    }

    @Test
    fun `advance moves the fixed instant forward`() {
        val start = Instant.parse("2026-07-13T00:00:00Z")
        val clock = FakeClock(start)
        clock.advance(3.hours)
        assertEquals(start + 3.hours, clock.now())
    }

    @Test
    fun `today derives from the fixed instant in the configured zone`() {
        val clock = FakeClock(Instant.parse("2026-07-13T10:00:00Z"))
        assertEquals(LocalDate(2026, 7, 13), clock.today())
        assertEquals(2026, clock.nowLocal().year)
    }

    @Test
    fun `deterministic id generator emits sequential prefixed ids`() {
        val generator = DeterministicIdGenerator(prefix = "img", counter = 0)
        assertEquals("img-0", generator.newId())
        assertEquals("img-1", generator.newId())
        assertEquals("img-2", generator.newId())
    }
}
```

- [ ] **Step 3: Run it (red).** Run:

```bash
./gradlew :shared:test-support:jvmTest --tests "com.slothiesmooth.nyx.shared.testsupport.TestDoublesTest"
```

Expected failure: `Unresolved reference 'FakeClock'` / `'DeterministicIdGenerator'`.

- [ ] **Step 4: Write `FakeClock`.** Create `FakeClock.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.testsupport.time

import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.shared.data.time.toLocalDateTimeIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Deterministic [Clock] for tests: reports a [fixed] instant (default zone UTC) and derives local
 * time exactly as `SystemClock` does. [advance] moves the fixed instant forward.
 */
@OptIn(ExperimentalTime::class)
class FakeClock(
    var fixed: Instant,
    private val zone: TimeZone = TimeZone.UTC,
) : Clock {
    override fun now(): Instant = fixed
    override fun zone(): TimeZone = zone
    override fun nowLocal(): LocalDateTime = fixed.toLocalDateTimeIn(zone)
    override fun today(): LocalDate = nowLocal().date

    fun advance(by: Duration) {
        fixed += by
    }
}
```

- [ ] **Step 5: Write `DeterministicIdGenerator`.** Create `DeterministicIdGenerator.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.testsupport.id

import com.slothiesmooth.nyx.shared.data.id.IdGenerator

/**
 * Deterministic [IdGenerator] for tests: emits `"$prefix-$n"` starting at [counter], incrementing
 * on each call, so generated ids are stable and readable in assertions.
 */
class DeterministicIdGenerator(
    private val prefix: String = "id",
    counter: Int = 0,
) : IdGenerator {
    private var next = counter
    override fun newId(): String = "$prefix-${next++}"
}
```

- [ ] **Step 6: Run it (green).** Run:

```bash
./gradlew :shared:test-support:jvmTest --tests "com.slothiesmooth.nyx.shared.testsupport.TestDoublesTest"
```

Expected: `BUILD SUCCESSFUL`, all 4 tests pass.

- [ ] **Step 7: Commit.**

```bash
git add shared/test-support/build.gradle.kts shared/test-support/src/commonMain/kotlin shared/test-support/src/commonTest/kotlin
git commit -m "feat(test-support): FakeClock and DeterministicIdGenerator"
```

---

### Task 11: `:shared:test-support` `createTestSqlDriver` expect/actual

Provides the cross-platform in-memory SQL driver seam. It is schema-agnostic (takes a
`SqlSchema<QueryResult.Value<Unit>>` so `:shared:test-support` never depends on `:client`); callers
pass `NyxDb.Schema.synchronous()`.

**Files:**
- Modify: `shared/test-support/build.gradle.kts` (add the `androidJvmMain` intermediate + drivers)
- Create: `shared/test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/db/TestSqlDriver.kt`
- Create: `shared/test-support/src/androidJvmMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/db/TestSqlDriver.androidJvm.kt`
- Create: `shared/test-support/src/iosMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/db/TestSqlDriver.ios.kt`
- Create: `shared/test-support/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/db/TestSqlDriver.wasmJs.kt`

**Interfaces:**
- Consumes: SqlDelight runtime (`SqlDriver`, `SqlSchema`, `QueryResult`).
- Produces: `expect fun createTestSqlDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver`
  — jvm+android actual = `JdbcSqliteDriver(IN_MEMORY)` + synchronous schema create; ios actual =
  in-memory `NativeSqliteDriver`; wasm actual = throws (no wasm DB driver in v1).

**Steps:**

- [ ] **Step 1: Add the intermediate source set + platform drivers to the build.** Edit
  `shared/test-support/build.gradle.kts`, adding an `androidJvmMain` intermediate (shared by the
  `jvm` and `android` targets, holding the single JDBC-based actual) and the per-target driver
  dependencies. The full `sourceSets { }` block becomes:

```kotlin
    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
        // Shared by jvm + android: their `createTestSqlDriver` actual both use JdbcSqliteDriver.
        // Android host (JVM) unit tests are the only android consumer of this test-support module.
        val androidJvmMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        jvmMain.get().dependsOn(androidJvmMain)
        androidMain.get().dependsOn(androidJvmMain)

        commonMain.dependencies {
            api(projects.shared.data)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.coroutines.core)
            api(libs.sqldelight.runtime)
            api(libs.sqldelight.async.extensions)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
```

- [ ] **Step 2: Write the `expect`.** Create `TestSqlDriver.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.testsupport.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * Creates a fresh in-memory SQL driver with [schema] already created, for isolated tests. Callers
 * pass a synchronous schema, e.g. `NyxDb.Schema.synchronous()` (the database is generated with
 * `generateAsync = true`, so its `Schema` is async until adapted). Not available on wasmJs in v1.
 */
expect fun createTestSqlDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver
```

- [ ] **Step 3: Write the jvm+android actual.** Create `TestSqlDriver.androidJvm.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.testsupport.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

/**
 * jvm + android-host actual: an in-memory JDBC SQLite driver with the schema created synchronously.
 */
actual fun createTestSqlDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    schema.create(driver)
    return driver
}
```

- [ ] **Step 4: Write the iOS actual.** Create `TestSqlDriver.ios.kt` (compiles only on macOS;
  never gate Linux progress on it):

```kotlin
package com.slothiesmooth.nyx.shared.testsupport.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS actual: an in-memory Native SQLite driver (the driver creates the schema itself).
 */
actual fun createTestSqlDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver =
    NativeSqliteDriver(
        schema = schema,
        name = "nyx-test.db",
        onConfiguration = { config -> config.copy(inMemory = true) },
    )
```

- [ ] **Step 5: Write the wasm actual.** Create `TestSqlDriver.wasmJs.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.testsupport.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * wasmJs actual: there is no SqlDelight driver on wasm in Nyx v1 (the web vault is in-memory), so
 * any attempt to build a test DB on wasm is a programming error. No wasm test uses this.
 */
actual fun createTestSqlDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver =
    throw NotImplementedError("No SqlDelight driver on wasmJs in Nyx v1; the web vault is in-memory.")
```

- [ ] **Step 6: Verify jvm compiles (the actual used by `:client` jvmTest).** Run:

```bash
./gradlew :shared:test-support:compileKotlinJvm :shared:test-support:compileKotlinWasmJs
```

Expected: `BUILD SUCCESSFUL` for both. (iOS actual is verified on the macOS lane later.)

- [ ] **Step 7: Commit.**

```bash
git add shared/test-support/build.gradle.kts shared/test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/testsupport/db shared/test-support/src/androidJvmMain shared/test-support/src/iosMain shared/test-support/src/wasmJsMain
git commit -m "feat(test-support): createTestSqlDriver expect/actual with in-memory drivers"
```

---

### Task 12: `:shared:compose-test-support` — `testInfrastructureModule` + `runFeatureUiTest`

Provides the shared feature-UI-test harness: a Koin module binding the common deterministic test
doubles, plus a `runComposeUiTest`-based helper that starts an isolated Koin, renders content, and
runs assertions. The DB is NOT wired here (kept decoupled from `:client`); a feature test that
needs a DB composes `testInfrastructureModule()` with its own module that builds `VaultSqlSource`
over `createTestSqlDriver(NyxDb.Schema.synchronous())`. See Open Questions re: the exact pawdex
`FeatureUiTest.kt` shape (unavailable) and the wasm exclusion.

**Files:**
- Create/Overwrite: `shared/compose-test-support/build.gradle.kts`
- Create: `shared/compose-test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/composetestsupport/TestInfrastructureModule.kt`
- Create: `shared/compose-test-support/src/commonMain/kotlin/com/slothiesmooth/nyx/shared/composetestsupport/FeatureUiTest.kt`
- Test: `shared/compose-test-support/src/commonTest/kotlin/com/slothiesmooth/nyx/shared/composetestsupport/TestInfrastructureModuleTest.kt`

**Interfaces:**
- Consumes: `:shared:data` (`Clock`, `IdGenerator`, `DomainEventBus`, `DefaultDomainEventBus`),
  `:shared:test-support` (`FakeClock`, `DeterministicIdGenerator`), Koin, Compose ui-test.
- Produces: `fun testInfrastructureModule(clock, idGenerator): Module`; `@OptIn(ExperimentalTestApi::class) fun runFeatureUiTest(module, content, assertions)`.

**Steps:**

- [ ] **Step 1: Write the build file.** This module targets android + iOS + jvm (NO wasmJs:
  Compose Multiplatform's `runComposeUiTest` has no wasm runner, and spec §9 places feature UI
  tests in `iosTest`/`androidDeviceTest` only — never wasm). Overwrite
  `shared/compose-test-support/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.slothiesmooth.nyx.shared.composetestsupport"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTestBuilder { }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm()

    sourceSets {
        all {
            languageSettings {
                optIn("androidx.compose.ui.test.ExperimentalTestApi")
            }
        }
        commonMain.dependencies {
            api(projects.shared.data)
            api(projects.shared.testSupport)
            api(libs.koin.core)
            implementation(compose.runtime)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
```

- [ ] **Step 2: Write the failing test.** Create `TestInfrastructureModuleTest.kt` (a plain Koin
  resolution test — deterministic, no Compose runner needed):

```kotlin
package com.slothiesmooth.nyx.shared.composetestsupport

import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.id.IdGenerator
import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.shared.testsupport.id.DeterministicIdGenerator
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertIs

class TestInfrastructureModuleTest {

    @Test
    fun `provides deterministic clock id generator and event bus`() {
        val koin = koinApplication { modules(testInfrastructureModule()) }.koin
        try {
            assertIs<FakeClock>(koin.get<Clock>())
            assertIs<DeterministicIdGenerator>(koin.get<IdGenerator>())
            assertIs<DefaultDomainEventBus>(koin.get<DomainEventBus>())
        } finally {
            koin.close()
        }
    }
}
```

- [ ] **Step 3: Run it (red).** Run:

```bash
./gradlew :shared:compose-test-support:jvmTest --tests "com.slothiesmooth.nyx.shared.composetestsupport.TestInfrastructureModuleTest"
```

Expected failure: `Unresolved reference 'testInfrastructureModule'`.

- [ ] **Step 4: Write `testInfrastructureModule`.** Create `TestInfrastructureModule.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.composetestsupport

import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.id.IdGenerator
import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.shared.testsupport.id.DeterministicIdGenerator
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Fixed instant used by the default test clock. */
@OptIn(ExperimentalTime::class)
private val FIXED_TEST_INSTANT: Instant = Instant.parse("2026-07-13T00:00:00Z")

/**
 * Koin module binding the platform-agnostic deterministic test doubles shared by feature UI tests:
 * a [FakeClock], a [DeterministicIdGenerator], and a fresh [DefaultDomainEventBus]. Feature tests
 * combine this with their own module (which supplies the in-memory DB / repositories).
 */
@OptIn(ExperimentalTime::class)
fun testInfrastructureModule(
    clock: Clock = FakeClock(FIXED_TEST_INSTANT),
    idGenerator: IdGenerator = DeterministicIdGenerator(),
): Module = module {
    single { clock }
    single { idGenerator }
    single<DomainEventBus> { DefaultDomainEventBus() }
}
```

- [ ] **Step 5: Write `runFeatureUiTest`.** Create `FeatureUiTest.kt`:

```kotlin
package com.slothiesmooth.nyx.shared.composetestsupport

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

/**
 * Runs a Compose UI test with a fresh Koin started from [module] (defaults to the shared
 * [testInfrastructureModule]), renders [content], then runs [assertions] against the composition.
 * Koin is stopped afterward so tests do not leak global state.
 *
 * Feature UI tests live in `iosTest` (macOS-gated) per the testing strategy; this harness is the
 * seam they call. A test needing the DB passes `module = testInfrastructureModule() + dbModule`
 * where `dbModule` builds `VaultSqlSource` over `createTestSqlDriver(NyxDb.Schema.synchronous())`.
 */
@OptIn(ExperimentalTestApi::class)
fun runFeatureUiTest(
    module: Module = testInfrastructureModule(),
    content: @Composable () -> Unit,
    assertions: ComposeUiTest.() -> Unit,
) = runComposeUiTest {
    startKoin { modules(module) }
    try {
        setContent { content() }
        assertions()
    } finally {
        stopKoin()
    }
}
```

- [ ] **Step 6: Run it (green).** Run:

```bash
./gradlew :shared:compose-test-support:jvmTest --tests "com.slothiesmooth.nyx.shared.composetestsupport.TestInfrastructureModuleTest"
```

Expected: `BUILD SUCCESSFUL`, the Koin resolution test passes and `FeatureUiTest.kt` compiles.

- [ ] **Step 7: Verify the full jvm compile (harness included).** Run:

```bash
./gradlew :shared:compose-test-support:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit.**

```bash
git add shared/compose-test-support/build.gradle.kts shared/compose-test-support/src
git commit -m "feat(compose-test-support): test infrastructure Koin module and runFeatureUiTest harness"
```

---

### Task 13: `:client` SqlDelight plugin config + `StegoImage.sq` schema

Adds the SqlDelight plugin, the `NyxDb` database config (`generateAsync = true`), and the schema +
labeled queries. This is the FIRST real content in `:client` (plan 01 created the skeleton; plan 05
later adds Compose/DI/`App()`). Merge the additions below into the existing `:client` build file.

**Files:**
- Modify: `client/build.gradle.kts` (add sqldelight plugin, `sqldelight { }` block, source-set deps)
- Create: `client/src/commonMain/sqldelight/com/slothiesmooth/nyx/client/data/sqldelight/StegoImage.sq`

**Interfaces:**
- Consumes: `:shared:data` contracts, SqlDelight runtime + drivers.
- Produces: generated database `NyxDb` (package `com.slothiesmooth.nyx.client.data.sqldelight`)
  with `NyxDb.Schema` and `stegoImageQueries` (labeled queries: `selectActive`, `selectArchived`,
  `selectById`, `upsert`, `setArchived`, `softDelete`, `purgeAll`, `countActive`).

**Steps:**

- [ ] **Step 1: Add the SqlDelight plugin + config + deps to `client/build.gradle.kts`.** Ensure
  the `plugins { }` block includes the sqldelight alias, add the `sqldelight { }` block at the top
  level, and add the source-set dependencies. The additions (merge with plan 01's existing
  `:client` config — do not remove its KMP target declarations):

```kotlin
plugins {
    // ... existing plan-01 plugins (kotlin.multiplatform, android.kmp.library, ...) ...
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("NyxDb") {
            packageName.set("com.slothiesmooth.nyx.client.data.sqldelight")
            generateAsync.set(true)
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:${libs.versions.sqldelight.get()}")
        }
    }
}

kotlin {
    // ... existing android { } / iosX64() / iosArm64() / iosSimulatorArm64() / jvm() /
    //     wasmJs { browser() } target declarations from plan 01 ...
    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
        commonMain.dependencies {
            api(projects.shared.data)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.sqldelight.async.extensions)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
        // wasmJsMain: no SqlDelight driver in v1 (web vault is in-memory).
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.shared.testSupport)
        }
    }
}
```

- [ ] **Step 2: Write the schema + queries.** Create the `.sq` file at
  `client/src/commonMain/sqldelight/com/slothiesmooth/nyx/client/data/sqldelight/StegoImage.sq`
  (the folder path under `sqldelight/` MUST match `packageName`):

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

selectActive:
SELECT *
FROM stego_image
WHERE deleted_at IS NULL AND is_archived = 0
ORDER BY created_at DESC;

selectArchived:
SELECT *
FROM stego_image
WHERE deleted_at IS NULL AND is_archived = 1
ORDER BY created_at DESC;

selectById:
SELECT *
FROM stego_image
WHERE id = ?;

upsert:
INSERT INTO stego_image(id, name, created_at, updated_at, deleted_at, is_archived)
VALUES (?, ?, ?, ?, ?, ?)
ON CONFLICT(id) DO UPDATE SET
    name = excluded.name,
    updated_at = excluded.updated_at,
    deleted_at = excluded.deleted_at,
    is_archived = excluded.is_archived;

setArchived:
UPDATE stego_image
SET is_archived = ?, updated_at = ?
WHERE id = ?;

softDelete:
UPDATE stego_image
SET deleted_at = ?
WHERE id = ?;

purgeAll:
DELETE FROM stego_image;

countActive:
SELECT count(*)
FROM stego_image
WHERE deleted_at IS NULL AND is_archived = 0;
```

- [ ] **Step 3: Generate + compile.** Run:

```bash
./gradlew :client:generateCommonMainNyxDbInterface :client:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. This validates the SQL, generates `NyxDb` (with `NyxDb.Schema`, the
`StegoImage` row data class, and `stegoImageQueries` exposing the eight labeled queries as suspend
mutations / `Query<T>` selects), and compiles the module. (If the generate task name differs in
your SqlDelight version, `:client:compileKotlinJvm` alone also triggers generation.)

- [ ] **Step 4: Commit.**

```bash
git add client/build.gradle.kts client/src/commonMain/sqldelight
git commit -m "feat(client): NyxDb SqlDelight schema and vault queries"
```

---

### Task 14: `:client` `SqlDelightSource` + `VaultSqlSource : VaultSource` + jvmTest suite (TDD)

**Files:**
- Create: `client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/data/source/database/sqldelight/SqlDelightSource.kt`
- Create: `client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/data/source/database/vault/VaultSqlSource.kt`
- Test: `client/src/jvmTest/kotlin/com/slothiesmooth/nyx/client/data/source/database/vault/VaultSqlSourceTest.kt`

**Interfaces:**
- Consumes: `NyxDb` (Task 13), `VaultSource`/`StegoImageRecord` (Task 6), `createTestSqlDriver`
  (Task 11), SqlDelight coroutines/async extensions.
- Produces:
  - `class SqlDelightSource(driver: SqlDriver, scope: CoroutineScope)` exposing `val database: SharedFlow<NyxDb>`
  - `class VaultSqlSource(source: SqlDelightSource, ioContext: CoroutineContext = Dispatchers.Default) : VaultSource`

**Steps:**

- [ ] **Step 1: Write the failing test.** Create `VaultSqlSourceTest.kt` in `jvmTest` (each test
  gets a fresh in-memory DB via `createTestSqlDriver`, so tests are isolated). `ioContext` is set
  to an `UnconfinedTestDispatcher` so query flows run on the test scheduler:

```kotlin
package com.slothiesmooth.nyx.client.data.source.database.vault

import com.slothiesmooth.nyx.client.data.source.database.sqldelight.SqlDelightSource
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.testsupport.db.createTestSqlDriver
import app.cash.sqldelight.async.coroutines.synchronous
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VaultSqlSourceTest {

    private fun newVault(scope: CoroutineScope, scheduler: TestCoroutineScheduler): VaultSqlSource {
        val driver = createTestSqlDriver(NyxDb.Schema.synchronous())
        val source = SqlDelightSource(driver, scope)
        return VaultSqlSource(source, UnconfinedTestDispatcher(scheduler))
    }

    private fun record(
        id: String,
        name: String = "Secret",
        archived: Boolean = false,
        deletedAt: String? = null,
    ) = StegoImageRecord(
        id = id,
        name = name,
        createdAt = "2026-07-13T00:00:00Z",
        updatedAt = "2026-07-13T00:00:00Z",
        deletedAt = deletedAt,
        isArchived = archived,
    )

    @Test
    fun `upsert then observeActive emits the record`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        val stored = record("id-1")

        vault.upsert(stored)

        assertEquals(listOf(stored), vault.observeActive().first())
    }

    @Test
    fun `upsert with an existing id updates in place`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        vault.upsert(record("id-1", name = "Old"))

        vault.upsert(record("id-1", name = "New"))

        val active = vault.observeActive().first()
        assertEquals(1, active.size)
        assertEquals("New", active.single().name)
    }

    @Test
    fun `getById returns the record or null`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        val stored = record("id-1")
        vault.upsert(stored)

        assertEquals(stored, vault.getById("id-1"))
        assertNull(vault.getById("missing"))
    }

    @Test
    fun `setArchived moves the record from active to archived`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        vault.upsert(record("id-1"))

        vault.setArchived("id-1", archived = true, updatedAt = "2026-07-14T00:00:00Z")

        assertTrue(vault.observeActive().first().isEmpty())
        assertEquals(listOf("id-1"), vault.observeArchived().first().map { it.id })
    }

    @Test
    fun `softDelete tombstones the record so it is excluded from active`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        vault.upsert(record("id-1"))

        vault.softDelete("id-1", deletedAt = "2026-07-14T00:00:00Z")

        assertTrue(vault.observeActive().first().isEmpty())
        assertEquals(0, vault.countActive())
    }

    @Test
    fun `countActive counts only live non-archived rows`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        vault.upsert(record("id-1"))
        vault.upsert(record("id-2"))
        assertEquals(2, vault.countActive())

        vault.setArchived("id-2", archived = true, updatedAt = "2026-07-14T00:00:00Z")
        assertEquals(1, vault.countActive())
    }

    @Test
    fun `purgeAll removes every row including tombstones`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        vault.upsert(record("id-1"))
        vault.upsert(record("id-2", archived = true))
        vault.softDelete("id-1", deletedAt = "2026-07-14T00:00:00Z")

        vault.purgeAll()

        assertEquals(0, vault.countActive())
        assertTrue(vault.observeActive().first().isEmpty())
        assertTrue(vault.observeArchived().first().isEmpty())
    }
}
```

- [ ] **Step 2: Run it (red).** Run:

```bash
./gradlew :client:jvmTest --tests "com.slothiesmooth.nyx.client.data.source.database.vault.VaultSqlSourceTest"
```

Expected failure: `Unresolved reference 'SqlDelightSource'` / `'VaultSqlSource'`.

- [ ] **Step 3: Write `SqlDelightSource`.** Create `SqlDelightSource.kt` (pawdex pattern; the outer
  app `CoroutineScope` is injected rather than `GlobalScope` — cleaner and testable):

```kotlin
package com.slothiesmooth.nyx.client.data.source.database.sqldelight

import app.cash.sqldelight.db.SqlDriver
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn

private const val REPLAY_LATEST = 1

/**
 * Lazily builds the single [NyxDb] over [driver] and shares it as a hot flow (replay 1) so every
 * source/repository observes the same database instance. The build runs `PRAGMA foreign_keys = ON`
 * once before emitting. [scope] is the outer application scope (injected via DI).
 */
class SqlDelightSource(
    private val driver: SqlDriver,
    scope: CoroutineScope,
) {
    val database: SharedFlow<NyxDb> = flow {
        driver.execute(identifier = null, sql = "PRAGMA foreign_keys = ON;", parameters = 0).await()
        emit(NyxDb(driver))
    }.shareIn(scope, SharingStarted.Lazily, replay = REPLAY_LATEST)
}
```

- [ ] **Step 4: Write `VaultSqlSource`.** Create `VaultSqlSource.kt`:

```kotlin
package com.slothiesmooth.nyx.client.data.source.database.vault

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.slothiesmooth.nyx.client.data.source.database.sqldelight.SqlDelightSource
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import com.slothiesmooth.nyx.client.data.sqldelight.StegoImage
import com.slothiesmooth.nyx.client.data.sqldelight.StegoImageQueries
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext

private const val ARCHIVED_TRUE = 1L
private const val ARCHIVED_FALSE = 0L

/**
 * [VaultSource] over SqlDelight. Reads observe the DB reactively; writes are suspend. [ioContext]
 * is the context query-mapping runs on (defaults to [Dispatchers.Default]; tests inject a test
 * dispatcher).
 */
class VaultSqlSource(
    private val source: SqlDelightSource,
    private val ioContext: CoroutineContext = Dispatchers.Default,
) : VaultSource {

    private suspend fun queries(): StegoImageQueries = database().stegoImageQueries

    private suspend fun database(): NyxDb = source.database.first()

    override fun observeActive(): Flow<List<StegoImageRecord>> =
        source.database.flatMapLatest { db ->
            db.stegoImageQueries.selectActive().asFlow().mapToList(ioContext)
        }.map { rows -> rows.map { it.toRecord() } }

    override fun observeArchived(): Flow<List<StegoImageRecord>> =
        source.database.flatMapLatest { db ->
            db.stegoImageQueries.selectArchived().asFlow().mapToList(ioContext)
        }.map { rows -> rows.map { it.toRecord() } }

    override suspend fun getById(id: String): StegoImageRecord? =
        queries().selectById(id).awaitAsOneOrNull()?.toRecord()

    override suspend fun upsert(record: StegoImageRecord) {
        queries().upsert(
            id = record.id,
            name = record.name,
            created_at = record.createdAt,
            updated_at = record.updatedAt,
            deleted_at = record.deletedAt,
            is_archived = if (record.isArchived) ARCHIVED_TRUE else ARCHIVED_FALSE,
        )
    }

    override suspend fun setArchived(id: String, archived: Boolean, updatedAt: String) {
        queries().setArchived(
            is_archived = if (archived) ARCHIVED_TRUE else ARCHIVED_FALSE,
            updated_at = updatedAt,
            id = id,
        )
    }

    override suspend fun softDelete(id: String, deletedAt: String) {
        queries().softDelete(deleted_at = deletedAt, id = id)
    }

    override suspend fun purgeAll() {
        queries().purgeAll()
    }

    override suspend fun countActive(): Int = queries().countActive().awaitAsOne().toInt()
}

private fun StegoImage.toRecord(): StegoImageRecord = StegoImageRecord(
    id = id,
    name = name,
    createdAt = created_at,
    updatedAt = updated_at,
    deletedAt = deleted_at,
    isArchived = is_archived != ARCHIVED_FALSE,
)
```

- [ ] **Step 5: Run it (green).** Run:

```bash
./gradlew :client:jvmTest --tests "com.slothiesmooth.nyx.client.data.source.database.vault.VaultSqlSourceTest"
```

Expected: `BUILD SUCCESSFUL`, all 7 tests pass. (`observeActive().first()` returns the initial
query snapshot, which includes rows upserted before collection; `asFlow().mapToList` re-queries
on write notifications; the tombstone/archive filters come from the `WHERE` clauses.)

- [ ] **Step 6: Full-module verification.** Run:

```bash
./gradlew :client:compileKotlinJvm :client:jvmTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit.**

```bash
git add client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/data/source client/src/jvmTest/kotlin/com/slothiesmooth/nyx/client/data/source
git commit -m "feat(client): SqlDelightSource and VaultSqlSource over NyxDb with jvmTest suite"
```

---

## Final phase verification

After all 14 tasks, run the whole phase's shared-infra build + host tests on Linux:

```bash
./gradlew \
  :shared:data:jvmTest \
  :shared:presentation:jvmTest \
  :shared:test-support:jvmTest \
  :shared:compose-test-support:jvmTest \
  :client:jvmTest \
  :shared:data:compileKotlinWasmJs \
  :shared:presentation:compileKotlinWasmJs \
  :shared:test-support:compileKotlinWasmJs
```

Expected: `BUILD SUCCESSFUL` — all host test suites green and the wasm compiles succeed for the
three modules that declare a wasmJs target in this phase (`:shared:compose-test-support` has no
wasm target by design; `:client`'s wasm target has no DB driver in v1). iOS/native compilation is
verified on the macOS lane later, never gated on Linux.

## Interfaces this plan produces for downstream plans

- `:shared:data`: `AppResult`/`AppError` (+ `map`/`getOrNull`), `StegoImageId`, `IdGenerator`,
  `Uuid4IdGenerator`, `Clock`/`SystemClock` (+ `Instant.toLocalDateTimeIn`), `DomainEvent`,
  `DomainEventBus`/`DefaultDomainEventBus`, and the source contracts `ImageCodec`, `VaultSource`,
  `VaultFileStore`, `SettingsSource`, `CameraSource`, `ShareSource`, `StegoImageRecord`,
  `PickedImage`, `PlatformCapabilities`.
- `:shared:presentation`: `BaseViewModel` (`async`/`ui`/`withState`/`bind`/`doInit`…),
  `ViewState`/`MutableViewState` (+ `tryCatch`/`notify`), `UiState`/`UiEvent`,
  `ViewStateHandler`, `NavController.{push,pop,set,replace,restore}Destination`.
- `:shared:test-support`: `FakeClock`, `DeterministicIdGenerator`, `createTestSqlDriver`.
- `:shared:compose-test-support`: `testInfrastructureModule`, `runFeatureUiTest`.
- `:client`: `NyxDb` (SqlDelight, package `com.slothiesmooth.nyx.client.data.sqldelight`),
  `SqlDelightSource`, `VaultSqlSource : VaultSource`.
