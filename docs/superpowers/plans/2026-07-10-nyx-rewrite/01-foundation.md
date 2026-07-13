# Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the entire new Nyx module tree as empty/skeleton modules that compile on Linux, with Gradle 9.4.1, a full version catalog, `build-logic` convention plugins, detekt + spotless quality gates, and a GitHub Actions CI workflow — old modules dropped from the build (files relocated, not deleted).
**Architecture:** A Kotlin Multiplatform monorepo driven by convention plugins in an included `build-logic` build. Every KMP module applies `nyx.kmp.library` (six targets: android/iosX64/iosArm64/iosSimulatorArm64/jvm/wasmJs); Compose modules add `nyx.compose`; feature modules use `nyx.feature.api` / `nyx.feature.basic`. iOS/native targets are declared but disabled on Linux via `kotlin.native.ignoreDisabledTargets=true`, so all Linux verification runs on jvm/wasmJs/android compilation only.
**Tech Stack:** Gradle 9.4.1, AGP 9.2.0 (`com.android.kotlin.multiplatform.library`), Kotlin 2.3.21, Compose Multiplatform 1.10.3, SqlDelight 2.3.2, detekt 1.23.8, spotless 7.0.4, foojay-resolver 1.0.0, JVM toolchain 21.

## Global Constraints

Copied verbatim from `00-INDEX.md` "Global constraints" — these apply to every task:

- Kotlin 2.3.21, AGP 9.2.0, Gradle 9.4.1, Compose Multiplatform 1.10.3, JVM target 21, compileSdk 36, targetSdk 36, minSdk 24.
- KMP targets on every KMP module: `androidTarget` (via `com.android.kotlin.multiplatform.library`, configured as `kotlin { android {} }`), `iosX64`, `iosArm64`, `iosSimulatorArm64`, `jvm`, `wasmJs`. `applyDefaultHierarchyTemplate()`. iOS compiles only on macOS — never gate Linux progress on iOS; `kotlin.native.ignoreDisabledTargets=true`.
- Namespace/package: `com.slothiesmooth.nyx.<area>` (full reverse-domain everywhere; Android `namespace` per module must be unique).
- US English in all identifiers/comments/docs/commits. No `@Suppress`-style gate-passers — the single documented exception: `NxColors.kt` may suppress MagicNumber (the one raw-ARGB file).
- kotlinx ImmutableCollections for ALL collections in state/domain surfaces (ImmutableList/Set/Map).
- Injected `Clock` only — direct system clock access banned outside `SystemClock`.
- No feature-to-feature api dependencies. `DomainEventBus` = only cross-feature channel.
- Repository writes return `AppResult`, reads return `Flow`. Use cases = single-purpose classes with `operator fun invoke`, `factoryOf`-registered.
- Composables are dumb: no filtering/sorting/mapping/pluralization in UI — VM state exposes render-ready values.
- Tests: kotlin.test + kotlinx-coroutines-test + hand-written fakes only. No mockk/kotest/turbine.
- `suspend` end-to-end for crypto (WebCrypto provider is suspend-only; `*Blocking` throws on wasm).
- No `println`; logging via Kermit.
- Commit after every green test cycle (conventional commits).

Phase-specific constraints:

- Kotlin code (`**/*.kt`) must satisfy detekt with `maxIssues: 0`; magic numbers are only allowed in `NxColors.kt` (introduced in Plan 04). Build scripts (`**/*.gradle.kts`) are NOT scanned by detekt (they are formatting-checked by spotless only), so the compileSdk/minSdk/version integers that live in build files are fine.
- The old module tree (`app/`, `feature_base/`, `steganography/`, `utils/`, `buildSrc/`) is NOT deleted in this phase — it is relocated under `legacy/` so its files stay on disk (deletion happens in Plan 08) while freeing the `steganography/` path for the new `:steganography` module and stopping `buildSrc` from being auto-included as a build.
- Every skeleton module ships exactly one placeholder Kotlin source (`Placeholder.kt` → `internal object Placeholder`) so compilation is real, not vacuous.
- iOS/native targets are declared in the convention plugin but MUST NOT be a Linux verification gate. All "expect PASS" checks in this plan use `compileKotlinJvm`, `compileKotlinWasmJs`, `assembleDebug`, `detektCheck`, `spotlessCheck`, or `./gradlew help`/`projects`.

### Why `build-logic` and the `legacy/` relocation (read before Task 1)

The task brief stated buildSrc "stops being an included build automatically when replaced by build-logic." That is factually wrong and would break the build: **Gradle auto-includes any directory literally named `buildSrc` at the root, independent of whether a `build-logic` included build exists.** As long as `buildSrc/` exists on disk it is compiled — and the current `buildSrc/build.gradle.kts` references version-catalog plugin aliases (`libs.plugins.kotlin.android`, `libs.plugins.safeArgs`, …) that this plan deletes from the catalog, so leaving `buildSrc/` in place makes every Gradle invocation fail. The only ways to stop it are to delete or rename the directory. This plan therefore **moves `buildSrc/` into `legacy/`** (content preserved, no longer auto-included).

Separately, the new `:steganography` module maps by default to the directory `steganography/`, which the OLD `:steganography` module already occupies. The old directory must be vacated. Relocating the whole old tree into `legacy/` solves both problems in one move and makes the detekt/spotless excludes a single `legacy/**` glob.

## Task 1: Upgrade the Gradle wrapper 8.3 → 9.4.1

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat` (regenerated by the wrapper task)

**Interfaces:** Produces the Gradle 9.4.1 runtime every later task and every later plan depends on. No Kotlin interfaces.

**Facts baked in:** The verified SHA-256 of `gradle-9.4.1-all.zip` is `708d2c6ecc97ca9a11838ef64a6c2301151b8dd10387e22dc1a12c30557cab5b` (fetched from `https://services.gradle.org/distributions/gradle-9.4.1-all.zip.sha256` on 2026-07-13). Pinning this in `distributionSha256Sum` makes the wrapper verify the download and fail loudly on any tampered/mismatched distribution. Do NOT invent or substitute this value; if the distribution ever changes, re-fetch from the official `.sha256` URL.

- [ ] **Step 1: Run the wrapper upgrade with a pinned checksum (first pass).**
  Run:
  ```bash
  ./gradlew wrapper --gradle-version 9.4.1 --distribution-type all \
    --gradle-distribution-sha256-sum 708d2c6ecc97ca9a11838ef64a6c2301151b8dd10387e22dc1a12c30557cab5b
  ```
  This runs under the current Gradle 8.3 and rewrites `gradle/wrapper/gradle-wrapper.properties` to point `distributionUrl` at `gradle-9.4.1-all.zip` and adds the `distributionSha256Sum` line. Expected: `BUILD SUCCESSFUL`. (Ignore any deprecation warnings from the old 8.3 build model — we do not configure the old project here.)

- [ ] **Step 2: Run the wrapper task a second time under 9.4.1.**
  Run the exact same command again:
  ```bash
  ./gradlew wrapper --gradle-version 9.4.1 --distribution-type all \
    --gradle-distribution-sha256-sum 708d2c6ecc97ca9a11838ef64a6c2301151b8dd10387e22dc1a12c30557cab5b
  ```
  The second invocation downloads 9.4.1 (verifying the checksum) and regenerates `gradle-wrapper.jar`, `gradlew`, and `gradlew.bat` with the 9.4.1 tooling. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Verify the runtime is 9.4.1 (do NOT configure the old project).**
  Run:
  ```bash
  ./gradlew --version
  ```
  Expected output contains `Gradle 9.4.1`. Do NOT run `./gradlew help`/`build`/`tasks` yet — the old `settings.gradle.kts`/`buildSrc`/modules are incompatible with Gradle 9.4.1 and will be replaced in Tasks 2–9. `--version` does not configure the project, so it succeeds.

- [ ] **Step 4: Confirm the checksum was pinned.**
  Run:
  ```bash
  grep -n "distributionSha256Sum" gradle/wrapper/gradle-wrapper.properties
  ```
  Expected: a line `distributionSha256Sum=708d2c6ecc97ca9a11838ef64a6c2301151b8dd10387e22dc1a12c30557cab5b`.

- [ ] **Step 5: Commit.**
  ```bash
  git add gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.jar gradlew gradlew.bat
  git commit -m "build: upgrade Gradle wrapper to 9.4.1 with pinned distribution checksum"
  ```

## Task 2: New version catalog (`gradle/libs.versions.toml`)

**Files:**
- Modify (full replacement): `gradle/libs.versions.toml`

**Interfaces:**
- **Consumes:** nothing.
- **Produces (consumed by every later task and plan):** the version pins from `00-INDEX.md` and the catalog aliases below. Plans 02–08 reference these exact aliases:
  - Plugins: `libs.plugins.android.application`, `android.library`, `android.kotlin.multiplatform.library`, `kotlin.multiplatform`, `kotlin.android`, `kotlin.jvm`, `kotlin.plugin.compose`, `kotlin.plugin.serialization`, `compose.multiplatform`, `sqldelight`, `paparazzi`, `detekt`, `spotless`.
  - Libraries (selection): `libs.koin.core`, `libs.koin.compose`, `libs.koin.compose.viewmodel`, `libs.koin.test`, `libs.sqldelight.android.driver`, `libs.sqldelight.native.driver`, `libs.sqldelight.sqlite.driver`, `libs.sqldelight.coroutines.extensions`, `libs.sqldelight.primitive.adapters`, `libs.sqldelight.dialect.sqlite338`, `libs.filekit.core`, `libs.filekit.dialogs`, `libs.filekit.dialogs.compose`, `libs.filekit.coil`, `libs.cryptography.core`, `libs.cryptography.provider.optimal`, `libs.kermit`, `libs.kotlinx.coroutines.core`, `libs.kotlinx.coroutines.test`, `libs.kotlinx.coroutines.swing`, `libs.kotlinx.serialization.json`, `libs.kotlinx.serialization.core`, `libs.kotlinx.datetime`, `libs.kotlinx.collections.immutable`, `libs.kotlinx.browser`, `libs.jetbrains.navigation.compose`, `libs.jetbrains.lifecycle.viewmodel`, `libs.jetbrains.lifecycle.viewmodel.compose`, `libs.jetbrains.lifecycle.runtime.compose`, `libs.coil.compose`, `libs.coil.compose.core`, `libs.androidx.datastore.preferences.core`, `libs.androidx.activity.compose`, `libs.androidx.core.ktx`, `libs.androidx.core.splashscreen`, `libs.detekt.formatting`.

**Note:** Gradle-level verification of this file happens at Task 9 (`./gradlew help` reads the catalog). This task's verification is content correctness.

- [ ] **Step 1: Replace the catalog with the full Nyx catalog.**
  Overwrite `gradle/libs.versions.toml` with exactly:
  ```toml
  [versions]
  agp = "9.2.0"
  kotlin = "2.3.21"
  compose-multiplatform = "1.10.3"
  koin = "4.2.1"
  sqldelight = "2.3.2"
  kotlinx-coroutines = "1.10.2"
  kotlinx-serialization = "1.11.0"
  kotlinx-datetime = "0.7.1"
  kotlinx-collections-immutable = "0.4.0"
  jetbrains-navigation = "2.9.2"
  jetbrains-lifecycle = "2.10.0"
  coil = "3.4.0"
  filekit = "0.13.0"
  cryptography = "0.6.0"
  kermit = "2.1.0"
  paparazzi = "2.0.0-alpha05"
  androidx-datastore = "1.2.1"
  androidx-activity = "1.13.0"
  androidx-core = "1.16.0"
  androidx-splashscreen = "1.2.0"
  kotlinx-browser = "0.5.0"
  detekt = "1.23.8"
  spotless = "7.0.4"

  [libraries]
  # Koin
  koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
  koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
  koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
  koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }

  # SqlDelight
  sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
  sqldelight-coroutines-extensions = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
  sqldelight-primitive-adapters = { module = "app.cash.sqldelight:primitive-adapters", version.ref = "sqldelight" }
  sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
  sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
  sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
  sqldelight-dialect-sqlite338 = { module = "app.cash.sqldelight:sqlite-3-38-dialect", version.ref = "sqldelight" }

  # FileKit
  filekit-core = { module = "io.github.vinceglb:filekit-core", version.ref = "filekit" }
  filekit-dialogs = { module = "io.github.vinceglb:filekit-dialogs", version.ref = "filekit" }
  filekit-dialogs-compose = { module = "io.github.vinceglb:filekit-dialogs-compose", version.ref = "filekit" }
  filekit-coil = { module = "io.github.vinceglb:filekit-coil", version.ref = "filekit" }

  # cryptography-kotlin
  cryptography-core = { module = "dev.whyoleg.cryptography:cryptography-core", version.ref = "cryptography" }
  cryptography-provider-optimal = { module = "dev.whyoleg.cryptography:cryptography-provider-optimal", version.ref = "cryptography" }

  # Logging
  kermit = { module = "co.touchlab:kermit", version.ref = "kermit" }

  # kotlinx
  kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
  kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
  kotlinx-coroutines-swing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinx-coroutines" }
  kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
  kotlinx-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core", version.ref = "kotlinx-serialization" }
  kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
  kotlinx-collections-immutable = { module = "org.jetbrains.kotlinx:kotlinx-collections-immutable", version.ref = "kotlinx-collections-immutable" }
  kotlinx-browser = { module = "org.jetbrains.kotlinx:kotlinx-browser", version.ref = "kotlinx-browser" }

  # JetBrains multiplatform AndroidX equivalents
  jetbrains-navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "jetbrains-navigation" }
  jetbrains-lifecycle-viewmodel = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel", version.ref = "jetbrains-lifecycle" }
  jetbrains-lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "jetbrains-lifecycle" }
  jetbrains-lifecycle-runtime-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "jetbrains-lifecycle" }

  # Coil 3
  coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
  coil-compose-core = { module = "io.coil-kt.coil3:coil-compose-core", version.ref = "coil" }

  # AndroidX (android/ios/jvm only where noted)
  androidx-datastore-preferences-core = { module = "androidx.datastore:datastore-preferences-core", version.ref = "androidx-datastore" }
  androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activity" }
  androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "androidx-core" }
  androidx-core-splashscreen = { module = "androidx.core:core-splashscreen", version.ref = "androidx-splashscreen" }

  # Static-analysis add-ons
  detekt-formatting = { module = "io.gitlab.arturbosch.detekt:detekt-formatting", version.ref = "detekt" }

  [bundles]
  koin = ["koin-core", "koin-compose", "koin-compose-viewmodel"]

  [plugins]
  android-application = { id = "com.android.application", version.ref = "agp" }
  android-library = { id = "com.android.library", version.ref = "agp" }
  android-kotlin-multiplatform-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
  kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
  kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
  kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
  kotlin-plugin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
  kotlin-plugin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
  compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
  sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
  paparazzi = { id = "app.cash.paparazzi", version.ref = "paparazzi" }
  detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
  spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
  ```

- [ ] **Step 2: Sanity-check the catalog content.**
  Run:
  ```bash
  grep -c "version.ref" gradle/libs.versions.toml && grep -n "cryptography-provider-optimal\|paparazzi\|kotlin-plugin-serialization" gradle/libs.versions.toml
  ```
  Expected: a non-zero count and the three grepped lines present.

- [ ] **Step 3: Commit.**
  ```bash
  git add gradle/libs.versions.toml
  git commit -m "build: replace version catalog with Nyx KMP pins and aliases"
  ```

## Task 3: New `gradle.properties`

**Files:**
- Modify (full replacement): `gradle.properties`

**Interfaces:** Produces the JVM/daemon/AndroidX/KMP flags the whole build relies on. No Kotlin interfaces.

**Gotcha baked in:** The old `gradle.properties` defined `accessKey`/`privateKey` gradle properties consumed by `app/build.gradle.kts`'s `buildConfigFieldFromGradleProperty(...)` via `checkNotNull(...)`. When the old `app` module leaves the build (Task 9), nothing reads those properties — remove them. Also remove the dead `android.enableJetifier` (Jetifier is obsolete under AndroidX-only) and the `kapt.*` flags (no annotation processing in the new stack; SqlDelight/Compose use compiler plugins, not kapt).

- [ ] **Step 1: Overwrite `gradle.properties`.**
  Write exactly:
  ```properties
  # JVM / Gradle daemon
  org.gradle.jvmargs=-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC
  org.gradle.daemon=true
  org.gradle.parallel=true
  org.gradle.caching=true

  # Kotlin
  kotlin.code.style=official
  kotlin.daemon.jvmargs=-Xmx4g

  # Kotlin Multiplatform / native
  kotlin.native.ignoreDisabledTargets=true
  kotlin.mpp.enableCInteropCommonization=true

  # Android
  android.useAndroidX=true
  android.nonTransitiveRClass=true
  ```

- [ ] **Step 2: Confirm the dead properties are gone.**
  Run:
  ```bash
  ! grep -qE "accessKey|privateKey|enableJetifier|kapt\." gradle.properties && echo "CLEAN"
  ```
  Expected: `CLEAN`.

- [ ] **Step 3: Commit.**
  ```bash
  git add gradle.properties
  git commit -m "build: rewrite gradle.properties for KMP (Xmx8g, cinterop commonization, drop dead keys)"
  ```

## Task 4: `build-logic` included build scaffold

**Files:**
- Create: `build-logic/settings.gradle.kts`
- Create: `build-logic/build.gradle.kts`
- Create: `build-logic/src/main/kotlin/.gitkeep` (empty; ensures the source dir exists)

**Interfaces:**
- **Consumes:** the plugin coordinates below (verified in `00-INDEX.md` build-logic facts).
- **Produces:** the `build-logic` included build that hosts every `nyx.*` convention plugin (Tasks 5–8). Convention plugins consume it as their classpath.

**Facts baked in (from `00-INDEX.md`):** `build-logic/build.gradle.kts` deps are `implementation` (NOT `compileOnly`): `com.android.tools.build:gradle:9.2.0` (contains the `com.android.application`/`library`/`kotlin.multiplatform.library` plugin ids), `org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21`, `org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.3.21`, `org.jetbrains.compose:compose-gradle-plugin:1.10.3`. **Additions beyond the index's four (all required so the convention plugins can compile and apply their plugin ids):** `org.jetbrains.kotlin:kotlin-serialization:2.3.21` (for `org.jetbrains.kotlin.plugin.serialization` in the `nyx.feature.*` conventions), `io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8` (for `id("io.gitlab.arturbosch.detekt")` + the `Detekt` task type import in `nyx.detekt`), and `com.diffplug.spotless:spotless-plugin-gradle:7.0.4` (for `id("com.diffplug.spotless")` + the `spotless {}` accessor in `nyx.spotless`). A precompiled script plugin that declares a plugin id in its `plugins {}` block needs that plugin's implementation on the `build-logic` compile classpath, or `build-logic` fails to compile. `build-logic/settings.gradle.kts` needs `google() + mavenCentral() + gradlePluginPortal()`.

- [ ] **Step 1: Write `build-logic/settings.gradle.kts`.**
  ```kotlin
  rootProject.name = "build-logic"

  pluginManagement {
      repositories {
          google()
          mavenCentral()
          gradlePluginPortal()
      }
  }

  dependencyResolutionManagement {
      repositories {
          google()
          mavenCentral()
          gradlePluginPortal()
      }
  }
  ```

- [ ] **Step 2: Write `build-logic/build.gradle.kts`.**
  ```kotlin
  plugins {
      `kotlin-dsl`
  }

  dependencies {
      implementation("com.android.tools.build:gradle:9.2.0")
      implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
      implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.3.21")
      implementation("org.jetbrains.kotlin:kotlin-serialization:2.3.21")
      implementation("org.jetbrains.compose:compose-gradle-plugin:1.10.3")
      implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
      implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.4")
  }
  ```

- [ ] **Step 3: Create the (empty) plugin source directory.**
  ```bash
  mkdir -p build-logic/src/main/kotlin && touch build-logic/src/main/kotlin/.gitkeep
  ```

- [ ] **Step 4: Verify `build-logic` builds in isolation.**
  Run:
  ```bash
  ./gradlew -p build-logic build
  ```
  Expected: `BUILD SUCCESSFUL`. This treats `build-logic` as a standalone build (using `build-logic/settings.gradle.kts`), downloads the plugin dependencies, and confirms the coordinates resolve. No convention plugins exist yet, so nothing is compiled beyond an empty `kotlin-dsl` project.

- [ ] **Step 5: Commit.**
  ```bash
  git add build-logic
  git commit -m "build: scaffold build-logic included build with plugin classpath"
  ```

## Task 5: Convention plugin `nyx.kmp.library`

**Files:**
- Create: `build-logic/src/main/kotlin/nyx.kmp.library.gradle.kts`

**Interfaces:**
- **Consumes:** `build-logic` classpath (Task 4); the root version catalog at apply-time via `VersionCatalogsExtension` (for `kotlinx-coroutines-test`).
- **Produces:** the `nyx.kmp.library` plugin id. Applied by every KMP module in Tasks 10–12 and by Plans 02–07. It declares the six KMP targets, the Android target inside `kotlin { android {} }` with a path-derived namespace, `applyDefaultHierarchyTemplate()`, JVM toolchain 21, and `commonTest` deps (`kotlin("test")` + `kotlinx-coroutines-test`).

**Namespace helper (baked-in rule):** `defaultAndroidNamespace(path)` strips the leading `:`, splits on `:`, drops any `client` segment, removes `-` from each remaining segment, joins with `.`, and prefixes `com.slothiesmooth.nyx.`. This yields the exact namespaces in the `00-INDEX.md` table for every module EXCEPT `:shared:design-library` (whose table namespace drops `shared`); that one module overrides its namespace explicitly in Task 10. Examples: `:crypto` → `com.slothiesmooth.nyx.crypto`; `:shared:data` → `com.slothiesmooth.nyx.shared.data`; `:shared:compose-test-support` → `com.slothiesmooth.nyx.shared.composetestsupport`; `:feature:vault:client:basic` → `com.slothiesmooth.nyx.feature.vault.basic`; `:feature:common:client:api` → `com.slothiesmooth.nyx.feature.common.api`.

- [ ] **Step 1: Write `nyx.kmp.library.gradle.kts`.**
  ```kotlin
  import org.gradle.api.artifacts.VersionCatalogsExtension

  plugins {
      id("org.jetbrains.kotlin.multiplatform")
      id("com.android.kotlin.multiplatform.library")
  }

  val nyxAndroidCompileSdk = 36
  val nyxAndroidMinSdk = 24
  val nyxJvmToolchain = 21

  fun defaultAndroidNamespace(projectPath: String): String {
      val segments = projectPath
          .removePrefix(":")
          .split(":")
          .filter { it != "client" }
          .map { it.replace("-", "") }
      return "com.slothiesmooth.nyx." + segments.joinToString(".")
  }

  val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

  kotlin {
      android {
          namespace = defaultAndroidNamespace(project.path)
          compileSdk = nyxAndroidCompileSdk
          minSdk = nyxAndroidMinSdk
          withHostTestBuilder {}.configure {}
      }

      jvm()

      iosX64()
      iosArm64()
      iosSimulatorArm64()

      wasmJs {
          browser()
      }

      applyDefaultHierarchyTemplate()

      jvmToolchain(nyxJvmToolchain)

      sourceSets {
          commonTest.dependencies {
              implementation(kotlin("test"))
              implementation(libs.findLibrary("kotlinx-coroutines-test").get())
          }
      }
  }
  ```

  Notes for the implementer:
  - The `kotlin { android {} }` block IS the Android target declaration for the `com.android.kotlin.multiplatform.library` plugin — do NOT also call `androidTarget()`. This block name (`android`, not `androidLibrary`) and the `withHostTestBuilder {}.configure {}` call are the AGP-9-KMP-plugin DSL verified for this stack in `00-INDEX.md`. If AGP rejects `withHostTestBuilder`, it is the single localized API touch-point in this convention; check `00-INDEX.md` "build-logic" facts.
  - `kotlin("test")` resolves the kotlin-test dependency for Kotlin 2.3.21 automatically; no catalog entry needed.

- [ ] **Step 2: Verify the convention plugin compiles.**
  Run:
  ```bash
  ./gradlew -p build-logic build
  ```
  Expected: `BUILD SUCCESSFUL` (the precompiled script plugin `nyx.kmp.library` compiles; its `plugins {}` block ids resolve from the Task 4 classpath).

- [ ] **Step 3: Commit.**
  ```bash
  git add build-logic/src/main/kotlin/nyx.kmp.library.gradle.kts
  git commit -m "build: add nyx.kmp.library convention plugin (six KMP targets)"
  ```

## Task 6: Convention plugin `nyx.compose`

**Files:**
- Create: `build-logic/src/main/kotlin/nyx.compose.gradle.kts`

**Interfaces:**
- **Consumes:** `build-logic` classpath (Task 4). Assumes a KMP plugin is already applied by the consuming module (it is only ever applied alongside `nyx.kmp.library`), so it configures dependencies via the string source-set configuration names created by that plugin.
- **Produces:** the `nyx.compose` plugin id. Applies `org.jetbrains.kotlin.plugin.compose` (compiler) + `org.jetbrains.compose` (Gradle plugin), adds the Compose baseline to `commonMain`, and configures `compose.resources { publicResClass = true; generateResClass = always }`.

**Facts baked in (from `00-INDEX.md`):** `compose.resources {}` is a PROJECT-level extension from the `org.jetbrains.compose` plugin (NOT inside `kotlin {}`); `org.jetbrains.kotlin.plugin.compose` is only the compiler plugin. This convention uses the string-config form `"commonMainImplementation"(...)` for dependencies (instead of the `kotlin {}` accessor) because the `kotlin` type-safe accessor is not generated inside this precompiled plugin — it does not declare a Kotlin plugin in its own `plugins {}` block. The `compose` accessor IS generated here because this plugin declares `org.jetbrains.compose`.

- [ ] **Step 1: Write `nyx.compose.gradle.kts`.**
  ```kotlin
  plugins {
      id("org.jetbrains.kotlin.plugin.compose")
      id("org.jetbrains.compose")
  }

  dependencies {
      "commonMainImplementation"(compose.runtime)
      "commonMainImplementation"(compose.foundation)
      "commonMainImplementation"(compose.material3)
      "commonMainImplementation"(compose.ui)
      "commonMainImplementation"(compose.components.resources)
      "commonMainImplementation"(compose.components.uiToolingPreview)
  }

  compose.resources {
      publicResClass = true
      generateResClass = always
  }
  ```

  Note: `generateResClass = always` uses the `always` receiver property on the resources extension (the documented Compose Multiplatform DSL); do not replace it with a string.

- [ ] **Step 2: Verify the convention plugin compiles.**
  Run:
  ```bash
  ./gradlew -p build-logic build
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit.**
  ```bash
  git add build-logic/src/main/kotlin/nyx.compose.gradle.kts
  git commit -m "build: add nyx.compose convention plugin (CMP + resources config)"
  ```

## Task 7: Convention plugins `nyx.feature.api` and `nyx.feature.basic`

**Files:**
- Create: `build-logic/src/main/kotlin/nyx.feature.api.gradle.kts`
- Create: `build-logic/src/main/kotlin/nyx.feature.basic.gradle.kts`

**Interfaces:**
- **Consumes:** the `nyx.kmp.library` (Task 5) and `nyx.compose` (Task 6) plugin ids; the root catalog at apply-time.
- **Produces:** `nyx.feature.api` and `nyx.feature.basic` plugin ids. Every feature module in Task 11 and Plans 05–06 applies one of them. Both stack `nyx.kmp.library` + `nyx.compose` + serialization and add the standard dependency set (Koin, JetBrains navigation, immutable collections). `api` exposes routing deps with `api` visibility so downstream `basic`/`client` modules see the route types; `basic` adds coroutines + Kermit as `implementation`.

- [ ] **Step 1: Write `nyx.feature.api.gradle.kts`.**
  ```kotlin
  import org.gradle.api.artifacts.VersionCatalogsExtension

  plugins {
      id("nyx.kmp.library")
      id("nyx.compose")
      id("org.jetbrains.kotlin.plugin.serialization")
  }

  val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

  dependencies {
      "commonMainApi"(libs.findLibrary("koin-core").get())
      "commonMainApi"(libs.findLibrary("jetbrains-navigation-compose").get())
      "commonMainApi"(libs.findLibrary("kotlinx-collections-immutable").get())
      "commonMainImplementation"(libs.findLibrary("kotlinx-serialization-core").get())
  }
  ```

- [ ] **Step 2: Write `nyx.feature.basic.gradle.kts`.**
  ```kotlin
  import org.gradle.api.artifacts.VersionCatalogsExtension

  plugins {
      id("nyx.kmp.library")
      id("nyx.compose")
      id("org.jetbrains.kotlin.plugin.serialization")
  }

  val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

  dependencies {
      "commonMainImplementation"(libs.findLibrary("koin-core").get())
      "commonMainImplementation"(libs.findLibrary("jetbrains-navigation-compose").get())
      "commonMainImplementation"(libs.findLibrary("kotlinx-collections-immutable").get())
      "commonMainImplementation"(libs.findLibrary("kotlinx-coroutines-core").get())
      "commonMainImplementation"(libs.findLibrary("kermit").get())
  }
  ```

- [ ] **Step 3: Verify both convention plugins compile.**
  Run:
  ```bash
  ./gradlew -p build-logic build
  ```
  Expected: `BUILD SUCCESSFUL` (both plugins compile; their `plugins {}` blocks apply the sibling precompiled plugins `nyx.kmp.library`/`nyx.compose`).

- [ ] **Step 4: Commit.**
  ```bash
  git add build-logic/src/main/kotlin/nyx.feature.api.gradle.kts build-logic/src/main/kotlin/nyx.feature.basic.gradle.kts
  git commit -m "build: add nyx.feature.api and nyx.feature.basic convention plugins"
  ```

## Task 8: Convention plugins `nyx.detekt` + `nyx.spotless` and the new `detekt.yml`

**Files:**
- Create: `build-logic/src/main/kotlin/nyx.detekt.gradle.kts`
- Create: `build-logic/src/main/kotlin/nyx.spotless.gradle.kts`
- Modify (full replacement of the old 701-line file): `detekt.yml`

**Interfaces:**
- **Consumes:** the detekt and spotless Gradle plugin artifacts on the `build-logic` classpath (added in Task 4: `detekt-gradle-plugin:1.23.8`, `spotless-plugin-gradle:7.0.4`). These two convention plugins apply the ids `io.gitlab.arturbosch.detekt` / `com.diffplug.spotless`; because the implementations are on the `build-logic` classpath, the ids resolve at both `build-logic` compile time and at root apply time (Task 9). The root build does NOT need to declare detekt/spotless `apply false` — applying `nyx.detekt`/`nyx.spotless` brings them.
- **Produces:** `nyx.detekt` (registers the root aggregate `detektCheck` task, `maxIssues: 0`) and `nyx.spotless` (registers `spotlessCheck`, ktlint on `**/*.kt` and `**/*.gradle.kts`). Both consumed by the root build (Task 9) and by CI (Task 14).

**Design decision baked in:** detekt and spotless must NOT scan the relocated old code, the `build-logic` build, or generated/build output. Both exclude `legacy/**`, `build-logic/**`, `**/build/**`, `**/generated/**`. detekt scans `**/*.kt` only (never `**/*.gradle.kts`) so the compileSdk/minSdk integers in build scripts do not trip `MagicNumber`; spotless formats both `.kt` and `.gradle.kts`. Line length is 180 to match the existing `.editorconfig` (`max_line_length = 180`).

- [ ] **Step 1: Write `nyx.detekt.gradle.kts`.**
  ```kotlin
  import io.gitlab.arturbosch.detekt.Detekt

  plugins {
      id("io.gitlab.arturbosch.detekt")
  }

  dependencies {
      "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
  }

  val detektCheck by tasks.registering(Detekt::class) {
      description = "Runs detekt across all module Kotlin sources."
      group = "verification"
      parallel = true
      ignoreFailures = false
      buildUponDefaultConfig = true
      config.setFrom(files("$rootDir/detekt.yml"))
      setSource(files(rootDir))
      include("**/*.kt")
      exclude(
          "**/build/**",
          "**/generated/**",
          "**/resources/**",
          "legacy/**",
          "build-logic/**",
      )
      reports {
          html.required.set(true)
          xml.required.set(false)
          sarif.required.set(false)
          txt.required.set(false)
          md.required.set(false)
      }
  }
  ```

  Note: `detekt-formatting` is version-pinned to `1.23.8` here (matches the catalog `detekt` version). The `"detektPlugins"(...)` string-config form avoids needing the catalog inside `build-logic`.

- [ ] **Step 2: Write `nyx.spotless.gradle.kts`.**
  ```kotlin
  plugins {
      id("com.diffplug.spotless")
  }

  spotless {
      kotlin {
          target("**/*.kt")
          targetExclude("**/build/**", "**/generated/**", "legacy/**", "build-logic/**")
          ktlint()
          trimTrailingWhitespace()
          endWithNewline()
      }
      kotlinGradle {
          target("**/*.gradle.kts")
          targetExclude("**/build/**", "legacy/**", "build-logic/**")
          ktlint()
      }
  }
  ```

  Note: `isEnforceCheck` defaults to `true`, so `spotlessCheck` is wired into `check` — this satisfies the "wired to check" requirement without extra code.

- [ ] **Step 3: Replace `detekt.yml` with a fresh minimal config.**
  Overwrite `detekt.yml` (deleting the old 701-line file's content) with:
  ```yaml
  build:
    maxIssues: 0
    excludeCorrectable: false

  config:
    validation: true
    checkExhaustiveness: false

  formatting:
    active: true
    android: true
    autoCorrect: true
    MaximumLineLength:
      active: true
      maxLineLength: 180

  style:
    active: true
    MaxLineLength:
      active: true
      maxLineLength: 180
    MagicNumber:
      active: true
      ignoreNumbers: ['-1', '0', '1', '2']
      ignoreEnums: true
      ignoreAnnotation: true
  ```

  With `buildUponDefaultConfig = true` (set in the task), the full detekt default ruleset plus the formatting ruleset are active; this file only tightens `maxIssues` to 0 and aligns line length to 180.

- [ ] **Step 4: Verify both convention plugins compile.**
  Run:
  ```bash
  ./gradlew -p build-logic build
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit.**
  ```bash
  git add build-logic/src/main/kotlin/nyx.detekt.gradle.kts build-logic/src/main/kotlin/nyx.spotless.gradle.kts detekt.yml
  git commit -m "build: add nyx.detekt and nyx.spotless conventions with minimal detekt.yml"
  ```

## Task 9: New `settings.gradle.kts`, root `build.gradle.kts`, and neutralize the old tree

**Files:**
- Modify (full replacement): `settings.gradle.kts`
- Modify (full replacement): `build.gradle.kts`
- Move (git mv): `app/`, `feature_base/`, `steganography/`, `utils/`, `buildSrc/` → `legacy/`

**Interfaces:**
- **Consumes:** `build-logic` (Tasks 4–8), the catalog (Task 2), `detekt.yml` (Task 8).
- **Produces:** the root build. `settings.gradle.kts` wires `includeBuild("build-logic")`, foojay 1.0.0, repos, `TYPESAFE_PROJECT_ACCESSORS`, and `rootProject.name = "nyx"` (no module includes yet — those are added incrementally in Tasks 10–13). The root `build.gradle.kts` declares all plugins `apply false`, applies `nyx.detekt` + `nyx.spotless`, and adds the native-test `-lsqlite3` linker block.

**This is the first task that configures the new project with Gradle 9.4.1.**

- [ ] **Step 1: Relocate the old module tree into `legacy/`.**
  Run:
  ```bash
  mkdir -p legacy
  git mv app legacy/app
  git mv feature_base legacy/feature_base
  git mv steganography legacy/steganography
  git mv utils legacy/utils
  git mv buildSrc legacy/buildSrc
  ```
  This preserves all old files on disk (Plan 08 deletes them), stops `buildSrc/` from being auto-included as a build, and frees the `steganography/` path for the new `:steganography` module.

- [ ] **Step 2: Write the new `settings.gradle.kts`.**
  ```kotlin
  rootProject.name = "nyx"

  pluginManagement {
      includeBuild("build-logic")
      repositories {
          google()
          mavenCentral()
          gradlePluginPortal()
      }
  }

  plugins {
      id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  }

  dependencyResolutionManagement {
      repositories {
          google()
          mavenCentral()
      }
  }

  enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

  // Module includes are added incrementally in Tasks 10-13.
  ```

- [ ] **Step 3: Write the new root `build.gradle.kts`.**
  ```kotlin
  plugins {
      alias(libs.plugins.android.application) apply false
      alias(libs.plugins.android.library) apply false
      alias(libs.plugins.android.kotlin.multiplatform.library) apply false
      alias(libs.plugins.kotlin.multiplatform) apply false
      alias(libs.plugins.kotlin.android) apply false
      alias(libs.plugins.kotlin.jvm) apply false
      alias(libs.plugins.kotlin.plugin.compose) apply false
      alias(libs.plugins.kotlin.plugin.serialization) apply false
      alias(libs.plugins.compose.multiplatform) apply false
      alias(libs.plugins.sqldelight) apply false
      alias(libs.plugins.paparazzi) apply false
      id("nyx.detekt")
      id("nyx.spotless")
  }

  // SqlDelight's native driver needs the system sqlite linked into native test executables.
  // Inert on Linux (native targets are disabled via kotlin.native.ignoreDisabledTargets).
  subprojects {
      plugins.withId("org.jetbrains.kotlin.multiplatform") {
          extensions.configure(org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java) {
              targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java).configureEach {
                  binaries.withType(org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable::class.java).configureEach {
                      linkerOpts("-lsqlite3")
                  }
              }
          }
      }
  }
  ```

- [ ] **Step 4: Verify the project configures under Gradle 9.4.1.**
  Run:
  ```bash
  ./gradlew help
  ```
  Expected: `BUILD SUCCESSFUL`. This proves: the 9.4.1 wrapper works, `build-logic` is an included build, the catalog parses, `nyx.detekt`/`nyx.spotless` apply to root, and the old tree no longer interferes.

- [ ] **Step 5: Verify no modules are included yet.**
  Run:
  ```bash
  ./gradlew projects
  ```
  Expected: `BUILD SUCCESSFUL` and the output lists only `Root project 'nyx'` with no subprojects.

- [ ] **Step 6: Commit.**
  ```bash
  git add -A
  git commit -m "build: new settings and root build; relocate old tree under legacy/"
  ```

## Task 10: Engine and shared module skeletons

**Files:**
- Modify: `settings.gradle.kts` (append includes)
- Create per module below: `<dir>/build.gradle.kts` + `<dir>/src/commonMain/kotlin/<pkgpath>/Placeholder.kt`

Modules and their convention plugin:
| Module path | Directory | Convention | Package |
|---|---|---|---|
| `:crypto` | `crypto` | `nyx.kmp.library` | `com.slothiesmooth.nyx.crypto` |
| `:steganography` | `steganography` | `nyx.kmp.library` | `com.slothiesmooth.nyx.steganography` |
| `:shared:data` | `shared/data` | `nyx.kmp.library` | `com.slothiesmooth.nyx.shared.data` |
| `:shared:test-support` | `shared/test-support` | `nyx.kmp.library` | `com.slothiesmooth.nyx.shared.testsupport` |
| `:shared:presentation` | `shared/presentation` | `nyx.kmp.library` + `nyx.compose` | `com.slothiesmooth.nyx.shared.presentation` |
| `:shared:compose-test-support` | `shared/compose-test-support` | `nyx.kmp.library` + `nyx.compose` | `com.slothiesmooth.nyx.shared.composetestsupport` |
| `:shared:design-library` | `shared/design-library` | `nyx.kmp.library` + `nyx.compose` (namespace override) | `com.slothiesmooth.nyx.designlibrary` |

**Interfaces:**
- **Consumes:** the four `nyx.*` conventions.
- **Produces:** empty compiling skeletons for the engine and shared modules. Plans 02/03/04 fill in the real code (contracts in `00-INDEX.md`).

- [ ] **Step 1: Create the pure-KMP engine and shared modules with a script.**
  Run:
  ```bash
  create_kmp() { # $1=dir  $2=pkg
    mkdir -p "$1/src/commonMain/kotlin/${2//.//}"
    printf 'plugins {\n    id("nyx.kmp.library")\n}\n' > "$1/build.gradle.kts"
    printf 'package %s\n\ninternal object Placeholder\n' "$2" > "$1/src/commonMain/kotlin/${2//.//}/Placeholder.kt"
  }
  create_kmp crypto com.slothiesmooth.nyx.crypto
  create_kmp steganography com.slothiesmooth.nyx.steganography
  create_kmp shared/data com.slothiesmooth.nyx.shared.data
  create_kmp shared/test-support com.slothiesmooth.nyx.shared.testsupport
  ```

- [ ] **Step 2: Create the compose-KMP shared modules with a script.**
  Run:
  ```bash
  create_kmp_compose() { # $1=dir  $2=pkg
    mkdir -p "$1/src/commonMain/kotlin/${2//.//}"
    printf 'plugins {\n    id("nyx.kmp.library")\n    id("nyx.compose")\n}\n' > "$1/build.gradle.kts"
    printf 'package %s\n\ninternal object Placeholder\n' "$2" > "$1/src/commonMain/kotlin/${2//.//}/Placeholder.kt"
  }
  create_kmp_compose shared/presentation com.slothiesmooth.nyx.shared.presentation
  create_kmp_compose shared/compose-test-support com.slothiesmooth.nyx.shared.composetestsupport
  ```

- [ ] **Step 3: Create `:shared:design-library` with its namespace override.**
  This module's `00-INDEX.md` namespace (`com.slothiesmooth.nyx.designlibrary`) drops the `shared` segment, so it overrides the convention default. Run:
  ```bash
  mkdir -p shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary
  cat > shared/design-library/build.gradle.kts <<'EOF'
  plugins {
      id("nyx.kmp.library")
      id("nyx.compose")
  }

  kotlin {
      android {
          namespace = "com.slothiesmooth.nyx.designlibrary"
      }
  }
  EOF
  printf 'package com.slothiesmooth.nyx.designlibrary\n\ninternal object Placeholder\n' \
    > shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/Placeholder.kt
  ```
  (The re-opened `kotlin { android { namespace = ... } }` overrides the convention-set namespace; last write wins.)

- [ ] **Step 4: Add the includes to `settings.gradle.kts`.**
  Append this block to the end of `settings.gradle.kts`:
  ```kotlin
  include(
      ":crypto",
      ":steganography",
      ":shared:data",
      ":shared:test-support",
      ":shared:presentation",
      ":shared:compose-test-support",
      ":shared:design-library",
  )
  ```

- [ ] **Step 5: Verify the engine and shared modules compile on the JVM target.**
  Run:
  ```bash
  ./gradlew :crypto:compileKotlinJvm :steganography:compileKotlinJvm :shared:data:compileKotlinJvm :shared:presentation:compileKotlinJvm :shared:design-library:compileKotlinJvm
  ```
  Expected: `BUILD SUCCESSFUL`. This exercises `nyx.kmp.library` (jvm target + toolchain) and `nyx.compose` (design-library/presentation compile with Compose on the classpath) for real.

- [ ] **Step 6: Verify the wasmJs target compiles too (Linux-safe, non-android path).**
  Run:
  ```bash
  ./gradlew :crypto:compileKotlinWasmJs :shared:design-library:compileKotlinWasmJs
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit.**
  ```bash
  git add settings.gradle.kts crypto steganography shared
  git commit -m "build: add engine and shared module skeletons"
  ```

## Task 11: Feature module skeletons (common + 7 features × api/basic)

**Files:**
- Modify: `settings.gradle.kts` (append includes)
- Create 16 modules, each `<dir>/build.gradle.kts` + `<dir>/src/commonMain/kotlin/<pkgpath>/Placeholder.kt`

Modules (`X ∈ {navigation, splash, theme, vault, encrypt, decrypt, settings}`):
| Module path | Directory | Convention | Package |
|---|---|---|---|
| `:feature:common:client:api` | `feature/common/client/api` | `nyx.feature.api` | `com.slothiesmooth.nyx.feature.common.api` |
| `:feature:common:client:koin` | `feature/common/client/koin` | `nyx.feature.basic` | `com.slothiesmooth.nyx.feature.common.koin` |
| `:feature:X:client:api` | `feature/X/client/api` | `nyx.feature.api` | `com.slothiesmooth.nyx.feature.X.api` |
| `:feature:X:client:basic` | `feature/X/client/basic` | `nyx.feature.basic` | `com.slothiesmooth.nyx.feature.X.basic` |

**Interfaces:**
- **Consumes:** `nyx.feature.api` / `nyx.feature.basic`.
- **Produces:** empty compiling skeletons for all feature modules. Plans 05/06 fill in `Feature` interfaces, routes, providers, VMs (contracts in `00-INDEX.md`).

- [ ] **Step 1: Create all 16 feature modules with a script.**
  Run:
  ```bash
  create_feature() { # $1=dir  $2=convention-id  $3=pkg
    mkdir -p "$1/src/commonMain/kotlin/${3//.//}"
    printf 'plugins {\n    id("%s")\n}\n' "$2" > "$1/build.gradle.kts"
    printf 'package %s\n\ninternal object Placeholder\n' "$3" > "$1/src/commonMain/kotlin/${3//.//}/Placeholder.kt"
  }

  create_feature feature/common/client/api  nyx.feature.api   com.slothiesmooth.nyx.feature.common.api
  create_feature feature/common/client/koin nyx.feature.basic com.slothiesmooth.nyx.feature.common.koin

  for f in navigation splash theme vault encrypt decrypt settings; do
    create_feature "feature/$f/client/api"   nyx.feature.api   "com.slothiesmooth.nyx.feature.$f.api"
    create_feature "feature/$f/client/basic" nyx.feature.basic "com.slothiesmooth.nyx.feature.$f.basic"
  done
  ```

- [ ] **Step 2: Add the includes to `settings.gradle.kts`.**
  Append this block:
  ```kotlin
  include(
      ":feature:common:client:api",
      ":feature:common:client:koin",
      ":feature:navigation:client:api",
      ":feature:navigation:client:basic",
      ":feature:splash:client:api",
      ":feature:splash:client:basic",
      ":feature:theme:client:api",
      ":feature:theme:client:basic",
      ":feature:vault:client:api",
      ":feature:vault:client:basic",
      ":feature:encrypt:client:api",
      ":feature:encrypt:client:basic",
      ":feature:decrypt:client:api",
      ":feature:decrypt:client:basic",
      ":feature:settings:client:api",
      ":feature:settings:client:basic",
  )
  ```

- [ ] **Step 3: Verify a representative spread of feature modules compiles.**
  Run:
  ```bash
  ./gradlew :feature:common:client:api:compileKotlinJvm :feature:common:client:koin:compileKotlinJvm :feature:vault:client:api:compileKotlinJvm :feature:vault:client:basic:compileKotlinJvm :feature:settings:client:basic:compileKotlinJvm
  ```
  Expected: `BUILD SUCCESSFUL`. This exercises both `nyx.feature.api` and `nyx.feature.basic` (KMP + Compose + serialization + catalog deps resolve).

- [ ] **Step 4: Verify all feature modules resolve in the project graph.**
  Run:
  ```bash
  ./gradlew projects | grep -c "feature"
  ```
  Expected: a count of at least 16 lines mentioning `feature`.

- [ ] **Step 5: Commit.**
  ```bash
  git add settings.gradle.kts feature
  git commit -m "build: add feature module skeletons (common + 7 features api/basic)"
  ```

## Task 12: `:client` module skeleton (with SqlDelight)

**Files:**
- Modify: `settings.gradle.kts` (append include)
- Create: `client/build.gradle.kts`
- Create: `client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/Placeholder.kt`

**Interfaces:**
- **Consumes:** `nyx.kmp.library`, `nyx.compose`, `libs.plugins.sqldelight`, `libs.sqldelight.dialect.sqlite338`.
- **Produces:** the `:client` skeleton with a configured (but empty) SqlDelight database `NyxDb` (package `com.slothiesmooth.nyx.client.data.sqldelight`, `generateAsync = true`, dialect `sqlite-3-38`). Plan 03 adds the `StegoImage.sq` schema and `VaultSqlSource`; Plan 05 adds `initKoin`, `appModule`, `App()`, `AppViewModel`.

**Fact baked in (from `00-INDEX.md`):** `generateAsync = true` + `synchronous()` schema adapters is the pawdex pattern for the sync drivers. With no `.sq` files present yet, SqlDelight generates an empty `NyxDb` interface, which compiles.

- [ ] **Step 1: Write `client/build.gradle.kts`.**
  ```kotlin
  plugins {
      id("nyx.kmp.library")
      id("nyx.compose")
      alias(libs.plugins.sqldelight)
  }

  sqldelight {
      databases {
          create("NyxDb") {
              packageName.set("com.slothiesmooth.nyx.client.data.sqldelight")
              generateAsync.set(true)
              dialect(libs.sqldelight.dialect.sqlite338)
          }
      }
  }
  ```

- [ ] **Step 2: Create the placeholder source.**
  Run:
  ```bash
  mkdir -p client/src/commonMain/kotlin/com/slothiesmooth/nyx/client
  printf 'package com.slothiesmooth.nyx.client\n\ninternal object Placeholder\n' \
    > client/src/commonMain/kotlin/com/slothiesmooth/nyx/client/Placeholder.kt
  ```

- [ ] **Step 3: Add the include to `settings.gradle.kts`.**
  Append:
  ```kotlin
  include(":client")
  ```

- [ ] **Step 4: Verify `:client` compiles on the JVM target.**
  Run:
  ```bash
  ./gradlew :client:compileKotlinJvm
  ```
  Expected: `BUILD SUCCESSFUL` (SqlDelight generates an empty `NyxDb` and the module compiles).

  If SqlDelight fails to configure the `wasmJs` source set on this stack, the fallback is to keep the plugin applied but scope the generated database to non-wasm source sets in Plan 03 (documented open item — see `00-INDEX.md`: no wasm SqlDelight driver in v1). For the phase-01 skeleton, `compileKotlinJvm` is the required gate and does not touch wasm.

- [ ] **Step 5: Commit.**
  ```bash
  git add settings.gradle.kts client
  git commit -m "build: add :client skeleton with SqlDelight NyxDb config"
  ```

## Task 13: Entry app skeletons + design-library snapshot module

**Files:**
- Modify: `settings.gradle.kts` (append includes)
- Create: `androidApp/build.gradle.kts`, `androidApp/src/main/AndroidManifest.xml`, `androidApp/src/main/kotlin/com/slothiesmooth/nyx/Placeholder.kt`
- Create: `desktopApp/build.gradle.kts`, `desktopApp/src/main/kotlin/com/slothiesmooth/nyx/desktop/Placeholder.kt`
- Create: `webApp/build.gradle.kts`, `webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/Placeholder.kt`
- Create: `shared/design-library/snapshot/build.gradle.kts`, `shared/design-library/snapshot/src/main/kotlin/com/slothiesmooth/nyx/designlibrary/snapshot/Placeholder.kt`

**Interfaces:**
- **Consumes:** catalog plugin aliases (`android.application`, `kotlin.android`, `kotlin.jvm`, `kotlin.multiplatform`, `kotlin.plugin.compose`, `compose.multiplatform`, `android.library`, `paparazzi`); `compose.*` dependency notations.
- **Produces:** the three entry-app skeletons and the paparazzi snapshot module. Plan 07 adds the platform modules and real entry points; Plan 04 adds the snapshot tests.

**Facts baked in (from `00-INDEX.md`):** desktopApp is a plain `kotlin("jvm")` + Compose module. webApp needs the KMP plugin (wasmJs is only exposed there) with a single wasmJs target + Compose. The snapshot module is a plain `com.android.library` that compiles Kotlin via AGP-9's built-in Kotlin support (NO `org.jetbrains.kotlin.android`), plus paparazzi `2.0.0-alpha05` (alpha04 throws `NoSuchMethodError` on Gradle 9.4.1). Paparazzi golden recording and the Compose wiring for snapshots are Plan 04's job — this task only proves the plugin applies and the module assembles.

- [ ] **Step 1: Write `androidApp/build.gradle.kts`.**
  ```kotlin
  plugins {
      alias(libs.plugins.android.application)
      alias(libs.plugins.kotlin.android)
      alias(libs.plugins.kotlin.plugin.compose)
      alias(libs.plugins.compose.multiplatform)
  }

  android {
      namespace = "com.slothiesmooth.nyx"
      compileSdk = 36

      defaultConfig {
          applicationId = "com.slothiesmooth.nyx"
          minSdk = 24
          targetSdk = 36
          versionCode = 1
          versionName = "1.0.0"
      }

      buildTypes {
          getByName("release") {
              isMinifyEnabled = false
          }
      }

      compileOptions {
          sourceCompatibility = JavaVersion.VERSION_21
          targetCompatibility = JavaVersion.VERSION_21
      }
  }

  kotlin {
      jvmToolchain(21)
  }

  dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)
      implementation(libs.androidx.activity.compose)
  }
  ```

- [ ] **Step 2: Create the Android manifest and placeholder.**
  Run:
  ```bash
  mkdir -p androidApp/src/main/kotlin/com/slothiesmooth/nyx
  cat > androidApp/src/main/AndroidManifest.xml <<'EOF'
  <?xml version="1.0" encoding="utf-8"?>
  <manifest xmlns:android="http://schemas.android.com/apk/res/android">
      <application android:label="Nyx" />
  </manifest>
  EOF
  printf 'package com.slothiesmooth.nyx\n\ninternal object Placeholder\n' \
    > androidApp/src/main/kotlin/com/slothiesmooth/nyx/Placeholder.kt
  ```

- [ ] **Step 3: Write `desktopApp/build.gradle.kts` and its placeholder.**
  ```kotlin
  plugins {
      alias(libs.plugins.kotlin.jvm)
      alias(libs.plugins.kotlin.plugin.compose)
      alias(libs.plugins.compose.multiplatform)
  }

  kotlin {
      jvmToolchain(21)
  }

  dependencies {
      implementation(compose.runtime)
      implementation(compose.desktop.currentOs)
  }
  ```
  Then:
  ```bash
  mkdir -p desktopApp/src/main/kotlin/com/slothiesmooth/nyx/desktop
  printf 'package com.slothiesmooth.nyx.desktop\n\ninternal object Placeholder\n' \
    > desktopApp/src/main/kotlin/com/slothiesmooth/nyx/desktop/Placeholder.kt
  ```

- [ ] **Step 4: Write `webApp/build.gradle.kts` and its placeholder.**
  ```kotlin
  plugins {
      alias(libs.plugins.kotlin.multiplatform)
      alias(libs.plugins.kotlin.plugin.compose)
      alias(libs.plugins.compose.multiplatform)
  }

  kotlin {
      jvmToolchain(21)

      wasmJs {
          browser()
          binaries.executable()
      }

      sourceSets {
          wasmJsMain.dependencies {
              implementation(compose.runtime)
              implementation(compose.foundation)
              implementation(compose.ui)
          }
      }
  }
  ```
  Then:
  ```bash
  mkdir -p webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web
  printf 'package com.slothiesmooth.nyx.web\n\ninternal object Placeholder\n' \
    > webApp/src/wasmJsMain/kotlin/com/slothiesmooth/nyx/web/Placeholder.kt
  ```
  (The `outputModuleName`, `ComposeViewport` entry point, `index.html`, and `styles.css` are added in Plan 07 — the skeleton only needs to compile the wasmJs target.)

- [ ] **Step 5: Write `shared/design-library/snapshot/build.gradle.kts` and its placeholder.**
  ```kotlin
  plugins {
      alias(libs.plugins.android.library)
      alias(libs.plugins.paparazzi)
  }

  android {
      namespace = "com.slothiesmooth.nyx.designlibrary.snapshot"
      compileSdk = 36

      defaultConfig {
          minSdk = 24
      }

      compileOptions {
          sourceCompatibility = JavaVersion.VERSION_21
          targetCompatibility = JavaVersion.VERSION_21
      }
  }
  ```
  Then:
  ```bash
  mkdir -p shared/design-library/snapshot/src/main/kotlin/com/slothiesmooth/nyx/designlibrary/snapshot
  printf 'package com.slothiesmooth.nyx.designlibrary.snapshot\n\ninternal object Placeholder\n' \
    > shared/design-library/snapshot/src/main/kotlin/com/slothiesmooth/nyx/designlibrary/snapshot/Placeholder.kt
  ```

- [ ] **Step 6: Add the includes to `settings.gradle.kts`.**
  Append:
  ```kotlin
  include(
      ":androidApp",
      ":desktopApp",
      ":webApp",
      ":shared:design-library:snapshot",
  )
  ```

- [ ] **Step 7: Verify the desktop and web apps compile on Linux.**
  Run:
  ```bash
  ./gradlew :desktopApp:compileKotlin :webApp:compileKotlinWasmJs
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Verify the Android app assembles a debug APK.**
  Run:
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL` (produces `androidApp/build/outputs/apk/debug/androidApp-debug.apk`).

- [ ] **Step 9: Verify the snapshot module assembles and paparazzi applied.**
  Run:
  ```bash
  ./gradlew :shared:design-library:snapshot:assembleDebug && ./gradlew :shared:design-library:snapshot:tasks | grep -i paparazzi
  ```
  Expected: `BUILD SUCCESSFUL` and the task list includes `recordPaparazzi*` / `verifyPaparazzi*` tasks — proving paparazzi alpha05 applies cleanly on Gradle 9.4.1 (the alpha04 → alpha05 fix from `00-INDEX.md`).

- [ ] **Step 10: Commit.**
  ```bash
  git add settings.gradle.kts androidApp desktopApp webApp shared/design-library/snapshot
  git commit -m "build: add entry app skeletons and paparazzi snapshot module"
  ```

## Task 14: CI workflow + remove the dead workflow, final green gate

**Files:**
- Create: `.github/workflows/ci.yml`
- Delete: `.github/workflows/check.yml`

**Interfaces:**
- **Consumes:** `detektCheck`, `spotlessCheck` (Task 8), `:androidApp:assembleDebug` (Task 13), the `allTests` KMP aggregate.
- **Produces:** the CI pipeline every later plan's PRs run against.

**Decision baked in:** The old `.github/workflows/check.yml` references dead modules/tasks (`:app:assembleDebug`, `konsist_test:test`) and would fail on every PR against the new tree, so it is deleted now (not deferred to Plan 08). The new `ci.yml` runs on PRs and pushes to `develop`. iOS (macOS lane), `verifyPaparazzi`, and wasm browser tests are added in later phases; the phase-01 CI stays deterministic on Linux runners. `allTests -x wasmJsBrowserTest` is a no-op on the current skeletons (no tests) and remains correct as later phases add `commonTest`s.

- [ ] **Step 1: Delete the old workflow.**
  ```bash
  git rm .github/workflows/check.yml
  ```

- [ ] **Step 2: Write `.github/workflows/ci.yml`.**
  ```yaml
  name: CI

  on:
    pull_request:
    push:
      branches: [ develop ]

  concurrency:
    group: ci-${{ github.ref }}
    cancel-in-progress: true

  jobs:
    quality:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-java@v4
          with:
            distribution: temurin
            java-version: '21'
        - uses: gradle/actions/setup-gradle@v4
        - name: Detekt and Spotless
          run: ./gradlew detektCheck spotlessCheck --continue

    android:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-java@v4
          with:
            distribution: temurin
            java-version: '21'
        - uses: gradle/actions/setup-gradle@v4
        - name: Assemble Android debug
          run: ./gradlew :androidApp:assembleDebug

    tests:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-java@v4
          with:
            distribution: temurin
            java-version: '21'
        - uses: gradle/actions/setup-gradle@v4
        - name: Run host tests
          run: ./gradlew allTests -x wasmJsBrowserTest --continue
  ```

- [ ] **Step 3: Verify the quality gates pass locally (mirrors the CI `quality` job).**
  Run:
  ```bash
  ./gradlew detektCheck spotlessCheck
  ```
  Expected: `BUILD SUCCESSFUL`. If spotless reports formatting diffs on the generated build files or placeholders, run `./gradlew spotlessApply` once, re-run `spotlessCheck`, and re-commit — the generated files are ktlint-clean by construction, so this should pass first time.

- [ ] **Step 4: Verify the tests job command is green locally.**
  Run:
  ```bash
  ./gradlew allTests -x wasmJsBrowserTest --continue
  ```
  Expected: `BUILD SUCCESSFUL` (no test sources yet → aggregate task is a no-op).

- [ ] **Step 5: Full skeleton build gate.**
  Run:
  ```bash
  ./gradlew projects && ./gradlew build -x lint -x wasmJsBrowserTest
  ```
  Expected: `BUILD SUCCESSFUL`. `projects` must list all 28 modules (3 apps, `:client`, `:crypto`, `:steganography`, 6 shared incl. snapshot, 16 feature modules). `build` is excluded from `lint` (Android lint on empty modules is noise) and from `wasmJsBrowserTest` (no browser on the dev box). If any native/iOS task appears despite Linux, confirm `kotlin.native.ignoreDisabledTargets=true` is present in `gradle.properties` (Task 3).

- [ ] **Step 6: Commit.**
  ```bash
  git add .github/workflows/ci.yml
  git commit -m "ci: add Linux CI (detekt/spotless, android assemble, host tests); drop dead check.yml"
  ```

## Done criteria for Phase 01

- `./gradlew projects` lists all 28 new modules and no old modules.
- `./gradlew build -x lint -x wasmJsBrowserTest` is `BUILD SUCCESSFUL`.
- `./gradlew :client:compileKotlinJvm`, `:crypto:compileKotlinJvm`, `:desktopApp:compileKotlin`, `:webApp:compileKotlinWasmJs`, `:androidApp:assembleDebug` all pass.
- `./gradlew detektCheck spotlessCheck` pass with zero issues.
- Gradle wrapper is 9.4.1 with a pinned distribution checksum.
- Old tree lives under `legacy/` (not deleted); `buildSrc` no longer auto-included; the `steganography/` path is owned by the new module.
- `.github/workflows/ci.yml` is the only workflow; `check.yml` is gone.
