# Design Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver `:shared:design-library` — the complete Nx token system (5 locked palettes, spacing, radius, shadow, JetBrains Mono type, 21-icon set), `NxTheme` + Material3 bridge, 16 components with previews — plus the `:shared:design-library:snapshot` Paparazzi module with recorded goldens and green token tests.

**Architecture:** A 6-target KMP Compose module (`com.slothiesmooth.nyx.designlibrary`) organized pawdex-style: `tokens/` (colors, palette, spacing, radius, shadow, type, theme, M3 bridge, preview infra), `atoms/` → `molecules/` → `templates/` components, each `NxX.kt` paired with `NxXPreview.kt` exposing a public `NxXSample()` reused by both `@AllThemePreview` IDE previews and Paparazzi snapshot tests. The snapshot module is a plain `com.android.library` JVM-test module parameterized over `NxPalette.entries`.

**Tech Stack:** Kotlin 2.3.21, Compose Multiplatform 1.10.3 (foundation, material3, components.resources, uiToolingPreview), kotlinx-collections-immutable 0.4.0, kotlin.test, Paparazzi 2.0.0-alpha05, AGP 9.2.0 (`com.android.kotlin.multiplatform.library` for the KMP module, `com.android.library` for snapshot).

## Global Constraints

Copied verbatim from `00-INDEX.md` (all of 00-INDEX applies; these are the lines this plan exercises):

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
- Composables are dumb: no filtering/sorting/mapping/pluralization in UI — VM state exposes
  render-ready values.
- Tests: kotlin.test + kotlinx-coroutines-test + hand-written fakes only. No mockk/kotest/turbine.
- No `println`; logging via Kermit.
- Commit after every green test cycle (conventional commits).
- Paparazzi 2.0.0-alpha05 required (alpha04 = NoSuchMethodError on Gradle 9.4.1 at report
  generation). Goldens are environment-sensitive (fonts). Record on Linux (dev machine) =
  matches ubuntu CI.

Phase-specific constraints:

- **Color purity:** `NxColors.kt` is the ONLY runtime file with raw ARGB literals. Documented
  exceptions (spec §8): `@Preview(backgroundColor = ...)` annotation args in
  `AllThemePreview.kt` (annotation args must be compile-time `Long` constants and cannot
  reference `NxColors`) and expected values in `NxColorsTest.kt`. Every component consumes
  `NxTokens.*` / `MaterialTheme` slots / `Color.Unspecified` / `Color.Transparent` /
  `Color.Black` (shadow ambient) only.
- **Dimension purity:** dp/sp literals appear ONLY as named property declarations (token
  objects like `NxSpacing`, or file-scope `private val` constants at the top of a component
  file, e.g. `private val TopBarHeight = 56.dp`). Never inline in composable bodies. This
  satisfies detekt MagicNumber via `ignorePropertyDeclaration: true` — coordinate with plan 01's
  `detekt.yml` (see Open Questions); do NOT add `@Suppress` anywhere except `NxColors.kt`.
- **Preview pattern (mandatory for every component):** `NxX.kt` + sibling `NxXPreview.kt`
  containing (a) public `@Composable fun NxXSample()` enumerating ALL visual states and
  (b) `private @AllThemePreview @Composable fun NxXPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) = NxTheme(palette) { NxXSample() }`.
  The public sample is the single source reused by Paparazzi.
- **Interface contracts:** all public signatures in this plan match the
  `:shared:design-library` section of `00-INDEX.md` exactly (NxColors slots, NxPalette shape,
  NxTokens, NxTheme, `toMaterial3ColorScheme(dark: Boolean)`, NxIconKind values, component
  parameter lists). Downstream plans (05, 06, 07) import them as written here.
- **TDD interpretation for this phase:** `NxColors`/`NxPalette` get real red-green TDD
  (locked-hex + WCAG assertions). Composables contain zero logic by design — their
  verification is (1) `./gradlew :shared:design-library:compileKotlinJvm` per task and
  (2) the Paparazzi golden recorded in the final task. Do not write throwaway "it composes"
  unit tests (tests serve purpose, not count).

## Context you need before starting (read once, facts baked in)

- **Pawdex reference source** (working code this plan ports, `Pd` → `Nx`):
  `/tmp/claude-1000/-home-slothie-StudioProjects-Nyx/446ab22c-565d-4c13-a434-33c246daab52/scratchpad/pawdex/shared/design-library/`.
  If that scratchpad path no longer exists, everything needed is already inlined in this plan —
  every file below is complete; the reference is optional.
- **Module paths:** library at `shared/design-library/`, snapshot at
  `shared/design-library/snapshot/` (Gradle paths `:shared:design-library`,
  `:shared:design-library:snapshot`). Plan 01 registers both in `settings.gradle.kts`
  ("all module skeletons compile"); Task 1 verifies and repairs if missing.
- **Compose resources:** the `compose.resources {}` block comes from the
  `org.jetbrains.compose` plugin (project level, NOT inside `kotlin {}`). We pin
  `packageOfResClass = "com.slothiesmooth.nyx.designlibrary.resources"` so `Res` imports are
  deterministic regardless of root project name. Font files must be lowercase snake_case
  (resource accessor names are derived from file names).
- **AGP KMP plugin quirks:** no buildTypes/flavors/BuildConfig; `androidResources.enable = true`
  is required for compose resources to be packaged into the Android target (pawdex-proven).
  The snapshot module is plain `com.android.library` and compiles Kotlin WITHOUT
  `org.jetbrains.kotlin.android` (AGP 9 built-in Kotlin — empirically verified in research).
- **Paparazzi:** 2.0.0-alpha05 only (alpha04 fails at report generation on Gradle 9.4.1 —
  cashapp/paparazzi#2227, empirically reproduced). alpha05 officially supports the AGP KMP
  library plugin. `DeviceConfig.PIXEL_5` + `SessionParams.RenderingMode.SHRINK`.
- **WCAG:** every palette below was contrast-verified (WCAG 2.x relative-luminance formula)
  before being locked into this plan. The verified pairs, all ≥ 4.5:1 in all 5 palettes:
  `fg/bg`, `fg/bgElev1`, `fg/bgElev2`, `fgMuted/bg`, `brandFg/brand`, `fgOnBrand/brand`,
  `fgOnBrand/danger` (Danger button text), `danger/bg`, `success/bg`, `warning/bg`, `info/bg`,
  `codeFg/codeBg`, `brand/bg`. Measured minima: Midnight 6.68, Espresso 8.64, Nardo 6.53,
  Cream 4.57 (success/bg), Mist 4.53 (warning/bg). `NxColorsTest` re-asserts these at
  every build.
- **Version catalog aliases used in this plan** (pawdex naming; reconcile in Task 1 Step 1):
  `libs.plugins.kotlin.multiplatform`, `libs.plugins.compose.compiler`,
  `libs.plugins.compose.multiplatform`, `libs.plugins.android.kmp.library`,
  `libs.plugins.paparazzi`, `libs.kotlin.test`, `libs.kotlinx.collections.immutable`,
  `libs.junit4`, `libs.versions.android.compileSdk`, `libs.versions.android.minSdk`.

---

### Task 1: Module wiring, build files, and JetBrains Mono font resources

**Files:**
- Modify: `settings.gradle.kts` (verify/add the two includes)
- Modify: `gradle/libs.versions.toml` (verify/add paparazzi + junit4 + collections-immutable entries)
- Create/Overwrite: `shared/design-library/build.gradle.kts`
- Create: `shared/design-library/src/commonMain/composeResources/font/jetbrainsmono_regular.ttf` (+ medium, semibold, bold, italic — downloaded, binary)

**Interfaces:**
- Consumes: plan 01 module skeletons, version catalog, Gradle 9.4.1 wrapper.
- Produces: a compiling `:shared:design-library` module with compose resources (fonts) that every later task builds on.

**Steps:**

- [ ] **Step 1: Reconcile version catalog.** Open `gradle/libs.versions.toml`. Verify the entries below exist (plan 01 owns this file; alias names there win — if plan 01 chose different alias names for the same coordinates, use plan 01's names throughout this plan). Add whatever is missing:

```toml
[versions]
# ... existing entries from plan 01 ...
paparazzi = "2.0.0-alpha05"   # pinned: alpha04 breaks on Gradle 9.4.1 (paparazzi#2227)
junit4 = "4.13.2"

[libraries]
# ... existing ...
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-collections-immutable = { module = "org.jetbrains.kotlinx:kotlinx-collections-immutable", version.ref = "kotlinx-collections-immutable" }
junit4 = { module = "junit:junit", version.ref = "junit4" }

[plugins]
# ... existing ...
paparazzi = { id = "app.cash.paparazzi", version.ref = "paparazzi" }
```

- [ ] **Step 2: Verify module registration.** Run:

```bash
grep -n "design-library" settings.gradle.kts
```

Expected: lines including both `include(":shared:design-library")` and `include(":shared:design-library:snapshot")`. If either is missing, add it.

- [ ] **Step 3: Write the library build file.** Overwrite `shared/design-library/build.gradle.kts` with exactly (if plan 01 provides a KMP-compose convention plugin producing this identical configuration, applying it instead is fine — the effective config below is what matters):

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
        namespace = "com.slothiesmooth.nyx.designlibrary"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
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
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }
        commonMain.dependencies {
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.collections.immutable)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.slothiesmooth.nyx.designlibrary.resources"
    generateResClass = always
}
```

- [ ] **Step 4: Download JetBrains Mono and place the 5 TTFs.** From the repo root (URL verified 2026-07-11, HTTP 200):

```bash
mkdir -p shared/design-library/src/commonMain/composeResources/font
curl -L -o /tmp/JetBrainsMono-2.304.zip \
  https://github.com/JetBrains/JetBrainsMono/releases/download/v2.304/JetBrainsMono-2.304.zip
unzip -o /tmp/JetBrainsMono-2.304.zip -d /tmp/jetbrainsmono
cp /tmp/jetbrainsmono/fonts/ttf/JetBrainsMono-Regular.ttf  shared/design-library/src/commonMain/composeResources/font/jetbrainsmono_regular.ttf
cp /tmp/jetbrainsmono/fonts/ttf/JetBrainsMono-Medium.ttf   shared/design-library/src/commonMain/composeResources/font/jetbrainsmono_medium.ttf
cp /tmp/jetbrainsmono/fonts/ttf/JetBrainsMono-SemiBold.ttf shared/design-library/src/commonMain/composeResources/font/jetbrainsmono_semibold.ttf
cp /tmp/jetbrainsmono/fonts/ttf/JetBrainsMono-Bold.ttf     shared/design-library/src/commonMain/composeResources/font/jetbrainsmono_bold.ttf
cp /tmp/jetbrainsmono/fonts/ttf/JetBrainsMono-Italic.ttf   shared/design-library/src/commonMain/composeResources/font/jetbrainsmono_italic.ttf
```

Verify: `ls shared/design-library/src/commonMain/composeResources/font/` lists exactly the 5 lowercase snake_case `.ttf` files (each ~270 KB). Lowercase snake_case is mandatory — compose resources derives Kotlin accessor names (`Res.font.jetbrainsmono_regular`) from file names.

- [ ] **Step 5: Verify the module compiles and Res is generated.** Run:

```bash
./gradlew :shared:design-library:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. (This also runs the compose-resources codegen; the generated `Res.font.*` accessors land in package `com.slothiesmooth.nyx.designlibrary.resources`.)

- [ ] **Step 6: Commit.**

```bash
git add settings.gradle.kts gradle/libs.versions.toml shared/design-library/build.gradle.kts shared/design-library/src/commonMain/composeResources
git commit -m "feat(design-library): module build wiring and JetBrains Mono font resources"
```

---

### Task 2: NxColors + NxPalette — locked hex + WCAG token tests (TDD)

**Files:**
- Test: `shared/design-library/src/commonTest/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxColorsTest.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxColors.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxPalette.kt`

**Interfaces:**
- Consumes: Task 1 module.
- Produces (00-INDEX contract):
  - `@Immutable data class NxColors(28 slots — bg..codeFg as listed in the contract)`
  - `val NxColorsMidnight/NxColorsEspresso/NxColorsNardo/NxColorsCream/NxColorsMist: NxColors`
  - `enum class NxPalette(val displayName: String, val dark: Boolean) { Midnight, Espresso, Nardo, Cream, Mist }` with `val colors: NxColors` and `companion object { val DefaultDark = Midnight; val DefaultLight = Cream }`

Note: 00-INDEX prose says "27 slots"; the contract data class enumerates 28 fields — the data class is authoritative and is reproduced exactly below.

**Palette identity (locked here, per spec §8 "hex table locked in the design-library phase"):**

| Palette | Mood | bg | fg | brand |
|---|---|---|---|---|
| Midnight (default dark) | deep night indigo, violet brand | `#14121F` | `#ECE9F6` | `#A78BFA` |
| Espresso (dark) | pure-black OLED | `#000000` | `#F2F2F7` | `#B8A6FF` |
| Nardo (dark) | dark violet | `#1D1430` | `#F0EAF9` | `#C084FC` |
| Cream (default light) | cool gray, deep violet brand | `#F4F4F8` | `#1B1926` | `#6D28D9` |
| Mist (light) | warm cream, plum brand | `#FAF5EC` | `#26201A` | `#7E22CE` |

**Steps:**

- [ ] **Step 1: Write the failing test.** Create `NxColorsTest.kt` (detekt's default MagicNumber excludes `**/commonTest/**`, so literals here are fine — and are a documented exception regardless):

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NxColorsTest {

    @Test
    fun `palette bg slots match locked hex`() {
        assertEquals(Color(0xFF14121F), NxColorsMidnight.bg)
        assertEquals(Color(0xFF000000), NxColorsEspresso.bg)
        assertEquals(Color(0xFF1D1430), NxColorsNardo.bg)
        assertEquals(Color(0xFFF4F4F8), NxColorsCream.bg)
        assertEquals(Color(0xFFFAF5EC), NxColorsMist.bg)
    }

    @Test
    fun `palette fg slots match locked hex`() {
        assertEquals(Color(0xFFECE9F6), NxColorsMidnight.fg)
        assertEquals(Color(0xFFF2F2F7), NxColorsEspresso.fg)
        assertEquals(Color(0xFFF0EAF9), NxColorsNardo.fg)
        assertEquals(Color(0xFF1B1926), NxColorsCream.fg)
        assertEquals(Color(0xFF26201A), NxColorsMist.fg)
    }

    @Test
    fun `palette brand slots match locked hex`() {
        assertEquals(Color(0xFFA78BFA), NxColorsMidnight.brand)
        assertEquals(Color(0xFFB8A6FF), NxColorsEspresso.brand)
        assertEquals(Color(0xFFC084FC), NxColorsNardo.brand)
        assertEquals(Color(0xFF6D28D9), NxColorsCream.brand)
        assertEquals(Color(0xFF7E22CE), NxColorsMist.brand)
    }

    @Test
    fun `palette enum wiring and defaults`() {
        assertEquals(NxPalette.Midnight, NxPalette.DefaultDark)
        assertEquals(NxPalette.Cream, NxPalette.DefaultLight)
        assertTrue(NxPalette.Midnight.dark)
        assertTrue(NxPalette.Espresso.dark)
        assertTrue(NxPalette.Nardo.dark)
        assertFalse(NxPalette.Cream.dark)
        assertFalse(NxPalette.Mist.dark)
        assertEquals(NxColorsMidnight, NxPalette.Midnight.colors)
        assertEquals(NxColorsEspresso, NxPalette.Espresso.colors)
        assertEquals(NxColorsNardo, NxPalette.Nardo.colors)
        assertEquals(NxColorsCream, NxPalette.Cream.colors)
        assertEquals(NxColorsMist, NxPalette.Mist.colors)
    }

    @Test
    fun `every palette meets WCAG AA on core foreground-background pairs`() {
        NxPalette.entries.forEach { palette ->
            val c = palette.colors
            val pairs = listOf(
                Triple("fg on bg", c.fg, c.bg),
                Triple("fg on bgElev1", c.fg, c.bgElev1),
                Triple("fg on bgElev2", c.fg, c.bgElev2),
                Triple("fgMuted on bg", c.fgMuted, c.bg),
                Triple("brandFg on brand", c.brandFg, c.brand),
                Triple("fgOnBrand on brand", c.fgOnBrand, c.brand),
                Triple("fgOnBrand on danger", c.fgOnBrand, c.danger),
                Triple("danger on bg", c.danger, c.bg),
                Triple("success on bg", c.success, c.bg),
                Triple("warning on bg", c.warning, c.bg),
                Triple("info on bg", c.info, c.bg),
                Triple("codeFg on codeBg", c.codeFg, c.codeBg),
            )
            pairs.forEach { (label, front, back) ->
                val ratio = contrastRatio(front, back)
                assertTrue(
                    ratio >= WCAG_AA_MIN_RATIO,
                    "${palette.name}: $label is $ratio, expected >= $WCAG_AA_MIN_RATIO",
                )
            }
        }
    }
}

private const val WCAG_AA_MIN_RATIO = 4.5

private fun linearize(channel: Float): Double {
    val value = channel.toDouble()
    return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}

private fun relativeLuminance(color: Color): Double =
    0.2126 * linearize(color.red) + 0.7152 * linearize(color.green) + 0.0722 * linearize(color.blue)

private fun contrastRatio(a: Color, b: Color): Double {
    val luminanceA = relativeLuminance(a)
    val luminanceB = relativeLuminance(b)
    val lighter = maxOf(luminanceA, luminanceB)
    val darker = minOf(luminanceA, luminanceB)
    return (lighter + 0.05) / (darker + 0.05)
}
```

- [ ] **Step 2: Run the test — expect RED (compile failure).**

```bash
./gradlew :shared:design-library:jvmTest --tests "com.slothiesmooth.nyx.designlibrary.tokens.NxColorsTest"
```

Expected failure: compilation error in `commonTest`, `Unresolved reference 'NxColorsMidnight'` (and siblings). That is the red state — the production types do not exist yet.

- [ ] **Step 3: Write `NxColors.kt`.** This is THE one raw-ARGB file; the suppression below is the single documented exception in the whole codebase:

```kotlin
// The single raw-ARGB file in the codebase (spec §8): every literal below is a locked design
// token verified for WCAG AA contrast in NxColorsTest. Suppression is documented in 00-INDEX.
@file:Suppress("MagicNumber")

package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class NxColors(
    val bg: Color, val bgElev1: Color, val bgElev2: Color, val bgSunken: Color, val bgInverse: Color,
    val fg: Color, val fgMuted: Color, val fgSubtle: Color, val fgFaint: Color, val fgInverse: Color, val fgOnBrand: Color,
    val brand: Color, val brandSoft: Color, val brandFg: Color,
    val accentViolet: Color, val accentCyan: Color, val accentAmber: Color, val accentRose: Color,
    val border: Color, val borderStrong: Color, val borderBrand: Color, val divider: Color,
    val success: Color, val warning: Color, val danger: Color, val info: Color,
    val codeBg: Color, val codeFg: Color,
)

/** Default dark. Deep night indigo with a violet brand — Nyx, goddess of night. */
val NxColorsMidnight = NxColors(
    bg = Color(0xFF14121F), bgElev1 = Color(0xFF1B1929), bgElev2 = Color(0xFF232033), bgSunken = Color(0xFF0E0C17), bgInverse = Color(0xFFECE9F6),
    fg = Color(0xFFECE9F6), fgMuted = Color(0xFFB5AECB), fgSubtle = Color(0xFF8B84A3), fgFaint = Color(0xFF5F5978),
    fgInverse = Color(0xFF14121F), fgOnBrand = Color(0xFF14121F),
    brand = Color(0xFFA78BFA), brandSoft = Color(0x2EA78BFA), brandFg = Color(0xFF14121F),
    accentViolet = Color(0xFFC4B5FD), accentCyan = Color(0xFF7DD3FC), accentAmber = Color(0xFFFCD34D), accentRose = Color(0xFFFDA4AF),
    border = Color(0x2EECE9F6), borderStrong = Color(0x47ECE9F6), borderBrand = Color(0xFFA78BFA), divider = Color(0x14ECE9F6),
    success = Color(0xFF5FD39A), warning = Color(0xFFFBBF24), danger = Color(0xFFF87171), info = Color(0xFF7DD3FC),
    codeBg = Color(0xFF0E0C17), codeFg = Color(0xFFC4B5FD),
)

/** Pure-black OLED dark. */
val NxColorsEspresso = NxColors(
    bg = Color(0xFF000000), bgElev1 = Color(0xFF0C0C14), bgElev2 = Color(0xFF16161F), bgSunken = Color(0xFF000000), bgInverse = Color(0xFFF2F2F7),
    fg = Color(0xFFF2F2F7), fgMuted = Color(0xFFB3B3C2), fgSubtle = Color(0xFF85859A), fgFaint = Color(0xFF55556A),
    fgInverse = Color(0xFF000000), fgOnBrand = Color(0xFF0A0A12),
    brand = Color(0xFFB8A6FF), brandSoft = Color(0x29B8A6FF), brandFg = Color(0xFF0A0A12),
    accentViolet = Color(0xFFCDBFFF), accentCyan = Color(0xFF8AE0FF), accentAmber = Color(0xFFFFD666), accentRose = Color(0xFFFFAFC0),
    border = Color(0x29F2F2F7), borderStrong = Color(0x42F2F2F7), borderBrand = Color(0xFFB8A6FF), divider = Color(0x12F2F2F7),
    success = Color(0xFF63E0A5), warning = Color(0xFFFFC94D), danger = Color(0xFFFF8A80), info = Color(0xFF8AE0FF),
    codeBg = Color(0xFF0C0C14), codeFg = Color(0xFFCDBFFF),
)

/** Dark violet. */
val NxColorsNardo = NxColors(
    bg = Color(0xFF1D1430), bgElev1 = Color(0xFF251A3D), bgElev2 = Color(0xFF2E2149), bgSunken = Color(0xFF150E24), bgInverse = Color(0xFFF0EAF9),
    fg = Color(0xFFF0EAF9), fgMuted = Color(0xFFC0B4D6), fgSubtle = Color(0xFF9488AE), fgFaint = Color(0xFF675C80),
    fgInverse = Color(0xFF1D1430), fgOnBrand = Color(0xFF1D1430),
    brand = Color(0xFFC084FC), brandSoft = Color(0x2EC084FC), brandFg = Color(0xFF1D1430),
    accentViolet = Color(0xFFDDD6FE), accentCyan = Color(0xFF67E8F9), accentAmber = Color(0xFFFDE047), accentRose = Color(0xFFFDA4AF),
    border = Color(0x2EF0EAF9), borderStrong = Color(0x47F0EAF9), borderBrand = Color(0xFFC084FC), divider = Color(0x14F0EAF9),
    success = Color(0xFF6EE7B7), warning = Color(0xFFFBBF24), danger = Color(0xFFFB7185), info = Color(0xFF67E8F9),
    codeBg = Color(0xFF150E24), codeFg = Color(0xFFDDD6FE),
)

/** Default light. Cool moonlit grays with a deep violet brand. */
val NxColorsCream = NxColors(
    bg = Color(0xFFF4F4F8), bgElev1 = Color(0xFFFAFAFD), bgElev2 = Color(0xFFFFFFFF), bgSunken = Color(0xFFE8E8F0), bgInverse = Color(0xFF17151F),
    fg = Color(0xFF1B1926), fgMuted = Color(0xFF4A475C), fgSubtle = Color(0xFF6E6B82), fgFaint = Color(0xFF9A97AB),
    fgInverse = Color(0xFFF4F4F8), fgOnBrand = Color(0xFFFFFFFF),
    brand = Color(0xFF6D28D9), brandSoft = Color(0x226D28D9), brandFg = Color(0xFFFFFFFF),
    accentViolet = Color(0xFF7C3AED), accentCyan = Color(0xFF0E7490), accentAmber = Color(0xFFB45309), accentRose = Color(0xFFBE123C),
    border = Color(0x1F1B1926), borderStrong = Color(0x381B1926), borderBrand = Color(0xFF6D28D9), divider = Color(0x141B1926),
    success = Color(0xFF15803D), warning = Color(0xFFB45309), danger = Color(0xFFB91C1C), info = Color(0xFF0E7490),
    codeBg = Color(0xFFE8E8F0), codeFg = Color(0xFF4C1D95),
)

/** Warm light. Cream first-light tones with a plum brand. */
val NxColorsMist = NxColors(
    bg = Color(0xFFFAF5EC), bgElev1 = Color(0xFFFDFAF3), bgElev2 = Color(0xFFFFFFFF), bgSunken = Color(0xFFF0E8D9), bgInverse = Color(0xFF201B14),
    fg = Color(0xFF26201A), fgMuted = Color(0xFF575043), fgSubtle = Color(0xFF7B7365), fgFaint = Color(0xFFA69D8D),
    fgInverse = Color(0xFFFAF5EC), fgOnBrand = Color(0xFFFFFFFF),
    brand = Color(0xFF7E22CE), brandSoft = Color(0x227E22CE), brandFg = Color(0xFFFFFFFF),
    accentViolet = Color(0xFF9333EA), accentCyan = Color(0xFF155E75), accentAmber = Color(0xFFA16207), accentRose = Color(0xFFBE185D),
    border = Color(0x1F26201A), borderStrong = Color(0x3826201A), borderBrand = Color(0xFF7E22CE), divider = Color(0x1426201A),
    success = Color(0xFF15803D), warning = Color(0xFFA16207), danger = Color(0xFFB91C1C), info = Color(0xFF155E75),
    codeBg = Color(0xFFF0E8D9), codeFg = Color(0xFF6B21A8),
)
```

- [ ] **Step 4: Write `NxPalette.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

enum class NxPalette(val displayName: String, val dark: Boolean) {
    Midnight("Midnight", true),
    Espresso("Espresso", true),
    Nardo("Nardo", true),
    Cream("Cream", false),
    Mist("Mist", false);

    val colors: NxColors
        get() = when (this) {
            Midnight -> NxColorsMidnight
            Espresso -> NxColorsEspresso
            Nardo -> NxColorsNardo
            Cream -> NxColorsCream
            Mist -> NxColorsMist
        }

    companion object {
        val DefaultDark = Midnight
        val DefaultLight = Cream
    }
}
```

- [ ] **Step 5: Run the test — expect GREEN.**

```bash
./gradlew :shared:design-library:jvmTest --tests "com.slothiesmooth.nyx.designlibrary.tokens.NxColorsTest"
```

Expected: `BUILD SUCCESSFUL`, 5 tests passed. If a WCAG assertion fails, a hex was mistyped — compare against this plan's tables; do NOT loosen the 4.5 threshold.

- [ ] **Step 6: Commit.**

```bash
git add shared/design-library/src
git commit -m "feat(design-library): NxColors with five locked palettes and WCAG AA token tests"
```

---

### Task 3: NxSpacing, NxRadius, NxShadow, NxType (+ NxTextStyle enum, font loader)

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxSpacing.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxRadius.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxShadow.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxType.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxTextStyle.kt`

**Interfaces:**
- Consumes: Task 1 font resources (`Res.font.jetbrainsmono_*` in package `com.slothiesmooth.nyx.designlibrary.resources`).
- Produces:
  - `object NxSpacing { val s0..s10: Dp }` (0/4/8/12/16/24/32/48/64/96/128 — pawdex PdSpacing scale)
  - `object NxRadius { val xs/sm/md/lg/xl/xxl/pill: Dp }` (6/10/14/20/28/40/999)
  - `object NxShadow { @Composable fun xs/sm/md/lg(shape): Modifier }`
  - `@Immutable data class NxType(display, title, heading, subhead, body, bodyStrong, caption, kicker, mono: TextStyle)`
  - `fun nxTypeFor(family: FontFamily): NxType`
  - `@Composable fun rememberNxJetBrainsMono(): FontFamily` (5 weights)
  - `enum class NxTextStyle { Display, Title, Heading, Subhead, Body, BodyStrong, Caption, Kicker, Mono }`

No unit tests here: these are constant declarations with zero logic (locked-hex equivalents for dimensions would only restate the source — noise). Verification is compilation; visual truth is locked by the Paparazzi goldens in Task 22.

**Steps:**

- [ ] **Step 1: Write `NxSpacing.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object NxSpacing {
    val s0: Dp = 0.dp
    val s1: Dp = 4.dp
    val s2: Dp = 8.dp
    val s3: Dp = 12.dp
    val s4: Dp = 16.dp
    val s5: Dp = 24.dp
    val s6: Dp = 32.dp
    val s7: Dp = 48.dp
    val s8: Dp = 64.dp
    val s9: Dp = 96.dp
    val s10: Dp = 128.dp
}
```

- [ ] **Step 2: Write `NxRadius.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object NxRadius {
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 14.dp
    val lg: Dp = 20.dp
    val xl: Dp = 28.dp
    val xxl: Dp = 40.dp
    val pill: Dp = 999.dp
}
```

- [ ] **Step 3: Write `NxShadow.kt`** (Color.Black is a named Compose constant, not a raw ARGB literal — allowed):

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object NxShadow {
    private val elevationXs: Dp = 1.dp
    private val elevationSm: Dp = 3.dp
    private val elevationMd: Dp = 8.dp
    private val elevationLg: Dp = 18.dp

    @Composable
    @ReadOnlyComposable
    fun xs(shape: Shape = RoundedCornerShape(NxRadius.md)): Modifier =
        Modifier.shadow(elevation = elevationXs, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)

    @Composable
    @ReadOnlyComposable
    fun sm(shape: Shape = RoundedCornerShape(NxRadius.md)): Modifier =
        Modifier.shadow(elevation = elevationSm, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)

    @Composable
    @ReadOnlyComposable
    fun md(shape: Shape = RoundedCornerShape(NxRadius.lg)): Modifier =
        Modifier.shadow(elevation = elevationMd, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)

    @Composable
    @ReadOnlyComposable
    fun lg(shape: Shape = RoundedCornerShape(NxRadius.lg)): Modifier =
        Modifier.shadow(elevation = elevationLg, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
}
```

- [ ] **Step 4: Write `NxTextStyle.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

enum class NxTextStyle {
    Display, Title, Heading, Subhead, Body, BodyStrong, Caption, Kicker, Mono
}
```

- [ ] **Step 5: Write `NxType.kt`.** Unlike pawdex's `PdType` object of functions, the 00-INDEX contract exposes `NxTokens.type: NxType` as a resolved value — so `NxType` is an immutable style bundle built once per composition from the loaded font family:

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.resources.Res
import com.slothiesmooth.nyx.designlibrary.resources.jetbrainsmono_bold
import com.slothiesmooth.nyx.designlibrary.resources.jetbrainsmono_italic
import com.slothiesmooth.nyx.designlibrary.resources.jetbrainsmono_medium
import com.slothiesmooth.nyx.designlibrary.resources.jetbrainsmono_regular
import com.slothiesmooth.nyx.designlibrary.resources.jetbrainsmono_semibold
import org.jetbrains.compose.resources.Font

@Immutable
data class NxType(
    val display: TextStyle,
    val title: TextStyle,
    val heading: TextStyle,
    val subhead: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val caption: TextStyle,
    val kicker: TextStyle,
    val mono: TextStyle,
)

@Composable
fun rememberNxJetBrainsMono(): FontFamily {
    val regular = Font(Res.font.jetbrainsmono_regular, weight = FontWeight.Normal)
    val medium = Font(Res.font.jetbrainsmono_medium, weight = FontWeight.Medium)
    val semibold = Font(Res.font.jetbrainsmono_semibold, weight = FontWeight.SemiBold)
    val bold = Font(Res.font.jetbrainsmono_bold, weight = FontWeight.Bold)
    val italic = Font(Res.font.jetbrainsmono_italic, weight = FontWeight.Normal, style = FontStyle.Italic)
    return remember(regular, medium, semibold, bold, italic) {
        FontFamily(regular, medium, semibold, bold, italic)
    }
}

private val displaySize = 36.sp
private val displayLine = 38.sp
private val titleSize = 28.sp
private val titleLine = 32.sp
private val headingSize = 22.sp
private val headingLine = 26.sp
private val subheadSize = 18.sp
private val subheadLine = 22.sp
private val bodySize = 14.sp
private val bodyLine = 20.sp
private val captionSize = 12.sp
private val captionLine = 16.sp
private val kickerSize = 11.sp
private val kickerLine = 14.sp
private val monoSize = 28.sp
private val monoLine = 30.sp
private val tightTracking = (-0.04).em
private val titleTracking = (-0.025).em
private val headingTracking = (-0.02).em
private val kickerTracking = 0.14.em

fun nxTypeFor(family: FontFamily): NxType = NxType(
    display = TextStyle(fontFamily = family, fontSize = displaySize, fontWeight = FontWeight.Bold, letterSpacing = tightTracking, lineHeight = displayLine),
    title = TextStyle(fontFamily = family, fontSize = titleSize, fontWeight = FontWeight.Bold, letterSpacing = titleTracking, lineHeight = titleLine),
    heading = TextStyle(fontFamily = family, fontSize = headingSize, fontWeight = FontWeight.Bold, letterSpacing = headingTracking, lineHeight = headingLine),
    subhead = TextStyle(fontFamily = family, fontSize = subheadSize, fontWeight = FontWeight.Bold, letterSpacing = headingTracking, lineHeight = subheadLine),
    body = TextStyle(fontFamily = family, fontSize = bodySize, fontWeight = FontWeight.Normal, lineHeight = bodyLine),
    bodyStrong = TextStyle(fontFamily = family, fontSize = bodySize, fontWeight = FontWeight.SemiBold, lineHeight = bodyLine),
    caption = TextStyle(fontFamily = family, fontSize = captionSize, fontWeight = FontWeight.Normal, lineHeight = captionLine),
    kicker = TextStyle(fontFamily = family, fontSize = kickerSize, fontWeight = FontWeight.SemiBold, letterSpacing = kickerTracking, lineHeight = kickerLine),
    mono = TextStyle(fontFamily = family, fontSize = monoSize, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", letterSpacing = tightTracking, lineHeight = monoLine),
)
```

- [ ] **Step 6: Verify compilation.**

```bash
./gradlew :shared:design-library:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. (If `Res.font.*` is unresolved, the font file names in Task 1 Step 4 do not match — they must be exactly `jetbrainsmono_regular.ttf` etc.)

- [ ] **Step 7: Commit.**

```bash
git add shared/design-library/src
git commit -m "feat(design-library): spacing, radius, shadow, and JetBrains Mono type tokens"
```

---

### Task 4: NxTheme, NxTokens, NxMaterial3Bridge, AllThemePreview, NxPaletteProvider

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxTheme.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxMaterial3Bridge.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/AllThemePreview.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxPaletteProvider.kt`

**Interfaces:**
- Consumes: Tasks 2-3 (`NxColors`, `NxPalette`, `NxType`, `nxTypeFor`, `rememberNxJetBrainsMono`).
- Produces (00-INDEX contract):
  - `val LocalNxColors: ProvidableCompositionLocal<NxColors>`, `val LocalNxType: ProvidableCompositionLocal<NxType>`
  - `object NxTokens { colors: NxColors @Composable get; type: NxType @Composable get; spacing = NxSpacing; radius = NxRadius }`
  - `@Composable fun NxTheme(palette: NxPalette = NxPalette.DefaultDark, content: @Composable () -> Unit)`
  - `fun NxColors.toMaterial3ColorScheme(dark: Boolean): ColorScheme`
  - `annotation class AllThemePreview` (5 stacked @Preview, one per palette)
  - `class NxPaletteProvider : PreviewParameterProvider<NxPalette>`

Note on placement: `toMaterial3ColorScheme` is an extension function, but 00-INDEX pins its file as `tokens/NxMaterial3Bridge.kt` — the contract wins over the generic "extensions live in util" rule (single receiver per file is still honored).

**Steps:**

- [ ] **Step 1: Write `NxTheme.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

val LocalNxColors = staticCompositionLocalOf<NxColors> {
    error("NxColors not provided. Wrap content in NxTheme.")
}

val LocalNxType = staticCompositionLocalOf<NxType> {
    error("NxType not provided. Wrap content in NxTheme.")
}

object NxTokens {
    val colors: NxColors
        @Composable @ReadOnlyComposable get() = LocalNxColors.current
    val type: NxType
        @Composable @ReadOnlyComposable get() = LocalNxType.current
    val spacing = NxSpacing
    val radius = NxRadius
}

@Composable
fun NxTheme(
    palette: NxPalette = NxPalette.DefaultDark,
    content: @Composable () -> Unit,
) {
    val colors = palette.colors
    val type = nxTypeFor(rememberNxJetBrainsMono())
    CompositionLocalProvider(
        LocalNxColors provides colors,
        LocalNxType provides type,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial3ColorScheme(dark = palette.dark),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colors.bg,
            ) {
                content()
            }
        }
    }
}
```

- [ ] **Step 2: Write `NxMaterial3Bridge.kt`** (pawdex `PdMaterial3Bridge` ported; accent mapping: pawdex `accentSky` → `accentCyan` as secondary, `accentPlum` → `accentViolet` as tertiary):

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

private const val FIXED_DIM_ALPHA = 0.6f
private const val ERROR_CONTAINER_ALPHA = 0.18f

/**
 * Maps Nx tokens onto Material3 slots so stock M3 widgets (AlertDialog, Snackbar, ...)
 * visually match Nx components without consuming Nx tokens directly.
 */
fun NxColors.toMaterial3ColorScheme(dark: Boolean): ColorScheme {
    val primaryFixedDim = brand.copy(alpha = FIXED_DIM_ALPHA)
    val secondaryFixedDim = accentCyan.copy(alpha = FIXED_DIM_ALPHA)
    val tertiaryFixedDim = accentViolet.copy(alpha = FIXED_DIM_ALPHA)
    val errorContainer = danger.copy(alpha = ERROR_CONTAINER_ALPHA)

    return if (dark) darkColorScheme(
        primary = brand,
        onPrimary = brandFg,
        primaryContainer = brandSoft,
        onPrimaryContainer = fgOnBrand,
        primaryFixed = brand,
        primaryFixedDim = primaryFixedDim,
        onPrimaryFixed = brandFg,
        onPrimaryFixedVariant = fgOnBrand,
        inversePrimary = brand,
        secondary = accentCyan,
        onSecondary = fg,
        secondaryContainer = bgElev2,
        onSecondaryContainer = fg,
        secondaryFixed = accentCyan,
        secondaryFixedDim = secondaryFixedDim,
        onSecondaryFixed = fg,
        onSecondaryFixedVariant = fgMuted,
        tertiary = accentViolet,
        onTertiary = fg,
        tertiaryContainer = bgElev2,
        onTertiaryContainer = fg,
        tertiaryFixed = accentViolet,
        tertiaryFixedDim = tertiaryFixedDim,
        onTertiaryFixed = fg,
        onTertiaryFixedVariant = fgMuted,
        background = bg,
        onBackground = fg,
        surface = bgElev1,
        onSurface = fg,
        surfaceVariant = bgElev2,
        onSurfaceVariant = fgMuted,
        surfaceTint = brand,
        inverseSurface = bgInverse,
        inverseOnSurface = fgInverse,
        error = danger,
        onError = bg,
        errorContainer = errorContainer,
        onErrorContainer = danger,
        outline = border,
        outlineVariant = divider,
        scrim = bg,
        surfaceBright = bgElev2,
        surfaceDim = bgSunken,
        surfaceContainer = bgElev1,
        surfaceContainerHigh = bgElev2,
        surfaceContainerHighest = bgElev2,
        surfaceContainerLow = bg,
        surfaceContainerLowest = bgSunken,
    ) else lightColorScheme(
        primary = brand,
        onPrimary = brandFg,
        primaryContainer = brandSoft,
        onPrimaryContainer = fgOnBrand,
        primaryFixed = brand,
        primaryFixedDim = primaryFixedDim,
        onPrimaryFixed = brandFg,
        onPrimaryFixedVariant = fgOnBrand,
        inversePrimary = brand,
        secondary = accentCyan,
        onSecondary = fg,
        secondaryContainer = bgElev2,
        onSecondaryContainer = fg,
        secondaryFixed = accentCyan,
        secondaryFixedDim = secondaryFixedDim,
        onSecondaryFixed = fg,
        onSecondaryFixedVariant = fgMuted,
        tertiary = accentViolet,
        onTertiary = fg,
        tertiaryContainer = bgElev2,
        onTertiaryContainer = fg,
        tertiaryFixed = accentViolet,
        tertiaryFixedDim = tertiaryFixedDim,
        onTertiaryFixed = fg,
        onTertiaryFixedVariant = fgMuted,
        background = bg,
        onBackground = fg,
        surface = bgElev1,
        onSurface = fg,
        surfaceVariant = bgElev2,
        onSurfaceVariant = fgMuted,
        surfaceTint = brand,
        inverseSurface = bgInverse,
        inverseOnSurface = fgInverse,
        error = danger,
        onError = bgElev2,
        errorContainer = errorContainer,
        onErrorContainer = danger,
        outline = border,
        outlineVariant = divider,
        scrim = bg,
        surfaceBright = bgElev2,
        surfaceDim = bgSunken,
        surfaceContainer = bgElev1,
        surfaceContainerHigh = bgElev2,
        surfaceContainerHighest = bgElev2,
        surfaceContainerLow = bg,
        surfaceContainerLowest = bgSunken,
    )
}
```

- [ ] **Step 3: Write `AllThemePreview.kt`.** Annotation args must be compile-time `Long` constants — they cannot reference `NxColors` (Color is not a constant type). The `const val`s keep detekt clean (`ignoreConstantDeclaration` is a detekt default) and are the documented spec §8 exception:

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

import org.jetbrains.compose.ui.tooling.preview.Preview

// Documented exception (spec §8): @Preview backgroundColor requires compile-time Long
// constants; these mirror each palette's NxColors.bg and are locked by NxColorsTest.
private const val BG_UMBRA = 0xFF14121F
private const val BG_ECLIPSE = 0xFF000000
private const val BG_DUSK = 0xFF1D1430
private const val BG_MOONLIGHT = 0xFFF4F4F8
private const val BG_DAWN = 0xFFFAF5EC

@Preview(name = "umbra", group = "dark", showBackground = true, backgroundColor = BG_UMBRA)
@Preview(name = "eclipse", group = "dark", showBackground = true, backgroundColor = BG_ECLIPSE)
@Preview(name = "dusk", group = "dark", showBackground = true, backgroundColor = BG_DUSK)
@Preview(name = "moonlight", group = "light", showBackground = true, backgroundColor = BG_MOONLIGHT)
@Preview(name = "dawn", group = "light", showBackground = true, backgroundColor = BG_DAWN)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class AllThemePreview
```

- [ ] **Step 4: Write `NxPaletteProvider.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

class NxPaletteProvider : PreviewParameterProvider<NxPalette> {
    override val values: Sequence<NxPalette> = NxPalette.entries.asSequence()
}
```

- [ ] **Step 5: Verify compilation + rerun token tests.**

```bash
./gradlew :shared:design-library:compileKotlinJvm :shared:design-library:jvmTest
```

Expected: `BUILD SUCCESSFUL`, NxColorsTest still green.

- [ ] **Step 6: Commit.**

```bash
git add shared/design-library/src
git commit -m "feat(design-library): NxTheme, NxTokens, Material3 bridge, and preview infrastructure"
```

---

## Component tasks — shared pattern

Tasks 5-20 follow one strict shape (spelled out once here, executed identically in each):

1. Write `NxX.kt` (complete code given in the task).
2. Write `NxXPreview.kt` (complete code given in the task) — public `NxXSample()` + private `@AllThemePreview` fun.
3. Verify: `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
4. Commit: `git add shared/design-library/src && git commit -m "<message given in task>"`.

Dependency order matters: NxText (5) and NxIcon (6) come first because nearly everything composes them; templates come last because they compose buttons and icon buttons.

---

### Task 5: NxText

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxText.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxTextPreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxTextStyle`, `NxTheme` (Tasks 3-4).
- Produces: `@Composable fun NxText(text: String, modifier: Modifier = Modifier, style: NxTextStyle = NxTextStyle.Body, color: Color = Color.Unspecified, maxLines: Int = Int.MAX_VALUE, textAlign: TextAlign? = null)` and `@Composable fun NxTextSample()`.

**Steps:**

- [ ] **Step 1: Write `NxText.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

@Composable
fun NxText(
    text: String,
    modifier: Modifier = Modifier,
    style: NxTextStyle = NxTextStyle.Body,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    val colors = NxTokens.colors
    val type = NxTokens.type
    val resolvedColor = if (color == Color.Unspecified) colors.fg else color
    val textStyle = when (style) {
        NxTextStyle.Display -> type.display
        NxTextStyle.Title -> type.title
        NxTextStyle.Heading -> type.heading
        NxTextStyle.Subhead -> type.subhead
        NxTextStyle.Body -> type.body
        NxTextStyle.BodyStrong -> type.bodyStrong
        NxTextStyle.Caption -> type.caption
        NxTextStyle.Kicker -> type.kicker
        NxTextStyle.Mono -> type.mono
    }
    Text(
        text = if (style == NxTextStyle.Kicker) text.uppercase() else text,
        modifier = modifier,
        color = resolvedColor,
        style = textStyle,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}
```

- [ ] **Step 2: Write `NxTextPreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxTextSample() {
    val colors = NxTokens.colors
    Column(
        modifier = Modifier.padding(NxSpacing.s4),
        verticalArrangement = Arrangement.spacedBy(NxSpacing.s3),
    ) {
        NxText("Hidden in plain sight.", style = NxTextStyle.Display)
        NxText("Vault", style = NxTextStyle.Title)
        NxText("Encrypt a message", style = NxTextStyle.Heading)
        NxText("Top bar title", style = NxTextStyle.Subhead)
        NxText("Standard body text inside cards.", style = NxTextStyle.Body)
        NxText("Body strong — same size, w600.", style = NxTextStyle.BodyStrong)
        NxText("Subtle line under titles.", style = NxTextStyle.Caption, color = colors.fgMuted)
        NxText("section label", style = NxTextStyle.Kicker, color = colors.fgSubtle)
        NxText("12 images", style = NxTextStyle.Mono)
    }
}

@AllThemePreview
@Composable
private fun NxTextPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxTextSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxText atom with previews"`.

---

### Task 6: NxIconSet, NxIcon, NxIconButton (21 icons, PathParser approach)

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/tokens/NxIconKind.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxVectorBuilder.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxIconSet.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxIconButton.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxIconSetPreview.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxIconButtonPreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxTheme` (Task 4), `NxText` (Task 5, previews only).
- Produces (00-INDEX contract — exact enum values, exact order):
  - `enum class NxIconKind { Plus, ChevronLeft, ChevronRight, Eye, EyeOff, Lock, Unlock, Image, Camera, Share, Trash, Archive, Restore, Copy, Check, Close, Settings, Palette, Info, Warning, Vault }` in `tokens/NxIconKind.kt` (package `...designlibrary.tokens` — 00-INDEX pins this file/package; plans 05/06 import it from `tokens`)
  - `@Composable fun NxIcon(kind: NxIconKind, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current, contentDescription: String? = null, size: Dp = 20.dp)`
  - `enum class NxIconButtonStyle { Outline, Ghost, Filled }`
  - `@Composable fun NxIconButton(kind: NxIconKind, onClick: () -> Unit, modifier: Modifier = Modifier, style: NxIconButtonStyle = NxIconButtonStyle.Outline, contentDescription: String? = null)`

Icon provenance: generic glyphs (chevrons, plus, close, check, camera, archive, settings, info, warning) are ported verbatim from pawdex `PdIconSet` (Feather-derived 24x24 stroke paths). Nyx-specific glyphs (eye, eye-off, lock, unlock, image, share, trash, restore, copy) are Feather-style paths with circles/polylines pre-converted to path arcs/line commands. `Palette` and `Vault` are hand-authored in the same 24x24, 1.75-stroke style. All path strings below are final — no drawing decisions remain.

**Steps:**

- [ ] **Step 1: Write `NxIconKind.kt`** (the icon-kind token enum — 00-INDEX pins it to the `tokens/` package; every atom, molecule, and template references `NxIconKind` from `tokens`, as do plans 05/06):

```kotlin
package com.slothiesmooth.nyx.designlibrary.tokens

enum class NxIconKind {
    Plus, ChevronLeft, ChevronRight, Eye, EyeOff, Lock, Unlock, Image, Camera, Share,
    Trash, Archive, Restore, Copy, Check, Close, Settings, Palette, Info, Warning, Vault
}
```

- [ ] **Step 2: Write `NxVectorBuilder.kt`** (pawdex `PathParser.kt` ported — builds ImageVectors from SVG path data via `addPathNodes`; stroke color is a black template tinted by `Icon`):

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val VIEWPORT = 24f
private val IconCanvas = 24.dp
private const val STROKE_WIDTH = 1.75f

internal object NxVectorBuilder {

    private fun builder(name: String = "NxIcon") = ImageVector.Builder(
        name = name,
        defaultWidth = IconCanvas,
        defaultHeight = IconCanvas,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    )

    fun stroke(vararg paths: String): ImageVector {
        val vectorBuilder = builder()
        paths.forEach { pathData ->
            vectorBuilder.addPath(
                pathData = addPathNodes(pathData),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return vectorBuilder.build()
    }
}
```

- [ ] **Step 3: Write `NxIconSet.kt`** (complete — every path string final):

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind

object NxIconSet {

    val Plus: ImageVector = NxVectorBuilder.stroke(
        "M12 5v14M5 12h14",
    )

    val ChevronLeft: ImageVector = NxVectorBuilder.stroke(
        "M15 18l-6-6 6-6",
    )

    val ChevronRight: ImageVector = NxVectorBuilder.stroke(
        "M9 18l6-6-6-6",
    )

    // eye outline + pupil circle (cx 12, cy 12, r 3) as arc path
    val Eye: ImageVector = NxVectorBuilder.stroke(
        "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z",
        "M15 12A3 3 0 1 1 9 12A3 3 0 0 1 15 12z",
    )

    // eye-off: two lid arcs + slashed pupil + diagonal strike line
    val EyeOff: ImageVector = NxVectorBuilder.stroke(
        "M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94",
        "M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19",
        "M14.12 14.12a3 3 0 1 1-4.24-4.24",
        "M1 1L23 23",
    )

    // body rect (x 3, y 11, w 18, h 11, rx 2) + closed shackle
    val Lock: ImageVector = NxVectorBuilder.stroke(
        "M5 11H19A2 2 0 0 1 21 13V20A2 2 0 0 1 19 22H5A2 2 0 0 1 3 20V13A2 2 0 0 1 5 11z",
        "M7 11V7a5 5 0 0 1 10 0v4",
    )

    // same body rect + open shackle
    val Unlock: ImageVector = NxVectorBuilder.stroke(
        "M5 11H19A2 2 0 0 1 21 13V20A2 2 0 0 1 19 22H5A2 2 0 0 1 3 20V13A2 2 0 0 1 5 11z",
        "M7 11V7a5 5 0 0 1 9.9-1",
    )

    // frame rect (x 3, y 3, w 18, h 18, rx 2) + sun dot (cx 8.5, cy 8.5, r 1.5) + mountain polyline
    val Image: ImageVector = NxVectorBuilder.stroke(
        "M5 3H19A2 2 0 0 1 21 5V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V5A2 2 0 0 1 5 3z",
        "M10 8.5A1.5 1.5 0 1 1 7 8.5A1.5 1.5 0 0 1 10 8.5z",
        "M21 15L16 10L5 21",
    )

    // camera body + lens circle (cx 12, cy 13, r 4)
    val Camera: ImageVector = NxVectorBuilder.stroke(
        "M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z",
        "M16 13A4 4 0 1 1 8 13A4 4 0 0 1 16 13z",
    )

    // share-2: three nodes (r 3) + two connector lines
    val Share: ImageVector = NxVectorBuilder.stroke(
        "M21 5A3 3 0 1 1 15 5A3 3 0 0 1 21 5z",
        "M9 12A3 3 0 1 1 3 12A3 3 0 0 1 9 12z",
        "M21 19A3 3 0 1 1 15 19A3 3 0 0 1 21 19z",
        "M8.59 13.51L15.42 17.49",
        "M15.41 6.51L8.59 10.49",
    )

    // trash-2: lid line + can with handle + two content lines
    val Trash: ImageVector = NxVectorBuilder.stroke(
        "M3 6L5 6L21 6",
        "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2",
        "M10 11L10 17",
        "M14 11L14 17",
    )

    // archive box: lid rect (x 2, y 4, w 20, h 5, rx 2) + body + handle line
    val Archive: ImageVector = NxVectorBuilder.stroke(
        "M4 4H20A2 2 0 0 1 22 6V7A2 2 0 0 1 20 9H4A2 2 0 0 1 2 7V6A2 2 0 0 1 4 4z",
        "M4 9v9a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9",
        "M10 13L14 13",
    )

    // rotate-ccw: corner polyline + arc sweep (counterclockwise restore)
    val Restore: ImageVector = NxVectorBuilder.stroke(
        "M1 4L1 10L7 10",
        "M3.51 15a9 9 0 1 0 2.13-9.36L1 10",
    )

    // copy: front rect (x 9, y 9, w 13, h 13, rx 2) + back sheet
    val Copy: ImageVector = NxVectorBuilder.stroke(
        "M11 9H20A2 2 0 0 1 22 11V20A2 2 0 0 1 20 22H11A2 2 0 0 1 9 20V11A2 2 0 0 1 11 9z",
        "M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1",
    )

    val Check: ImageVector = NxVectorBuilder.stroke(
        "M20 6L9 17l-5-5",
    )

    val Close: ImageVector = NxVectorBuilder.stroke(
        "M18 6L6 18M6 6l12 12",
    )

    // gear: hub circle (cx 12, cy 12, r 3) + tooth ring
    val Settings: ImageVector = NxVectorBuilder.stroke(
        "M15 12A3 3 0 1 1 9 12A3 3 0 0 1 15 12z",
        "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33h0a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51h0a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82v0a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z",
    )

    // painter's palette: disk with thumb notch + four paint wells (r 1)
    val Palette: ImageVector = NxVectorBuilder.stroke(
        "M12 21a9 9 0 1 1 9-9c0 1.66-1.34 3-3 3h-2.5a2 2 0 0 0-2 2c0 .5.2 1 .5 1.4.3.4.5.8.5 1.1a1.5 1.5 0 0 1-1.5 1.5Z",
        "M7.5 11.5A1 1 0 1 1 5.5 11.5A1 1 0 0 1 7.5 11.5z",
        "M10 7.5A1 1 0 1 1 8 7.5A1 1 0 0 1 10 7.5z",
        "M14.5 6.5A1 1 0 1 1 12.5 6.5A1 1 0 0 1 14.5 6.5z",
        "M18.5 10.5A1 1 0 1 1 16.5 10.5A1 1 0 0 1 18.5 10.5z",
    )

    // info: ring (r 10) + stem + dot
    val Info: ImageVector = NxVectorBuilder.stroke(
        "M22 12A10 10 0 1 1 2 12A10 10 0 0 1 22 12z",
        "M12 16L12 12",
        "M12 8L12.01 8",
    )

    // alert-triangle: outline + exclamation stem + dot
    val Warning: ImageVector = NxVectorBuilder.stroke(
        "M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z",
        "M12 9L12 13",
        "M12 17L12.01 17",
    )

    // safe: door rect (x 3, y 3, w 18, h 18, rx 2) + dial (r 4) + dial handle + two feet
    val Vault: ImageVector = NxVectorBuilder.stroke(
        "M5 3H19A2 2 0 0 1 21 5V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V5A2 2 0 0 1 5 3z",
        "M16 12A4 4 0 1 1 8 12A4 4 0 0 1 16 12z",
        "M12 12L14.5 9.5",
        "M7 21L7 23",
        "M17 21L17 23",
    )
}

private fun NxIconKind.vector(): ImageVector = when (this) {
    NxIconKind.Plus -> NxIconSet.Plus
    NxIconKind.ChevronLeft -> NxIconSet.ChevronLeft
    NxIconKind.ChevronRight -> NxIconSet.ChevronRight
    NxIconKind.Eye -> NxIconSet.Eye
    NxIconKind.EyeOff -> NxIconSet.EyeOff
    NxIconKind.Lock -> NxIconSet.Lock
    NxIconKind.Unlock -> NxIconSet.Unlock
    NxIconKind.Image -> NxIconSet.Image
    NxIconKind.Camera -> NxIconSet.Camera
    NxIconKind.Share -> NxIconSet.Share
    NxIconKind.Trash -> NxIconSet.Trash
    NxIconKind.Archive -> NxIconSet.Archive
    NxIconKind.Restore -> NxIconSet.Restore
    NxIconKind.Copy -> NxIconSet.Copy
    NxIconKind.Check -> NxIconSet.Check
    NxIconKind.Close -> NxIconSet.Close
    NxIconKind.Settings -> NxIconSet.Settings
    NxIconKind.Palette -> NxIconSet.Palette
    NxIconKind.Info -> NxIconSet.Info
    NxIconKind.Warning -> NxIconSet.Warning
    NxIconKind.Vault -> NxIconSet.Vault
}

private val IconSizeDefault = 20.dp

@Composable
fun NxIcon(
    kind: NxIconKind,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
    size: Dp = IconSizeDefault,
) {
    Icon(
        imageVector = kind.vector(),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
```

- [ ] **Step 4: Write `NxIconButton.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

enum class NxIconButtonStyle { Outline, Ghost, Filled }

private val IconButtonSize = 36.dp
private val IconButtonIconSize = 20.dp
private val IconButtonBorderWidth = 1.dp

@Composable
fun NxIconButton(
    kind: NxIconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NxIconButtonStyle = NxIconButtonStyle.Outline,
    contentDescription: String? = null,
) {
    val colors = NxTokens.colors
    val background = if (style == NxIconButtonStyle.Filled) colors.brand else Color.Transparent
    val tint = if (style == NxIconButtonStyle.Filled) colors.brandFg else colors.fg
    val border =
        if (style == NxIconButtonStyle.Outline) BorderStroke(IconButtonBorderWidth, colors.border) else null
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        border = border,
        modifier = modifier.size(IconButtonSize),
    ) {
        NxIcon(
            kind = kind,
            tint = tint,
            size = IconButtonIconSize,
            contentDescription = contentDescription,
            modifier = Modifier.size(IconButtonSize),
        )
    }
}
```

- [ ] **Step 5: Write `NxIconSetPreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

private const val ICONS_PER_ROW = 6

@Composable
fun NxIconSetSample() {
    val colors = NxTokens.colors
    val rows = NxIconKind.entries.chunked(ICONS_PER_ROW)
    Column(
        modifier = Modifier.padding(NxSpacing.s4),
        verticalArrangement = Arrangement.spacedBy(NxSpacing.s3),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(NxSpacing.s3)) {
                row.forEach { kind ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NxIcon(kind = kind, tint = colors.fg)
                        NxText(text = kind.name, style = NxTextStyle.Caption, color = colors.fgMuted)
                    }
                }
            }
        }
    }
}

@AllThemePreview
@Composable
private fun NxIconSetPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxIconSetSample() }
}
```

Add the missing import line to the file above: `import org.jetbrains.compose.ui.tooling.preview.PreviewParameter` (alphabetical position: after the `com.slothiesmooth...` imports).

- [ ] **Step 6: Write `NxIconButtonPreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxIconButtonSample() {
    Row(
        modifier = Modifier.padding(NxSpacing.s4),
        horizontalArrangement = Arrangement.spacedBy(NxSpacing.s3),
    ) {
        NxIconButton(kind = NxIconKind.Plus, onClick = {}, style = NxIconButtonStyle.Outline)
        NxIconButton(kind = NxIconKind.Share, onClick = {}, style = NxIconButtonStyle.Ghost)
        NxIconButton(kind = NxIconKind.Camera, onClick = {}, style = NxIconButtonStyle.Filled)
        NxIconButton(kind = NxIconKind.Trash, onClick = {}, style = NxIconButtonStyle.Outline)
    }
}

@AllThemePreview
@Composable
private fun NxIconButtonPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxIconButtonSample() }
}
```

- [ ] **Step 7: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 8: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): 21-icon NxIconSet with NxIcon and NxIconButton"`.

---

### Task 7: NxButton

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxButton.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxButtonPreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxRadius`, `NxSpacing` (Task 3-4), `NxIcon`/`NxIconKind` (Task 6).
- Produces (00-INDEX contract): `enum class NxButtonStyle { Primary, Soft, Ghost, Danger }`, `enum class NxButtonSize { Regular, Small }`, `@Composable fun NxButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, style: NxButtonStyle = NxButtonStyle.Primary, size: NxButtonSize = NxButtonSize.Regular, block: Boolean = false, leadingIcon: NxIconKind? = null, enabled: Boolean = true)`.

Design note: pawdex used `Color.White` for Danger text; Nyx uses `colors.fgOnBrand` — token-pure, and `fgOnBrand/danger` contrast is asserted ≥ 4.5:1 in NxColorsTest for all palettes.

**Steps:**

- [ ] **Step 1: Write `NxButton.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxRadius
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

enum class NxButtonStyle { Primary, Soft, Ghost, Danger }
enum class NxButtonSize { Regular, Small }

private val ButtonHeightRegular = 48.dp
private val ButtonHeightSmall = 36.dp
private val ButtonPadRegular = 20.dp
private val ButtonPadSmall = 14.dp
private val ButtonTextSizeSmall = 13.sp
private val ButtonIconSize = 16.dp
private val GhostBorderWidth = 1.dp
private const val DISABLED_ALPHA = 0.4f

@Composable
fun NxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NxButtonStyle = NxButtonStyle.Primary,
    size: NxButtonSize = NxButtonSize.Regular,
    block: Boolean = false,
    leadingIcon: NxIconKind? = null,
    enabled: Boolean = true,
) {
    val colors = NxTokens.colors
    val type = NxTokens.type
    val background = when (style) {
        NxButtonStyle.Primary -> colors.brand
        NxButtonStyle.Soft -> colors.brandSoft
        NxButtonStyle.Ghost -> Color.Transparent
        NxButtonStyle.Danger -> colors.danger
    }
    val foreground = when (style) {
        NxButtonStyle.Primary -> colors.brandFg
        NxButtonStyle.Soft -> colors.brand
        NxButtonStyle.Ghost -> colors.fg
        NxButtonStyle.Danger -> colors.fgOnBrand
    }
    val border =
        if (style == NxButtonStyle.Ghost) BorderStroke(GhostBorderWidth, colors.borderStrong) else null
    val height = if (size == NxButtonSize.Regular) ButtonHeightRegular else ButtonHeightSmall
    val horizontalPad = if (size == NxButtonSize.Regular) ButtonPadRegular else ButtonPadSmall
    val textStyle = if (size == NxButtonSize.Regular) {
        type.bodyStrong
    } else {
        type.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = ButtonTextSizeSmall)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(NxRadius.sm),
        color = background,
        contentColor = foreground,
        border = border,
        modifier = modifier
            .let { if (block) it.fillMaxWidth() else it }
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        Row(
            modifier = Modifier.height(height).padding(horizontal = horizontalPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NxSpacing.s2, Alignment.CenterHorizontally),
        ) {
            if (leadingIcon != null) {
                NxIcon(kind = leadingIcon, tint = foreground, size = ButtonIconSize)
            }
            Text(text = text, style = textStyle, color = foreground)
        }
    }
}
```

- [ ] **Step 2: Write `NxButtonPreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

private val SampleWidth = 360.dp

@Composable
fun NxButtonSample() {
    Column(
        modifier = Modifier.padding(NxSpacing.s4).width(SampleWidth),
        verticalArrangement = Arrangement.spacedBy(NxSpacing.s3),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(NxSpacing.s2)) {
            NxButton(text = "Primary", onClick = {}, style = NxButtonStyle.Primary)
            NxButton(text = "Soft", onClick = {}, style = NxButtonStyle.Soft)
            NxButton(text = "Ghost", onClick = {}, style = NxButtonStyle.Ghost)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NxSpacing.s2)) {
            NxButton(text = "Small", onClick = {}, style = NxButtonStyle.Primary, size = NxButtonSize.Small)
            NxButton(text = "Small soft", onClick = {}, style = NxButtonStyle.Soft, size = NxButtonSize.Small)
            NxButton(text = "Small ghost", onClick = {}, style = NxButtonStyle.Ghost, size = NxButtonSize.Small)
        }
        NxButton(text = "Encrypt message", onClick = {}, style = NxButtonStyle.Primary, block = true, leadingIcon = NxIconKind.Lock)
        NxButton(text = "Wipe vault", onClick = {}, style = NxButtonStyle.Danger, leadingIcon = NxIconKind.Trash)
        NxButton(text = "Disabled", onClick = {}, style = NxButtonStyle.Primary, enabled = false)
    }
}

@AllThemePreview
@Composable
private fun NxButtonPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxButtonSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxButton with four styles, two sizes, and previews"`.

---

### Task 8: NxField + NxPasswordField

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxField.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxPasswordField.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxFieldPreview.kt`

**Interfaces:**
- Consumes: `NxTokens` (Task 4), `NxIconButton`/`NxIconKind` (Task 6).
- Produces:
  - `@Composable fun NxField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, placeholder: String = "", multiline: Boolean = false, enabled: Boolean = true)`
  - `@Composable fun NxPasswordField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, placeholder: String = "", enabled: Boolean = true)` — visibility toggle is view-local UI state (not business logic; allowed).

**Steps:**

- [ ] **Step 1: Write `NxField.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

private val FieldTextSize = 15.sp
private val MultilineMinHeight = 80.dp
private const val MULTILINE_MIN_LINES = 3

@Composable
fun NxField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    multiline: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = NxTokens.colors
    val type = NxTokens.type
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label.uppercase(), style = type.kicker, color = colors.fgSubtle) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, style = type.body, color = colors.fgFaint) }
        } else {
            null
        },
        singleLine = !multiline,
        minLines = if (multiline) MULTILINE_MIN_LINES else 1,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.brand,
            unfocusedBorderColor = colors.borderStrong,
            focusedTextColor = colors.fg,
            unfocusedTextColor = colors.fg,
            focusedLabelColor = colors.brand,
            unfocusedLabelColor = colors.fgSubtle,
            cursorColor = colors.brand,
            focusedContainerColor = colors.bgElev1,
            unfocusedContainerColor = colors.bgElev1,
        ),
        textStyle = type.body.copy(fontSize = FieldTextSize),
        modifier = if (multiline) modifier.heightIn(min = MultilineMinHeight) else modifier,
    )
}
```

- [ ] **Step 2: Write `NxPasswordField.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

private val PasswordTextSize = 15.sp

@Composable
fun NxPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
    val colors = NxTokens.colors
    val type = NxTokens.type
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label.uppercase(), style = type.kicker, color = colors.fgSubtle) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, style = type.body, color = colors.fgFaint) }
        } else {
            null
        },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            NxIconButton(
                kind = if (visible) NxIconKind.EyeOff else NxIconKind.Eye,
                onClick = { visible = !visible },
                style = NxIconButtonStyle.Ghost,
                contentDescription = if (visible) "Hide password" else "Show password",
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.brand,
            unfocusedBorderColor = colors.borderStrong,
            focusedTextColor = colors.fg,
            unfocusedTextColor = colors.fg,
            focusedLabelColor = colors.brand,
            unfocusedLabelColor = colors.fgSubtle,
            cursorColor = colors.brand,
            focusedContainerColor = colors.bgElev1,
            unfocusedContainerColor = colors.bgElev1,
        ),
        textStyle = type.body.copy(fontSize = PasswordTextSize),
        modifier = modifier,
    )
}
```

- [ ] **Step 3: Write `NxFieldPreview.kt`** (covers both field kinds and all their states — the shared sample keeps one snapshot class for the pair):

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

private val SampleWidth = 360.dp

@Composable
fun NxFieldSample() {
    Column(
        modifier = Modifier.padding(NxSpacing.s4).width(SampleWidth),
        verticalArrangement = Arrangement.spacedBy(NxSpacing.s3),
    ) {
        NxField(value = "vacation-2026.png", onValueChange = {}, label = "Image name", modifier = Modifier.fillMaxWidth())
        NxField(value = "", onValueChange = {}, label = "Message", placeholder = "The secret to hide", multiline = true, modifier = Modifier.fillMaxWidth())
        NxField(value = "read only", onValueChange = {}, label = "Disabled", enabled = false, modifier = Modifier.fillMaxWidth())
        NxPasswordField(value = "hunter2hunter2", onValueChange = {}, label = "Password", modifier = Modifier.fillMaxWidth())
        NxPasswordField(value = "", onValueChange = {}, label = "Confirm password", placeholder = "Repeat it exactly", modifier = Modifier.fillMaxWidth())
    }
}

@AllThemePreview
@Composable
private fun NxFieldPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxFieldSample() }
}
```

- [ ] **Step 4: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 5: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxField and NxPasswordField with visibility toggle"`.

---

### Task 9: NxChip

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxChip.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/atoms/NxChipPreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxRadius`, `NxSpacing` (Tasks 3-4).
- Produces: `@Composable fun NxChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)`.

**Steps:**

- [ ] **Step 1: Write `NxChip.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.tokens.NxRadius
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

private val ChipHorizontalPadding = 14.dp
private val ChipTextSize = 13.sp
private val ChipBorderWidth = 1.dp

@Composable
fun NxChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NxTokens.colors
    val type = NxTokens.type
    val background = if (selected) colors.brand else Color.Transparent
    val foreground = if (selected) colors.brandFg else colors.fg
    val border = if (selected) null else BorderStroke(ChipBorderWidth, colors.borderStrong)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NxRadius.pill),
        color = background,
        border = border,
        modifier = modifier,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = ChipHorizontalPadding, vertical = NxSpacing.s2),
            color = foreground,
            style = type.caption.copy(fontWeight = FontWeight.Medium, fontSize = ChipTextSize),
        )
    }
}
```

- [ ] **Step 2: Write `NxChipPreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxChipSample() {
    Row(
        modifier = Modifier.padding(NxSpacing.s4),
        horizontalArrangement = Arrangement.spacedBy(NxSpacing.s2),
    ) {
        NxChip(text = "Active", selected = true, onClick = {})
        NxChip(text = "Archived", selected = false, onClick = {})
        NxChip(text = "All", selected = false, onClick = {})
    }
}

@AllThemePreview
@Composable
private fun NxChipPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxChipSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxChip with selected and unselected states"`.

---

### Task 10: NxCard

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxCard.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxCardPreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxRadius`, `NxShadow`, `NxSpacing` (Tasks 3-4), `NxText` (Task 5, preview only).
- Produces (00-INDEX contract): `enum class NxCardVariant { Elevated, Flat }`, `@Composable fun NxCard(modifier: Modifier = Modifier, variant: NxCardVariant = NxCardVariant.Elevated, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)`.

**Steps:**

- [ ] **Step 1: Write `NxCard.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.NxRadius
import com.slothiesmooth.nyx.designlibrary.tokens.NxShadow
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

enum class NxCardVariant { Elevated, Flat }

private val CardBorderWidth = 1.dp

@Composable
fun NxCard(
    modifier: Modifier = Modifier,
    variant: NxCardVariant = NxCardVariant.Elevated,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = NxTokens.colors
    val shape = RoundedCornerShape(NxRadius.lg)
    val background = when (variant) {
        NxCardVariant.Elevated -> colors.bgElev1
        NxCardVariant.Flat -> Color.Transparent
    }
    val border = when (variant) {
        NxCardVariant.Elevated -> BorderStroke(CardBorderWidth, colors.border)
        NxCardVariant.Flat -> BorderStroke(CardBorderWidth, colors.divider)
    }
    val baseModifier = when (variant) {
        NxCardVariant.Elevated -> modifier.then(NxShadow.xs(shape))
        NxCardVariant.Flat -> modifier
    }

    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = background, border = border, modifier = baseModifier) {
            Column(Modifier.padding(NxSpacing.s4)) { content() }
        }
    } else {
        Surface(shape = shape, color = background, border = border, modifier = baseModifier) {
            Column(Modifier.padding(NxSpacing.s4)) { content() }
        }
    }
}
```

- [ ] **Step 2: Write `NxCardPreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

private val SampleWidth = 280.dp

@Composable
fun NxCardSample() {
    val colors = NxTokens.colors
    Column(
        modifier = Modifier.padding(NxSpacing.s4).width(SampleWidth),
        verticalArrangement = Arrangement.spacedBy(NxSpacing.s3),
    ) {
        NxCard(variant = NxCardVariant.Elevated) {
            NxText("Elevated card", style = NxTextStyle.BodyStrong)
            NxText("Border, shadow, elevated surface", style = NxTextStyle.Caption, color = colors.fgSubtle)
        }
        NxCard(variant = NxCardVariant.Flat) {
            NxText("Flat card", style = NxTextStyle.BodyStrong)
            NxText("Divider border, no shadow", style = NxTextStyle.Caption, color = colors.fgSubtle)
        }
        NxCard(variant = NxCardVariant.Elevated, onClick = {}) {
            NxText("Clickable card", style = NxTextStyle.BodyStrong)
            NxText("Whole surface is a tap target", style = NxTextStyle.Caption, color = colors.fgSubtle)
        }
    }
}

@AllThemePreview
@Composable
private fun NxCardPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxCardSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxCard with elevated and flat variants"`.

---

### Task 11: NxTopBar

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxTopBar.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxTopBarPreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxSpacing`, `NxTextStyle` (Tasks 3-4), `NxText` (Task 5), `NxIconButton`/`NxIconButtonStyle`/`NxIconKind` (Task 6).
- Produces (pinned by plan 06 §"Pinned molecule/template signatures"): `@Composable fun NxTopBar(title: String, modifier: Modifier = Modifier, subtitle: String? = null, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {})`.

Anatomy note: ported from Baro `BaroTopBar` (back affordance → title → trailing actions row). Nyx drops Baro's theme/scale/glance controllers (those are app-level in Nyx, injected by feature screens as `trailing` content) and adds the pinned `subtitle` line under the title. The back affordance uses `NxIconKind.ChevronLeft` (the icon set has no dedicated arrow; ChevronLeft is the back glyph).

**Steps:**

- [ ] **Step 1: Write `NxTopBar.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

/**
 * A screen top bar: an optional [onBack] back affordance, a [title] with an optional [subtitle]
 * underneath, and a right-aligned [trailing] actions slot. Stateless — every affordance is supplied
 * by the caller.
 */
@Composable
fun NxTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = NxTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NxSpacing.s4, vertical = NxSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NxSpacing.s2),
    ) {
        if (onBack != null) {
            NxIconButton(
                kind = NxIconKind.ChevronLeft,
                onClick = onBack,
                style = NxIconButtonStyle.Ghost,
                contentDescription = "Back",
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            NxText(text = title, style = NxTextStyle.Title, color = colors.fg, maxLines = 1)
            if (subtitle != null) {
                NxText(text = subtitle, style = NxTextStyle.Caption, color = colors.fgMuted, maxLines = 1)
            }
        }
        trailing()
    }
}
```

- [ ] **Step 2: Write `NxTopBarPreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButtonStyle
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxTopBarSample() {
    Column(verticalArrangement = Arrangement.spacedBy(NxSpacing.s4)) {
        NxTopBar(title = "Vault")
        NxTopBar(
            title = "Encrypt",
            subtitle = "Step 2 of 3",
            onBack = {},
            trailing = {
                NxIconButton(
                    kind = NxIconKind.Share,
                    onClick = {},
                    style = NxIconButtonStyle.Ghost,
                    contentDescription = "Share",
                )
            },
        )
    }
}

@AllThemePreview
@Composable
private fun NxTopBarPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxTopBarSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxTopBar with back, subtitle, and trailing actions"`.

---

### Task 12: NxBottomNav (+ NxBottomNavItem)

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/models/NxBottomNavItem.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/organisms/NxBottomNav.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/organisms/NxBottomNavPreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxSpacing`, `NxTextStyle` (Tasks 3-4), `NxIcon`/`NxIconKind` (Task 6), `NxText` (Task 5), `kotlinx.collections.immutable.ImmutableList`.
- Produces (pinned by plan 05 Task 3):
  - `@Immutable data class NxBottomNavItem(val icon: NxIconKind, val label: String)` in `models/`
  - `@Composable fun NxBottomNav(items: ImmutableList<NxBottomNavItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)` in `organisms/`

> **Reconciliation with plan 05:** plan 05 Task 3 (Steps 1-3) was authored to build these three files itself, on the assumption plan 04 stopped at `NxCard`. They are delivered here instead. The code below is byte-identical to plan 05 Task 3 Steps 1-3, so if both plans run in sequence plan 05's re-creation is a harmless identical overwrite — but the intent is that **plan 05 Task 3 skips NxBottomNav (built here)** and only reconciles NxEmptyState (see Task 16 note). `NxBottomNavItem` (the design-library item type) is distinct from `feature.navigation.api.NavItem`; the navigation feature maps `NavItem → NxBottomNavItem` (plan 05 Task 8), keeping the design library free of any feature dependency. Anatomy ported from Baro `BaroBottomNav`, swapping the canvas-drawn glyphs for `NxIcon`.

**Steps:**

- [ ] **Step 1: Write `NxBottomNavItem.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.models

import androidx.compose.runtime.Immutable
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind

/** One bottom-navigation destination: an [icon] over its [label]. Selection lives in the caller. */
@Immutable
data class NxBottomNavItem(
    val icon: NxIconKind,
    val label: String,
)
```

> Note: `NxIconKind` lives in `tokens/NxIconKind.kt` (package `...designlibrary.tokens`), created in Task 6 Step 1 — 00-INDEX pins it to the tokens section, and plans 05/06 import it from there. This `...designlibrary.tokens.NxIconKind` import is authoritative; every atom, molecule, template, and preview in this plan references `NxIconKind` from `tokens`.

- [ ] **Step 2: Write `NxBottomNav.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.models.NxBottomNavItem
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import kotlinx.collections.immutable.ImmutableList

private val BarHeight: Dp = 64.dp
private val NavIconSize: Dp = 22.dp
private val HairlineWidth: Dp = 1.dp
private val SelectedStripeWidth: Dp = 2.dp

/**
 * The bottom navigation bar: full-width, evenly-weighted [items], each an [NxIcon] over its label.
 * The item at [selectedIndex] is tinted `brand` and carries a top accent stripe; tapping fires
 * [onSelect]. Stateless — selection and routing live in the caller. Fills under the system gesture bar.
 */
@Composable
fun NxBottomNav(
    items: ImmutableList<NxBottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NxTokens.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgElev1)
            .drawBehind {
                drawLine(
                    color = colors.divider,
                    start = Offset(x = 0f, y = 0f),
                    end = Offset(x = size.width, y = 0f),
                    strokeWidth = HairlineWidth.toPx(),
                )
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(BarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                NxBottomNavCell(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun NxBottomNavCell(
    item: NxBottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NxTokens.colors
    val tint = if (selected) colors.brand else colors.fgMuted
    val interactionSource = remember { MutableInteractionSource() }
    val stripe = if (selected) {
        Modifier.drawBehind {
            drawLine(
                color = colors.brand,
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = size.width, y = 0f),
                strokeWidth = SelectedStripeWidth.toPx(),
            )
        }
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick)
            .then(stripe)
            .padding(vertical = NxSpacing.s2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NxSpacing.s1),
    ) {
        NxIcon(kind = item.icon, tint = tint, contentDescription = item.label, size = NavIconSize)
        NxText(text = item.label, style = NxTextStyle.Kicker, color = tint, maxLines = 1)
    }
}
```

- [ ] **Step 3: Write `NxBottomNavPreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.organisms

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.designlibrary.models.NxBottomNavItem
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxBottomNavSample() {
    NxBottomNav(
        items = persistentListOf(
            NxBottomNavItem(NxIconKind.Vault, "Vault"),
            NxBottomNavItem(NxIconKind.Lock, "Encrypt"),
            NxBottomNavItem(NxIconKind.Unlock, "Decrypt"),
            NxBottomNavItem(NxIconKind.Settings, "Settings"),
        ),
        selectedIndex = 0,
        onSelect = {},
    )
}

@AllThemePreview
@Composable
private fun NxBottomNavPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxBottomNavSample() }
}
```

> The preview import above uses `...tokens.NxIconKind` — the canonical location (created in Task 6 Step 1, per the 00-INDEX tokens section). It is byte-identical to plan 05's import; no change needed.

- [ ] **Step 4: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 5: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxBottomNav organism with NxBottomNavItem"`.

---

### Task 13: NxFab

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxFab.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxFabPreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxShadow` (Tasks 3-4), `NxIcon`/`NxIconKind` (Task 6).
- Produces (pinned by plan 06): `@Composable fun NxFab(icon: NxIconKind, onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String? = null)`.

**Steps:**

- [ ] **Step 1: Write `NxFab.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxShadow
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

private val FabSize: Dp = 56.dp
private val FabIconSize: Dp = 24.dp

/** A circular floating action button: a brand-filled disk carrying a single [icon]. */
@Composable
fun NxFab(
    icon: NxIconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = NxTokens.colors
    val shape = CircleShape
    Surface(
        onClick = onClick,
        shape = shape,
        color = colors.brand,
        contentColor = colors.brandFg,
        modifier = modifier.then(NxShadow.md(shape)).size(FabSize),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NxIcon(kind = icon, tint = colors.brandFg, size = FabIconSize, contentDescription = contentDescription)
        }
    }
}
```

- [ ] **Step 2: Write `NxFabPreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxFabSample() {
    Row(
        modifier = Modifier.padding(NxSpacing.s4),
        horizontalArrangement = Arrangement.spacedBy(NxSpacing.s3),
    ) {
        NxFab(icon = NxIconKind.Plus, onClick = {}, contentDescription = "Encrypt a message")
        NxFab(icon = NxIconKind.Camera, onClick = {}, contentDescription = "Capture")
    }
}

@AllThemePreview
@Composable
private fun NxFabPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxFabSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxFab floating action button"`.

---

### Task 14: NxImageTile

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxImageTile.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxImageTilePreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxRadius` (Tasks 3-4), `NxIcon`/`NxIconKind` (Task 6).
- Produces (pinned by plan 06): `@Composable fun NxImageTile(image: ImageBitmap?, modifier: Modifier = Modifier, selected: Boolean = false, contentDescription: String? = null, onClick: (() -> Unit)? = null)`.

Contract note (plan 06 §): the tile takes an **already-decoded `ImageBitmap?`**, never raw bytes — decoding is logic and lives in the VM, so the tile stays dumb. When `image` is null it renders a placeholder glyph. The tile is intrinsically square (`aspectRatio(1f)` baked in) so it drops straight into a fixed-column grid cell; callers that already pass `Modifier.aspectRatio(1f)` are unaffected (identical ratio).

**Steps:**

- [ ] **Step 1: Write `NxImageTile.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxRadius
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

private const val TileAspectRatio = 1f
private val TileBorderWidth: Dp = 2.dp
private val TilePlaceholderIconSize: Dp = 32.dp

/**
 * A square image tile rendering an already-decoded [image] (or a placeholder glyph when null),
 * clipped to a rounded shape. A [selected] tile gains a brand-tinted border; a non-null [onClick]
 * makes the whole tile a tap target.
 */
@Composable
fun NxImageTile(
    image: ImageBitmap?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = NxTokens.colors
    val shape = RoundedCornerShape(NxRadius.md)
    val border = if (selected) BorderStroke(TileBorderWidth, colors.brand) else null
    val tileModifier = modifier.aspectRatio(TileAspectRatio)
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = colors.bgElev2, border = border, modifier = tileModifier) {
            NxImageTileContent(image = image, contentDescription = contentDescription, placeholderTint = colors.fgFaint)
        }
    } else {
        Surface(shape = shape, color = colors.bgElev2, border = border, modifier = tileModifier) {
            NxImageTileContent(image = image, contentDescription = contentDescription, placeholderTint = colors.fgFaint)
        }
    }
}

@Composable
private fun NxImageTileContent(
    image: ImageBitmap?,
    contentDescription: String?,
    placeholderTint: Color,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            NxIcon(
                kind = NxIconKind.Image,
                tint = placeholderTint,
                size = TilePlaceholderIconSize,
                contentDescription = contentDescription,
            )
        }
    }
}
```

- [ ] **Step 2: Write `NxImageTilePreview.kt`** (a null `image` on both tiles exercises the placeholder path and needs no bundled bitmap — the golden captures placeholder + selection states):

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

private val TilePreviewWidth: Dp = 120.dp

@Composable
fun NxImageTileSample() {
    Row(
        modifier = Modifier.padding(NxSpacing.s4),
        horizontalArrangement = Arrangement.spacedBy(NxSpacing.s3),
    ) {
        NxImageTile(image = null, selected = true, contentDescription = "Selected cover", onClick = {}, modifier = Modifier.width(TilePreviewWidth))
        NxImageTile(image = null, selected = false, contentDescription = "Cover image", modifier = Modifier.width(TilePreviewWidth))
    }
}

@AllThemePreview
@Composable
private fun NxImageTilePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxImageTileSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxImageTile with placeholder and selection states"`.

---

### Task 15: NxSectionHeader

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxSectionHeader.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxSectionHeaderPreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxSpacing`, `NxTextStyle` (Tasks 3-4), `NxText` (Task 5).
- Produces (pinned by plan 06): `@Composable fun NxSectionHeader(title: String, modifier: Modifier = Modifier, actionText: String? = null, onAction: (() -> Unit)? = null)`.

Anatomy note: ported from Baro `BaroSectionHeader` — a weighted title with an optional right-aligned action. Baro's `eyebrow` kicker is dropped (not in the pinned signature); the trailing slot becomes a pinned `actionText`/`onAction` pair rendered as a brand-tinted tappable label.

**Steps:**

- [ ] **Step 1: Write `NxSectionHeader.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

/**
 * A list/section header: a weighted [title] with an optional right-aligned action rendered as a
 * brand-tinted tappable label ([actionText] + [onAction], e.g. "See all").
 */
@Composable
fun NxSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = NxTokens.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NxSpacing.s2),
    ) {
        NxText(text = title, style = NxTextStyle.Subhead, color = colors.fg, modifier = Modifier.weight(1f))
        if (actionText != null && onAction != null) {
            NxText(
                text = actionText,
                style = NxTextStyle.BodyStrong,
                color = colors.brand,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}
```

- [ ] **Step 2: Write `NxSectionHeaderPreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxSectionHeaderSample() {
    Column(
        modifier = Modifier.padding(NxSpacing.s4),
        verticalArrangement = Arrangement.spacedBy(NxSpacing.s3),
    ) {
        NxSectionHeader(title = "Archived")
        NxSectionHeader(title = "Recent", actionText = "See all", onAction = {})
    }
}

@AllThemePreview
@Composable
private fun NxSectionHeaderPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxSectionHeaderSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxSectionHeader with optional action"`.

---

### Task 16: NxEmptyState

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxEmptyState.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxEmptyStatePreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxSpacing`, `NxTextStyle` (Tasks 3-4), `NxText` (Task 5), `NxIcon`/`NxIconKind` (Task 6), `NxButton` (Task 7).
- Produces (pinned by plan 06 §"Pinned molecule/template signatures", which mandates "plan 04 Tasks 11-20 must expose exactly these"): `@Composable fun NxEmptyState(icon: NxIconKind, title: String, body: String, modifier: Modifier = Modifier, ctaText: String? = null, onCta: (() -> Unit)? = null)`.

> **Reconciliation with plan 05 (parameter-name conflict — must resolve):** plan 05 Task 3 also builds `NxEmptyState`, but with the CTA parameter named **`ctaLabel`**; plan 06 pins it as **`ctaText`**. A single Kotlin parameter cannot answer to both named-argument call sites. This plan takes plan 06's name (`ctaText`) because plan 06 explicitly declares plan 04 Tasks 11-20 as the source of truth for these signatures. Consequence: **plan 05 Task 3 Steps 4-5 (NxEmptyState) are superseded by this task** (skip them), and plan 05's one CTA call site — `BasicSettingsProvider` (plan 05 Task 9 Step 4), `NxEmptyState(... ctaLabel = "Change theme" ...)` — must be updated to `ctaText = "Change theme"`. Every other plan-05 `NxEmptyState` call passes only `icon`/`title`/`body` and is unaffected. Naming is also internally consistent this way: `ctaText`/`onCta` mirrors `NxSectionHeader`'s `actionText`/`onAction`.

**Steps:**

- [ ] **Step 1: Write `NxEmptyState.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

private val EmptyIconSize: Dp = 40.dp
private val EmptyMaxWidth: Dp = 320.dp

/**
 * Centered empty / placeholder state: a large [icon], a [title], supporting [body], and an optional
 * call-to-action ([ctaText] + [onCta]). Used for the "coming soon" shell stubs and every empty list.
 */
@Composable
fun NxEmptyState(
    icon: NxIconKind,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    ctaText: String? = null,
    onCta: (() -> Unit)? = null,
) {
    val colors = NxTokens.colors
    Column(
        modifier = modifier.fillMaxSize().padding(NxSpacing.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NxSpacing.s3, Alignment.CenterVertically),
    ) {
        NxIcon(kind = icon, tint = colors.fgMuted, size = EmptyIconSize)
        NxText(text = title, style = NxTextStyle.Heading, textAlign = TextAlign.Center)
        NxText(
            text = body,
            style = NxTextStyle.Body,
            color = colors.fgSubtle,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = EmptyMaxWidth),
        )
        if (ctaText != null && onCta != null) {
            NxButton(text = ctaText, onClick = onCta)
        }
    }
}
```

- [ ] **Step 2: Write `NxEmptyStatePreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxEmptyStateSample() {
    NxEmptyState(
        icon = NxIconKind.Vault,
        title = "Your vault is empty",
        body = "Hidden messages you save will appear here.",
        ctaText = "Encrypt a message",
        onCta = {},
    )
}

@AllThemePreview
@Composable
private fun NxEmptyStatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxEmptyStateSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxEmptyState with optional call-to-action"`.

---

### Task 17: NxProgressOverlay

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxProgressOverlay.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/molecules/NxProgressOverlayPreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxSpacing`, `NxTextStyle` (Tasks 3-4), `NxText` (Task 5), M3 `CircularProgressIndicator`.
- Produces (pinned by plan 06): `@Composable fun NxProgressOverlay(label: String, modifier: Modifier = Modifier)`.

Color note: the scrim is `colors.bg` at a named `ScrimAlpha` (deriving alpha from a token color is token-pure — no raw ARGB); the spinner is tinted `colors.brand`. Callers pass `Modifier.fillMaxSize()` to blanket the screen.

**Steps:**

- [ ] **Step 1: Write `NxProgressOverlay.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

private const val ScrimAlpha = 0.72f

/**
 * A blocking progress scrim: a translucent [background] over the content it covers, centered on a
 * brand spinner and a [label]. Size is caller-driven (typically `Modifier.fillMaxSize()`).
 */
@Composable
fun NxProgressOverlay(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = NxTokens.colors
    Box(
        modifier = modifier.background(colors.bg.copy(alpha = ScrimAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NxSpacing.s3),
        ) {
            CircularProgressIndicator(color = colors.brand)
            NxText(text = label, style = NxTextStyle.Caption, color = colors.fgMuted)
        }
    }
}
```

- [ ] **Step 2: Write `NxProgressOverlayPreview.kt`** (a fixed-height box gives the overlay bounds to fill in the golden):

```kotlin
package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

private val OverlayPreviewHeight: Dp = 240.dp

@Composable
fun NxProgressOverlaySample() {
    Box(modifier = Modifier.fillMaxWidth().height(OverlayPreviewHeight)) {
        NxProgressOverlay(label = "Encrypting", modifier = Modifier.fillMaxSize())
    }
}

@AllThemePreview
@Composable
private fun NxProgressOverlayPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxProgressOverlaySample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxProgressOverlay blocking scrim"`.

---

### Task 18: NxDetailTemplate

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/templates/NxDetailTemplate.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/templates/NxDetailTemplatePreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxSpacing` (Tasks 3-4), `NxTopBar` (Task 11).
- Produces (pinned by plan 06): `@Composable fun NxDetailTemplate(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit)`.

Template composition: `NxTopBar` (with `onBack` + `trailing`) over a vertically scrolling, padded content column. Composes only already-built pieces (Task 11 top bar); the `content` block supplies feature-specific children in a `ColumnScope`.

**Steps:**

- [ ] **Step 1: Write `NxDetailTemplate.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

/**
 * A detail screen scaffold: an [NxTopBar] (with a back affordance and an optional [trailing] actions
 * slot) over a scrolling, padded [content] column.
 */
@Composable
fun NxDetailTemplate(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = NxTokens.colors
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = title, onBack = onBack, trailing = trailing)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(NxSpacing.s4),
            verticalArrangement = Arrangement.spacedBy(NxSpacing.s3),
            content = content,
        )
    }
}
```

- [ ] **Step 2: Write `NxDetailTemplatePreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxDetailTemplateSample() {
    NxDetailTemplate(
        title = "vacation-2026.png",
        onBack = {},
        trailing = {
            NxIconButton(kind = NxIconKind.Share, onClick = {}, style = NxIconButtonStyle.Ghost, contentDescription = "Share")
        },
    ) {
        NxText(text = "Saved 2 hours ago", style = NxTextStyle.Caption, color = NxTokens.colors.fgMuted)
        NxButton(text = "Decrypt this", onClick = {}, block = true, leadingIcon = NxIconKind.Unlock)
        NxButton(text = "Archive", onClick = {}, style = NxButtonStyle.Soft, block = true, leadingIcon = NxIconKind.Archive)
    }
}

@AllThemePreview
@Composable
private fun NxDetailTemplatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxDetailTemplateSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxDetailTemplate scaffold"`.

---

### Task 19: NxFormTemplate

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/templates/NxFormTemplate.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/templates/NxFormTemplatePreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxSpacing` (Tasks 3-4), `NxButton` (Task 7), `NxTopBar` (Task 11).
- Produces: `@Composable fun NxFormTemplate(title: String, onBack: () -> Unit, primaryLabel: String, onPrimary: () -> Unit, modifier: Modifier = Modifier, primaryEnabled: Boolean = true, content: @Composable ColumnScope.() -> Unit)`.

Signature note: 00-INDEX lists `NxFormTemplate` with no parameter list and plan 06 does not call it directly (it uses `NxWizardTemplate` for the encrypt flow and `NxDetailTemplate` elsewhere), so this signature is defined here. It mirrors `NxDetailTemplate` (top bar + scrolling content) and adds a pinned full-width primary action at the bottom — the standard single-submit form shape. It composes only Task 7 (`NxButton`) and Task 11 (`NxTopBar`).

**Steps:**

- [ ] **Step 1: Write `NxFormTemplate.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens

/**
 * A single-submit form scaffold: an [NxTopBar] over a scrolling, padded [content] column of fields,
 * with a pinned full-width primary action ([primaryLabel] + [onPrimary], gated by [primaryEnabled])
 * anchored at the bottom.
 */
@Composable
fun NxFormTemplate(
    title: String,
    onBack: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = NxTokens.colors
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(NxSpacing.s4),
            verticalArrangement = Arrangement.spacedBy(NxSpacing.s3),
            content = content,
        )
        Column(modifier = Modifier.fillMaxWidth().padding(NxSpacing.s4)) {
            NxButton(text = primaryLabel, onClick = onPrimary, block = true, enabled = primaryEnabled)
        }
    }
}
```

- [ ] **Step 2: Write `NxFormTemplatePreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxField
import com.slothiesmooth.nyx.designlibrary.atoms.NxPasswordField
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxFormTemplateSample() {
    NxFormTemplate(
        title = "Encrypt",
        onBack = {},
        primaryLabel = "Encrypt message",
        onPrimary = {},
    ) {
        NxField(value = "vacation-2026.png", onValueChange = {}, label = "Image name", modifier = Modifier.fillMaxWidth())
        NxPasswordField(value = "hunter2hunter2", onValueChange = {}, label = "Password", modifier = Modifier.fillMaxWidth())
    }
}

@AllThemePreview
@Composable
private fun NxFormTemplatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxFormTemplateSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxFormTemplate with pinned primary action"`.

---

### Task 20: NxWizardTemplate

**Files:**
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/templates/NxWizardTemplate.kt`
- Create: `shared/design-library/src/commonMain/kotlin/com/slothiesmooth/nyx/designlibrary/templates/NxWizardTemplatePreview.kt`

**Interfaces:**
- Consumes: `NxTokens`, `NxSpacing`, `NxTextStyle` (Tasks 3-4), `NxText` (Task 5), `NxTopBar` (Task 11), `kotlinx.collections.immutable.ImmutableList`.
- Produces (pinned by plan 06): `@Composable fun NxWizardTemplate(stepLabels: ImmutableList<String>, currentStep: Int, title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)`.

Template composition: `NxTopBar` over a numbered step-indicator row (each step a circular badge + label; badges/labels at or before `currentStep` tint `brand`/`fg`, later steps `bgElev2`/`fgMuted`) over a scrolling, padded `content` column. `stepLabels` is an `ImmutableList` per the collections rule. Composes only Task 5 (`NxText`) and Task 11 (`NxTopBar`).

**Steps:**

- [ ] **Step 1: Write `NxWizardTemplate.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
import com.slothiesmooth.nyx.designlibrary.tokens.NxSpacing
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import kotlinx.collections.immutable.ImmutableList

private val StepBadgeSize: Dp = 24.dp

/**
 * A multi-step wizard scaffold: an [NxTopBar] over a numbered step indicator (driven by [stepLabels]
 * and [currentStep]) over a scrolling, padded [content] column for the active step's body.
 */
@Composable
fun NxWizardTemplate(
    stepLabels: ImmutableList<String>,
    currentStep: Int,
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = NxTokens.colors
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = title, onBack = onBack)
        NxWizardSteps(stepLabels = stepLabels, currentStep = currentStep)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(NxSpacing.s4),
            verticalArrangement = Arrangement.spacedBy(NxSpacing.s3),
            content = content,
        )
    }
}

@Composable
private fun NxWizardSteps(
    stepLabels: ImmutableList<String>,
    currentStep: Int,
) {
    val colors = NxTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NxSpacing.s4, vertical = NxSpacing.s3),
        horizontalArrangement = Arrangement.spacedBy(NxSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stepLabels.forEachIndexed { index, label ->
            val active = index <= currentStep
            val badgeColor = if (active) colors.brand else colors.bgElev2
            val badgeTextColor = if (active) colors.brandFg else colors.fgMuted
            val labelColor = if (active) colors.fg else colors.fgMuted
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(NxSpacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = CircleShape, color = badgeColor, modifier = Modifier.size(StepBadgeSize)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NxText(text = (index + 1).toString(), style = NxTextStyle.Caption, color = badgeTextColor)
                    }
                }
                NxText(text = label, style = NxTextStyle.Caption, color = labelColor, maxLines = 1)
            }
        }
    }
}
```

- [ ] **Step 2: Write `NxWizardTemplatePreview.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxField
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.NxTokens
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxWizardTemplateSample() {
    NxWizardTemplate(
        stepLabels = persistentListOf("Image", "Message", "Done"),
        currentStep = 1,
        title = "Encrypt",
        onBack = {},
    ) {
        NxText(text = "Write the secret you want to hide.", style = NxTextStyle.Body, color = NxTokens.colors.fgMuted)
        NxField(value = "", onValueChange = {}, label = "Message", placeholder = "The secret to hide", multiline = true, modifier = Modifier.fillMaxWidth())
    }
}

@AllThemePreview
@Composable
private fun NxWizardTemplatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxWizardTemplateSample() }
}
```

- [ ] **Step 3: Verify:** `./gradlew :shared:design-library:compileKotlinJvm` → `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit:** `git add shared/design-library/src && git commit -m "feat(design-library): NxWizardTemplate with numbered step indicator"`.

---

## Snapshot module — Paparazzi goldens

Tasks 21-22 stand up `:shared:design-library:snapshot` (a plain `com.android.library` JVM-test module) and record the golden set. It reuses every public `NxXSample()` from Tasks 5-20 — the samples are the single source of truth shared by the IDE `@AllThemePreview` probes and these snapshots, so a component and its golden can never drift. Goldens are recorded on Linux (this dev machine), which matches the ubuntu CI runner's font rendering (00-INDEX Paparazzi note).

---

### Task 21: Snapshot module wiring + `nxPaparazzi()` factory

**Files:**
- Modify: `settings.gradle.kts` (verify `include(":shared:design-library:snapshot")`)
- Modify: `gradle/libs.versions.toml` (verify/add the `com.android.library` plugin alias)
- Create: `shared/design-library/snapshot/build.gradle.kts`
- Create: `shared/design-library/snapshot/src/test/kotlin/com/slothiesmooth/nyx/designlibrary/snapshot/NxPaparazzi.kt`

**Interfaces:**
- Consumes: `:shared:design-library` (Tasks 1-20), Paparazzi 2.0.0-alpha05 + junit4 (added in Task 1), version catalog.
- Produces: a configured snapshot module exposing `fun nxPaparazzi(): Paparazzi` (`DeviceConfig.PIXEL_5` + `SessionParams.RenderingMode.SHRINK`).

**Steps:**

- [ ] **Step 1: Reconcile the catalog plugin alias.** The snapshot module is a plain Android library (NOT the KMP-library plugin), so it needs the `com.android.library` plugin. Verify `gradle/libs.versions.toml` has it (plan 01 owns this file — if plan 01 already declares an alias for `com.android.library`, use that name throughout this task). Add if missing:

```toml
[plugins]
# ... existing (agp version.ref already defined by plan 01) ...
android-library = { id = "com.android.library", version.ref = "agp" }
```

- [ ] **Step 2: Verify module registration.** Run:

```bash
grep -n "design-library:snapshot" settings.gradle.kts
```

Expected: `include(":shared:design-library:snapshot")` (added by plan 01 / Task 1 Step 2). If missing, add it.

- [ ] **Step 3: Write `shared/design-library/snapshot/build.gradle.kts`.** Plain `com.android.library` (no `org.jetbrains.kotlin.android` — AGP 9's built-in Kotlin compiles the sources, per 00-INDEX). The JetBrains compose plugin supplies the `compose.*` dependency accessors and the Kotlin compose-compiler plugin compiles the `@Composable` test lambdas (if plan 01 exposes a convention plugin that produces this identical effective config for a paparazzi module, applying it instead is fine):

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.slothiesmooth.nyx.designlibrary.snapshot"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

dependencies {
    implementation(project(":shared:design-library"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    testImplementation(libs.junit4)
}
```

- [ ] **Step 4: Write `NxPaparazzi.kt`** (the shared factory — one place pins the device + rendering mode for every snapshot class):

```kotlin
package com.slothiesmooth.nyx.designlibrary.snapshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams

/**
 * Shared Paparazzi factory: renders on a Pixel 5 and SHRINKs the frame to each sample's own bounds.
 * Recorded on Linux (dev machine) to match the ubuntu CI runner's font rendering.
 */
fun nxPaparazzi(): Paparazzi = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_5,
    renderingMode = SessionParams.RenderingMode.SHRINK,
    showSystemUi = false,
)
```

- [ ] **Step 5: Verify the module configures and the factory compiles.** Run:

```bash
./gradlew :shared:design-library:snapshot:compileDebugUnitTestKotlin
./gradlew :shared:design-library:snapshot:tasks --all | grep -i paparazzi
```

Expected: `BUILD SUCCESSFUL` on the first command (the test source set — currently just `NxPaparazzi.kt` — compiles against `:shared:design-library` and Paparazzi), and the second lists `recordPaparazziDebug` / `verifyPaparazziDebug` (confirming the plugin applied). If `compileDebugUnitTestKotlin` is not the task name on this AGP build, discover it with `./gradlew :shared:design-library:snapshot:tasks --all | grep -i "UnitTestKotlin"`.

- [ ] **Step 6: Commit.**

```bash
git add settings.gradle.kts gradle/libs.versions.toml shared/design-library/snapshot/build.gradle.kts shared/design-library/snapshot/src
git commit -m "feat(design-library): Paparazzi snapshot module and nxPaparazzi factory"
```

---

### Task 22: Parameterized snapshot classes + record goldens (final task)

**Files:**
- Create: `shared/design-library/snapshot/src/test/kotlin/com/slothiesmooth/nyx/designlibrary/snapshot/NxAtomsSnapshotTest.kt`
- Create: `shared/design-library/snapshot/src/test/kotlin/com/slothiesmooth/nyx/designlibrary/snapshot/NxMoleculesSnapshotTest.kt`
- Create: `shared/design-library/snapshot/src/test/kotlin/com/slothiesmooth/nyx/designlibrary/snapshot/NxOrganismsSnapshotTest.kt`
- Create: `shared/design-library/snapshot/src/test/kotlin/com/slothiesmooth/nyx/designlibrary/snapshot/NxTemplatesSnapshotTest.kt`
- Create (recorded, binary): `shared/design-library/snapshot/src/test/snapshots/**` (goldens — one PNG per sample × palette)

**Interfaces:**
- Consumes: `nxPaparazzi()` (Task 21), `NxPalette`/`NxTheme` (Task 4), every public `NxXSample()` (Tasks 5-20).
- Produces: recorded goldens plus a green `verifyPaparazziDebug`.

One `@RunWith(Parameterized::class)` class per atomic layer (atoms / molecules / organisms / templates), each parameterized over `NxPalette.entries` and rendering every sample in its layer through `NxTheme(palette)`. The JUnit parameter name (`{0}` → the palette) and the `@Test` method name together make each golden file unique (e.g. `NxAtomsSnapshotTest_buttons[Midnight].png`).

**Steps:**

- [ ] **Step 1: Write `NxAtomsSnapshotTest.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.snapshot

import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import com.slothiesmooth.nyx.designlibrary.atoms.NxButtonSample
import com.slothiesmooth.nyx.designlibrary.atoms.NxChipSample
import com.slothiesmooth.nyx.designlibrary.atoms.NxFieldSample
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButtonSample
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconSetSample
import com.slothiesmooth.nyx.designlibrary.atoms.NxTextSample
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NxAtomsSnapshotTest(private val palette: NxPalette) {

    @get:Rule
    val paparazzi: Paparazzi = nxPaparazzi()

    @Test fun text() = snapshot { NxTextSample() }

    @Test fun icons() = snapshot { NxIconSetSample() }

    @Test fun iconButtons() = snapshot { NxIconButtonSample() }

    @Test fun buttons() = snapshot { NxButtonSample() }

    @Test fun fields() = snapshot { NxFieldSample() }

    @Test fun chips() = snapshot { NxChipSample() }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot { NxTheme(palette = palette) { content() } }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun palettes(): List<NxPalette> = NxPalette.entries
    }
}
```

- [ ] **Step 2: Write `NxMoleculesSnapshotTest.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.snapshot

import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import com.slothiesmooth.nyx.designlibrary.molecules.NxCardSample
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyStateSample
import com.slothiesmooth.nyx.designlibrary.molecules.NxFabSample
import com.slothiesmooth.nyx.designlibrary.molecules.NxImageTileSample
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlaySample
import com.slothiesmooth.nyx.designlibrary.molecules.NxSectionHeaderSample
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBarSample
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NxMoleculesSnapshotTest(private val palette: NxPalette) {

    @get:Rule
    val paparazzi: Paparazzi = nxPaparazzi()

    @Test fun card() = snapshot { NxCardSample() }

    @Test fun topBar() = snapshot { NxTopBarSample() }

    @Test fun fab() = snapshot { NxFabSample() }

    @Test fun imageTile() = snapshot { NxImageTileSample() }

    @Test fun sectionHeader() = snapshot { NxSectionHeaderSample() }

    @Test fun emptyState() = snapshot { NxEmptyStateSample() }

    @Test fun progressOverlay() = snapshot { NxProgressOverlaySample() }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot { NxTheme(palette = palette) { content() } }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun palettes(): List<NxPalette> = NxPalette.entries
    }
}
```

- [ ] **Step 3: Write `NxOrganismsSnapshotTest.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.snapshot

import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import com.slothiesmooth.nyx.designlibrary.organisms.NxBottomNavSample
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NxOrganismsSnapshotTest(private val palette: NxPalette) {

    @get:Rule
    val paparazzi: Paparazzi = nxPaparazzi()

    @Test fun bottomNav() = snapshot { NxBottomNavSample() }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot { NxTheme(palette = palette) { content() } }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun palettes(): List<NxPalette> = NxPalette.entries
    }
}
```

- [ ] **Step 4: Write `NxTemplatesSnapshotTest.kt`:**

```kotlin
package com.slothiesmooth.nyx.designlibrary.snapshot

import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import com.slothiesmooth.nyx.designlibrary.templates.NxDetailTemplateSample
import com.slothiesmooth.nyx.designlibrary.templates.NxFormTemplateSample
import com.slothiesmooth.nyx.designlibrary.templates.NxWizardTemplateSample
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NxTemplatesSnapshotTest(private val palette: NxPalette) {

    @get:Rule
    val paparazzi: Paparazzi = nxPaparazzi()

    @Test fun detail() = snapshot { NxDetailTemplateSample() }

    @Test fun form() = snapshot { NxFormTemplateSample() }

    @Test fun wizard() = snapshot { NxWizardTemplateSample() }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot { NxTheme(palette = palette) { content() } }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun palettes(): List<NxPalette> = NxPalette.entries
    }
}
```

- [ ] **Step 5: Record the goldens.** Run:

```bash
./gradlew :shared:design-library:snapshot:recordPaparazziDebug
```

Expected: `BUILD SUCCESSFUL`; PNGs written under `shared/design-library/snapshot/src/test/snapshots/` — one per `@Test` method × palette (17 samples × 5 palettes = 85 goldens: 6 atoms + 7 molecules + 1 organism + 3 templates). Eyeball a handful (e.g. the `Midnight` and `Cream` button/topBar goldens) to confirm fonts, tokens, and layout rendered as expected before locking them in.

- [ ] **Step 6: Commit the goldens.**

```bash
git add shared/design-library/snapshot/src/test/snapshots
git commit -m "test(design-library): record Paparazzi goldens for all Nx components across five palettes"
```

- [ ] **Step 7: Verify against the recorded goldens.** Run:

```bash
./gradlew :shared:design-library:snapshot:verifyPaparazziDebug
```

Expected: `BUILD SUCCESSFUL` — every rendered frame matches its committed golden (this is the exact task ubuntu CI runs; Linux-recorded goldens match the CI runner's fonts).

---

## Cross-plan note (for the executor — reconciles plans 05/06 against this completed plan 04)

This plan now delivers all 16 components (Tasks 5-20) plus the snapshot module (Tasks 21-22). Two items overlap with downstream plans as originally written; resolve them once when executing:

1. **`NxBottomNav` + `NxEmptyState` are delivered here (Tasks 12, 16), not in plan 05 Task 3.** Plan 05 was written assuming plan 04 stopped at `NxCard`. When executing plan 05, **skip its Task 3** (both components exist after this plan) and keep only plan 05's downstream wiring (the `NavItem → NxBottomNavItem` mapping in its Task 8, and the feature stubs in its Task 9).
2. **`NxEmptyState` CTA parameter is `ctaText` (not `ctaLabel`).** Chosen to satisfy plan 06's explicit mandate that plan 04 Tasks 11-20 expose those exact signatures. Update plan 05's single `ctaLabel = "Change theme"` call in `BasicSettingsProvider` to `ctaText = "Change theme"`.
3. **`NxIconKind` package (resolved).** `NxIconKind` is declared in `tokens/NxIconKind.kt` (package `...designlibrary.tokens`), created in Task 6 Step 1 — matching 00-INDEX (which lists it in the tokens section) and the `...designlibrary.tokens.NxIconKind` imports already used throughout plans 05/06. Every reference in this plan (Tasks 6-16, 18) imports it from `tokens`. No action is required for plans 05/06 — their existing `tokens.NxIconKind` imports are already correct.
