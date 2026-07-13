# Engines Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the two pure computation engines — `:steganography` (LSB 2-bit-per-channel encoder over `PixelImage`) and `:crypto` (`DefaultNyxCrypto`, AES-256-GCM + PBKDF2 via cryptography-kotlin) — each with a complete acceptance test suite green on jvm, android host, and wasm.

**Architecture:** Two independent 6-target KMP modules with no dependency on each other in production. `:steganography` is stdlib + kotlinx-coroutines only (no platform APIs); it frames a UTF-8 payload with markers and hides it in the low 2 bits of each R/G/B channel across one or more `PixelImage` covers, never touching alpha. `:crypto` is thin glue over cryptography-kotlin: PBKDF2-HMAC-SHA256 key derivation + AES-GCM AEAD + Base64 blob packing, returning a sealed `DecryptResult`. The end-to-end pipeline (`crypto.encrypt` → `stego.encode` → `stego.decode` → `crypto.decrypt`) is proven by an integration test living in `:crypto`'s `commonTest` with a **test-only** dependency on `:steganography`.

**Tech Stack:** Kotlin 2.3.21, AGP 9.2.0 (`com.android.kotlin.multiplatform.library`), kotlinx-coroutines 1.10.2, cryptography-kotlin 0.6.0 (`cryptography-core` + `cryptography-provider-optimal`), `kotlin.io.encoding.Base64`, kotlin.test + kotlinx-coroutines-test.

## Global Constraints

Copied verbatim from `00-INDEX.md` (all of 00-INDEX applies; these are the lines this phase exercises):

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
- Tests: kotlin.test + kotlinx-coroutines-test + hand-written fakes only. No mockk/kotest/turbine.
- `suspend` end-to-end for crypto (WebCrypto provider is suspend-only; `*Blocking` throws on wasm).
- No `println`; logging via Kermit.
- Commit after every green test cycle (conventional commits).

Phase-specific constraints:

- **`:steganography` is engine-pure:** stdlib + `kotlinx-coroutines-core` ONLY. No `android.graphics`,
  no skiko, no `System.gc()`, no class-level `CoroutineScope`/`Job` field, no `Vector`, no
  `@Suppress`, no silent truncation. Bulk `IntArray` operations only (never per-pixel `setPixel`).
- **`:crypto` uses the library, never hand-rolled primitives:** no `javax.crypto`, no
  `android.util.Base64`, no CBC+HMAC composition, no fixed/reused salt or nonce, never returns
  `null` on failure (returns the sealed `DecryptResult`). No module-level `CoroutineScope`/`Job`.
- **Alpha is never touched** by the stego algorithm. Covers arrive opaque from the codec (00-INDEX:
  `ImageCodec.decode` forces `or 0xFF000000`), but the algorithm must not assume that — it simply
  leaves bits 24..31 of every pixel unchanged.
- **detekt maxIssues = 0, buildUponDefaultConfig.** The code below is written to pass detekt's
  default rules with zero suppressions. The specific rules this phase must satisfy and how the code
  satisfies them are listed in "Context" → "detekt rules this phase must not trip".
- **Interface contracts:** every public signature matches the `:steganography` and `:crypto`
  sections of `00-INDEX.md` exactly. Downstream plans (05 DI registration, 06 encrypt/decrypt use
  cases) import them as written here.

---

## Context you need before starting (read once — every fact is baked in here)

You are skilled but assume you know **nothing** about this codebase, pawdex, KMP quirks, or the
research. Everything needed is below; the pawdex reference is not required for this phase.

### Module coordinates

| Module | Gradle path | Directory | Kotlin package | Android namespace |
|---|---|---|---|---|
| steganography | `:steganography` | `steganography/` | `com.slothiesmooth.nyx.steganography` | `com.slothiesmooth.nyx.steganography` |
| crypto | `:crypto` | `crypto/` | `com.slothiesmooth.nyx.crypto` | `com.slothiesmooth.nyx.crypto` |

`steganography/` currently still holds the OLD Android module (`src/main/java/in/technowolf/...`,
`local.noui.library` plugin, `proguard-rules.pro`). Plan 01 is meant to replace it with a KMP
skeleton; Task 1 verifies and repairs (overwriting the build file and clearing stale `src/main`).
`crypto/` does not exist yet; Task 5 creates it. In both cases, if plan 01 already produced the
skeleton, Task 1/5 only verify it — the effective build config below is what matters.

### The steganography LSB algorithm (specified precisely so it is not guessed)

Port the **idea** from the old code (`app/src/main/java/in/technowolf/nyx/core/Steganography.kt`)
but none of its sins. The scheme:

- A `PixelImage` is a flat `IntArray` of ARGB pixels, row-major, one `0xAARRGGBB` int per pixel.
- **2 bits per channel, R/G/B only** → 6 payload bits per pixel. Alpha (bits 24..31) is never read
  or written.
- The payload is framed: `frame = startMarker + payload + endMarker`, then UTF-8 encoded to bytes
  (`(startMarker + payload + endMarker).encodeToByteArray()`). Default markers `@!#` / `#!@`.
- Each **byte** is split into **4 chunks of 2 bits**, most-significant chunk first:
  chunk 0 = bits 6..7, chunk 1 = bits 4..5, chunk 2 = bits 2..3, chunk 3 = bits 0..1.
- Chunks are written into successive **channels** in R,G,B order across pixels and across images:
  channel 0 = pixel0.R, channel 1 = pixel0.G, channel 2 = pixel0.B, channel 3 = pixel1.R, …
  Writing a chunk into a channel = clear the low 2 bits (`and 0xFC`) then `or chunk`.
- **Multi-image spanning:** channels flow continuously from one image into the next; a byte may
  straddle an image boundary. Images not needed once the frame is fully written are copied unchanged.
- **Capacity, checked up front before mutating anything:**
  `availableBits = (Σ width·height·3) · 2`; `requiredBits = frameBytes.size · 8`. Each byte needs
  4 channels · 2 bits = 8 storage bits, so this is an apples-to-apples comparison. If
  `requiredBits > availableBits` → return `CapacityExceeded(requiredBits, availableBits)` and touch
  no pixels.
- **Decode** reads channels in the same order, reassembling each byte from 4 chunks
  (`byte = c0<<6 | c1<<4 | c2<<2 | c3`), then detects the frame **at the byte level** (markers are
  ASCII; UTF-8 is self-synchronizing, so an ASCII marker byte can never appear inside a multibyte
  sequence — byte-level detection is fully correct and avoids the old code's incremental-UTF-8 bug):
  - As soon as the decoded buffer reaches `startMarker` length and does **not** start with the start
    marker → no frame present → return `null`.
  - As soon as the buffer is at least `startMarker+endMarker` length and **ends with** the end
    marker → the frame is complete → payload = buffer minus the leading start and trailing end
    markers, decoded as UTF-8.
  - Stream exhausted with no complete frame → `null`.
- **Marker limitation (documented, not a bug):** the payload must not itself contain the end-marker
  byte sequence `#!@`. In Nyx the payload is always the crypto Base64 blob, whose alphabet
  `[A-Za-z0-9+/=]` excludes `#`, `!`, `@`, so this never occurs. Standalone stego tests avoid `#!@`
  in payloads.

### The crypto API (verified against cryptography-kotlin 0.6.0 — use VERBATIM, do not re-derive)

From 00-INDEX "Research-verified API facts". Default `cipher.encrypt(plaintext)` generates a fresh
random 12-byte IV and PREPENDS it: output = `iv(12) || ciphertext || tag(16)`; `decrypt()` splits it
back automatically and throws on wrong key/tamper. Therefore the Nyx blob is
`Base64(salt(16) || cipher.encrypt(plaintext))` = `Base64([salt 16][iv 12][ct+tag])`. No
`@DelicateCryptographyApi`, no explicit nonce handling.

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

iOS needs NO extra provider (00-INDEX deviation #1: `optimal` covers AES-GCM via CryptoKit and
PBKDF2 via CommonCrypto; `openssl3-prebuilt` is not required). Do not add anything to `iosMain`.

### detekt rules this phase must not trip (why the code below is shaped the way it is)

- **`MagicNumber`** — every non-trivial literal in `commonMain` lives in a named `private const val`
  (detekt ignores `const`/property declarations by default). detekt's default `MagicNumber` also
  ignores `0, 1, 2, -1` and excludes `**/commonTest/**`, so test literals (hex colors, bit counts)
  are fine as-is.
- **`ReturnCount`** (default max 2) — no function below has more than two `return` statements.
- **`RedundantSuspendModifier`** — a `suspend` function with no suspension point is flagged. Both
  engines are contractually `suspend`. `:crypto` genuinely calls suspend library functions.
  `:steganography` calls no suspend library function, so each public method opens with
  `coroutineContext.ensureActive()` (a suspend property access + cooperative-cancellation check) —
  this makes the modifier non-redundant AND adds real value. That is why `:steganography` depends on
  `kotlinx-coroutines-core`.
- **`InjectDispatcher`** — no hardcoded `Dispatchers.X` appears in any function body (the engines do
  not switch dispatchers; the caller/use-case owns the scope and dispatcher).
- **`TooGenericExceptionCaught`** — no explicit `catch (e: Throwable)`. `:crypto` uses `runCatching`
  plus `coroutineContext.ensureActive()` (the canonical "don't swallow cancellation" pattern, which
  also satisfies `SuspendFunSwallowedCancellation` if plan 01 enables it) to convert any decrypt
  failure into `WrongPasswordOrTampered`.
- **`LoopWithTooManyJumpStatements`** (default max 1) — no loop below contains more than one
  `break`/`continue`/`return`.

### Deliberate deviations from the literal contracts (flagged; confirm if uncomfortable)

1. `:steganography` gains a `kotlinx-coroutines-core` dependency. Spec §5 said "stdlib-only", but
   the 00-INDEX contract mandates `suspend` methods; kotlinx-coroutines is a **multiplatform** library
   (not a platform dep — no android/skiko), used only for `coroutineContext.ensureActive()`. The
   spec's intent (no `android.graphics`/skiko) is preserved. The constructor stays exactly the
   contract's two parameters.
2. `PixelImage` gains `init { require(pixels.size == width * height) }`. The 00-INDEX contract shows
   the primary constructor only; this invariant is a compatible addition that the encode/decode
   indexing relies on, and it is testable.

### Cross-target verification vocabulary

- Fast TDD loop: `./gradlew :<module>:jvmTest`.
- Android host (unit) tests run on the JVM (no emulator); the AGP KMP plugin's task name may vary,
  so each cross-target task first discovers it with
  `./gradlew :<module>:tasks --all | grep -i "hosttest"` (expected: a task named like
  `testAndroidHostTest`) and runs whatever that prints.
- wasm tests run in **headless Chrome via Karma** (`wasmJsBrowserTest`). CI must have Chrome
  installed; a local machine without Chrome skips it with `-x wasmJsBrowserTest`. Do not `@Ignore`
  it — PBKDF2 600k on wasm runs through the browser's **native** WebCrypto (`deriveBits`), so it is
  fast, not slow.

---

### Task 1: `:steganography` module wiring and build file

**Files:**
- Modify: `settings.gradle.kts` (verify `include(":steganography")`)
- Modify: `gradle/libs.versions.toml` (verify/add coroutines + kotlin-test entries)
- Create/Overwrite: `steganography/build.gradle.kts`
- Delete (stale old-module sources being repurposed): `steganography/src/main`, `steganography/proguard-rules.pro`

**Interfaces:**
- Consumes: plan 01 module skeleton, version catalog, Gradle 9.4.1 wrapper, typesafe project accessors.
- Produces: a compiling, empty `:steganography` KMP module (6 targets) that Tasks 2-4 build on.

**Steps:**

- [ ] **Step 1: Reconcile the version catalog.** Open `gradle/libs.versions.toml`. Plan 01 owns this
  file; its alias names win — if plan 01 named the same coordinates differently, use plan 01's names
  throughout this plan. Verify these exist and add whatever is missing:

```toml
[versions]
agp = "9.2.0"
kotlin = "2.3.21"
kotlinx-coroutines = "1.10.2"
android-compileSdk = "36"
android-minSdk = "24"

[libraries]
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
android-kmp-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
```

- [ ] **Step 2: Verify module registration.** Run:

```bash
grep -n '":steganography"' settings.gradle.kts
```

Expected: a line inside the `include(...)` call containing `":steganography"`. If missing, add it.

- [ ] **Step 3: Remove stale old-module sources.** The directory is being repurposed from the old
  Android module to a KMP module. Run:

```bash
git rm -r --ignore-unmatch steganography/src/main steganography/proguard-rules.pro
```

Expected: the old `in/technowolf/...` Android sources and the proguard file are removed. (The KMP
plugin compiles `src/commonMain`/`src/androidMain`, never `src/main`, so leaving them would be
inert — but removing them keeps the module clean and avoids confusion. Formal deletion of the rest
of the old tree is plan 08.)

- [ ] **Step 4: Write the build file.** Create/overwrite `steganography/build.gradle.kts` (if plan 01
  provides a KMP convention plugin producing the identical effective config, applying it instead is
  fine — the config below is what matters):

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.slothiesmooth.nyx.steganography"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTestBuilder { }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm()
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

- [ ] **Step 5: Verify the empty module compiles.** Run:

```bash
./gradlew :steganography:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. (No sources yet — this proves the build wiring, targets, and catalog
aliases resolve.)

- [ ] **Step 6: Commit.**

```bash
git add settings.gradle.kts gradle/libs.versions.toml steganography/build.gradle.kts
git commit -m "build(steganography): KMP module wiring for the LSB engine"
```

---

### Task 2: `PixelImage` and `StegoEncodeResult` types

**Files:**
- Test: `steganography/src/commonTest/kotlin/com/slothiesmooth/nyx/steganography/PixelImageTest.kt`
- Create: `steganography/src/commonMain/kotlin/com/slothiesmooth/nyx/steganography/PixelImage.kt`
- Create: `steganography/src/commonMain/kotlin/com/slothiesmooth/nyx/steganography/StegoEncodeResult.kt`

**Interfaces:**
- Consumes: Task 1 module.
- Produces (00-INDEX contract):
  - `class PixelImage(val width: Int, val height: Int, val pixels: IntArray)` (ARGB; `pixels.size == width*height`, enforced).
  - `sealed interface StegoEncodeResult` with `data class Success(val images: List<PixelImage>)` and `data class CapacityExceeded(val requiredBits: Long, val availableBits: Long)`.

**Steps:**

- [ ] **Step 1: Write the failing test.** Create `PixelImageTest.kt`:

```kotlin
package com.slothiesmooth.nyx.steganography

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PixelImageTest {

    @Test
    fun `holds its dimensions and pixel data`() {
        val pixels = intArrayOf(0xFF112233.toInt(), 0xFF445566.toInt(), 1, 2)
        val image = PixelImage(width = 2, height = 2, pixels = pixels)
        assertEquals(2, image.width)
        assertEquals(2, image.height)
        assertContentEquals(pixels, image.pixels)
    }

    @Test
    fun `rejects a pixel array whose size is not width times height`() {
        assertFailsWith<IllegalArgumentException> {
            PixelImage(width = 2, height = 2, pixels = intArrayOf(1, 2, 3))
        }
    }
}
```

- [ ] **Step 2: Run it — expect a compile failure.**

```bash
./gradlew :steganography:compileTestKotlinJvm
```

Expected: `> Task :steganography:compileTestKotlinJvm FAILED` with
`Unresolved reference: PixelImage`.

- [ ] **Step 3: Create `PixelImage.kt`.**

```kotlin
package com.slothiesmooth.nyx.steganography

/**
 * A decoded raster image as a flat, row-major array of ARGB integers.
 *
 * Each entry packs one pixel as `0xAARRGGBB`. [pixels] length is always [width] * [height].
 */
class PixelImage(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
) {
    init {
        require(pixels.size == width * height) {
            "pixels size ${pixels.size} must equal width * height (${width * height})"
        }
    }
}
```

- [ ] **Step 4: Create `StegoEncodeResult.kt`.**

```kotlin
package com.slothiesmooth.nyx.steganography

/** Outcome of [Steganography.encode]. */
sealed interface StegoEncodeResult {

    /** The framed payload was embedded; [images] are the stego covers (in input order). */
    data class Success(val images: List<PixelImage>) : StegoEncodeResult

    /**
     * The framed payload does not fit in the supplied covers.
     *
     * [requiredBits] is the storage the framed payload needs; [availableBits] is what the covers
     * offer (2 usable bits per R/G/B channel). Nothing was mutated.
     */
    data class CapacityExceeded(val requiredBits: Long, val availableBits: Long) : StegoEncodeResult
}
```

- [ ] **Step 5: Run the test — expect PASS.**

```bash
./gradlew :steganography:jvmTest --tests "com.slothiesmooth.nyx.steganography.PixelImageTest"
```

Expected: `BUILD SUCCESSFUL`, both tests pass.

- [ ] **Step 6: Commit.**

```bash
git add steganography/src/commonMain steganography/src/commonTest
git commit -m "feat(steganography): PixelImage and StegoEncodeResult types"
```

---

### Task 3: `Steganography` — LSB encode/decode (TDD)

The engine is a single cohesive bit-packing algorithm. It is built in two genuine red-green cycles:
the **capacity check + full encoder** is driven by the capacity test, then the **decoder** is driven
by the round-trip test. The remaining acceptance tests (spanning, no-marker, boundary, LSB-delta,
alpha, unicode, empty) run against the finished engine and are grouped into two commits.

**Files:**
- Test: `steganography/src/commonTest/kotlin/com/slothiesmooth/nyx/steganography/SteganographyTest.kt`
- Create: `steganography/src/commonMain/kotlin/com/slothiesmooth/nyx/steganography/Steganography.kt`

**Interfaces:**
- Consumes: `PixelImage`, `StegoEncodeResult` (Task 2).
- Produces (00-INDEX contract):
  - `class Steganography(private val startMarker: String = "@!#", private val endMarker: String = "#!@")`
  - `suspend fun encode(images: List<PixelImage>, payload: String): StegoEncodeResult`
  - `suspend fun decode(images: List<PixelImage>): String?`  (null = no framed payload found)

**Steps:**

- [ ] **Step 1: Write the failing capacity test.** Create `SteganographyTest.kt`:

```kotlin
package com.slothiesmooth.nyx.steganography

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SteganographyTest {

    @Test
    fun `encode reports required and available bits when the cover is too small`() = runTest {
        val stego = Steganography()
        // 2x2 cover = 4 px * 3 channels = 12 channels -> 24 usable bits.
        val cover = solidImage(width = 2, height = 2, argb = 0xFF000000.toInt())
        // frame "@!#" + "X" + "#!@" = 7 bytes -> 56 required bits.
        val exceeded = assertIs<StegoEncodeResult.CapacityExceeded>(stego.encode(listOf(cover), "X"))
        assertEquals(56L, exceeded.requiredBits)
        assertEquals(24L, exceeded.availableBits)
    }
}

private fun solidImage(width: Int, height: Int, argb: Int): PixelImage =
    PixelImage(width, height, IntArray(width * height) { argb })
```

- [ ] **Step 2: Run it — expect a compile failure.**

```bash
./gradlew :steganography:compileTestKotlinJvm
```

Expected: `> Task :steganography:compileTestKotlinJvm FAILED` with
`Unresolved reference: Steganography`.

- [ ] **Step 3: Write the encoder (capacity check + full embedding, no decoder yet).** Create
  `Steganography.kt`:

```kotlin
package com.slothiesmooth.nyx.steganography

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

private const val BITS_PER_CHANNEL = 2
private const val BITS_PER_BYTE = 8
private const val CHANNELS_PER_PIXEL = 3
private const val CHUNKS_PER_BYTE = 4
private const val CHUNK_MASK = 0x3
private const val CHANNEL_MASK = 0xFF
private const val CLEAR_LOW_TWO_BITS = 0xFC
private const val RED_SLOT = 0
private const val GREEN_SLOT = 1
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BLUE_SHIFT = 0

/**
 * Hides a UTF-8 payload in the low 2 bits of each R/G/B channel (6 bits per pixel), framed with
 * [startMarker] / [endMarker], optionally spanning multiple covers. Alpha is never touched.
 */
class Steganography(
    private val startMarker: String = "@!#",
    private val endMarker: String = "#!@",
) {

    suspend fun encode(images: List<PixelImage>, payload: String): StegoEncodeResult {
        coroutineContext.ensureActive()
        val frameBytes = buildFrame(payload)
        val requiredBits = frameBytes.size.toLong() * BITS_PER_BYTE
        val availableBits = channelCount(images) * BITS_PER_CHANNEL
        if (requiredBits > availableBits) {
            return StegoEncodeResult.CapacityExceeded(requiredBits, availableBits)
        }
        return StegoEncodeResult.Success(embed(images, frameBytes))
    }

    private fun buildFrame(payload: String): ByteArray =
        (startMarker + payload + endMarker).encodeToByteArray()

    private fun channelCount(images: List<PixelImage>): Long =
        images.sumOf { it.width.toLong() * it.height.toLong() * CHANNELS_PER_PIXEL }

    private fun embed(images: List<PixelImage>, frameBytes: ByteArray): List<PixelImage> {
        val totalChunks = frameBytes.size * CHUNKS_PER_BYTE
        var chunkIndex = 0
        return images.map { image ->
            val pixels = image.pixels.copyOf()
            val channels = image.width * image.height * CHANNELS_PER_PIXEL
            var localChannel = 0
            while (chunkIndex < totalChunks && localChannel < channels) {
                val pixelIndex = localChannel / CHANNELS_PER_PIXEL
                val colorSlot = localChannel % CHANNELS_PER_PIXEL
                pixels[pixelIndex] =
                    writeChunk(pixels[pixelIndex], colorSlot, chunkAt(frameBytes, chunkIndex))
                chunkIndex++
                localChannel++
            }
            PixelImage(image.width, image.height, pixels)
        }
    }

    private fun chunkAt(frameBytes: ByteArray, chunkIndex: Int): Int {
        val byteIndex = chunkIndex / CHUNKS_PER_BYTE
        val subIndex = chunkIndex % CHUNKS_PER_BYTE
        val bitOffset = (CHUNKS_PER_BYTE - 1 - subIndex) * BITS_PER_CHANNEL
        return ((frameBytes[byteIndex].toInt() and CHANNEL_MASK) ushr bitOffset) and CHUNK_MASK
    }

    private fun writeChunk(pixel: Int, colorSlot: Int, chunk: Int): Int {
        val shift = channelShift(colorSlot)
        val channelValue = (pixel ushr shift) and CHANNEL_MASK
        val updatedChannel = (channelValue and CLEAR_LOW_TWO_BITS) or chunk
        val clearMask = (CHANNEL_MASK shl shift).inv()
        return (pixel and clearMask) or (updatedChannel shl shift)
    }

    private fun channelShift(colorSlot: Int): Int = when (colorSlot) {
        RED_SLOT -> RED_SHIFT
        GREEN_SLOT -> GREEN_SHIFT
        else -> BLUE_SHIFT
    }
}
```

- [ ] **Step 4: Run the capacity test — expect PASS.**

```bash
./gradlew :steganography:jvmTest --tests "com.slothiesmooth.nyx.steganography.SteganographyTest"
```

Expected: `BUILD SUCCESSFUL`, the capacity test passes.

- [ ] **Step 5: Commit.**

```bash
git add steganography/src/commonMain steganography/src/commonTest
git commit -m "feat(steganography): up-front capacity check and LSB embedding"
```

- [ ] **Step 6: Write the failing round-trip test.** Append to `SteganographyTest.kt` (inside the
  class):

```kotlin
    @Test
    fun `round trips a payload through a single cover`() = runTest {
        val stego = Steganography()
        val cover = solidImage(width = 64, height = 64, argb = 0xFF3366AA.toInt())
        val secret = "Hello, Nyx!"
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), secret))
        assertEquals(secret, stego.decode(result.images))
    }
```

- [ ] **Step 7: Run it — expect a compile failure.**

```bash
./gradlew :steganography:compileTestKotlinJvm
```

Expected: `> Task :steganography:compileTestKotlinJvm FAILED` with
`Unresolved reference: decode`.

- [ ] **Step 8: Add the decoder.** Add these members to the `Steganography` class in
  `Steganography.kt` (place `decode` right after `encode`, and the private helpers after
  `channelShift`):

```kotlin
    suspend fun decode(images: List<PixelImage>): String? {
        coroutineContext.ensureActive()
        return extractFrameBytes(images)?.decodeToString()
    }
```

```kotlin
    private fun extractFrameBytes(images: List<PixelImage>): ByteArray? {
        val startBytes = startMarker.encodeToByteArray()
        val endBytes = endMarker.encodeToByteArray()
        val decoded = ArrayList<Byte>()
        val totalChannels = channelCount(images)
        var partialByte = 0
        var chunkCount = 0
        var channel = 0L
        var frame: ByteArray? = null
        while (channel < totalChannels && frame == null) {
            partialByte = (partialByte shl BITS_PER_CHANNEL) or chunkAtChannel(images, channel)
            chunkCount++
            if (chunkCount == CHUNKS_PER_BYTE) {
                decoded.add(partialByte.toByte())
                partialByte = 0
                chunkCount = 0
                if (markerBroken(decoded, startBytes)) return null
                frame = completedFrame(decoded, startBytes, endBytes)
            }
            channel++
        }
        return frame
    }

    private fun markerBroken(decoded: List<Byte>, startBytes: ByteArray): Boolean =
        decoded.size == startBytes.size && !startsWithBytes(decoded, startBytes)

    private fun completedFrame(
        decoded: List<Byte>,
        startBytes: ByteArray,
        endBytes: ByteArray,
    ): ByteArray? {
        val minFrameSize = startBytes.size + endBytes.size
        return if (decoded.size >= minFrameSize && endsWithBytes(decoded, endBytes)) {
            decoded.subList(startBytes.size, decoded.size - endBytes.size).toByteArray()
        } else {
            null
        }
    }

    private fun chunkAtChannel(images: List<PixelImage>, globalChannel: Long): Int {
        var remaining = globalChannel
        for (image in images) {
            val channels = image.width.toLong() * image.height.toLong() * CHANNELS_PER_PIXEL
            if (remaining < channels) {
                val pixelIndex = (remaining / CHANNELS_PER_PIXEL).toInt()
                val colorSlot = (remaining % CHANNELS_PER_PIXEL).toInt()
                return readChunk(image.pixels[pixelIndex], colorSlot)
            }
            remaining -= channels
        }
        return 0
    }

    private fun readChunk(pixel: Int, colorSlot: Int): Int =
        (pixel ushr channelShift(colorSlot)) and CHUNK_MASK

    private fun startsWithBytes(data: List<Byte>, prefix: ByteArray): Boolean {
        if (data.size < prefix.size) return false
        return prefix.indices.all { data[it] == prefix[it] }
    }

    private fun endsWithBytes(data: List<Byte>, suffix: ByteArray): Boolean {
        if (data.size < suffix.size) return false
        val offset = data.size - suffix.size
        return suffix.indices.all { data[offset + it] == suffix[it] }
    }
```

- [ ] **Step 9: Run the round-trip test — expect PASS.**

```bash
./gradlew :steganography:jvmTest --tests "com.slothiesmooth.nyx.steganography.SteganographyTest"
```

Expected: `BUILD SUCCESSFUL`; capacity and round-trip tests pass.

- [ ] **Step 10: Commit.**

```bash
git add steganography/src/commonMain steganography/src/commonTest
git commit -m "feat(steganography): byte-level marker-framed LSB decoder"
```

- [ ] **Step 11: Add framing/capacity acceptance tests.** Append to `SteganographyTest.kt` (inside
  the class). These exercise the finished engine — expected to pass on arrival:

```kotlin
    @Test
    fun `spans a payload across multiple covers`() = runTest {
        val stego = Steganography()
        // Each cover: 6x2 = 36 channels = 9 frame-byte capacity.
        // frame "@!#" + "SPANNING" + "#!@" = 14 bytes -> needs two covers.
        val first = solidImage(width = 6, height = 2, argb = 0xFF101010.toInt())
        val second = solidImage(width = 6, height = 2, argb = 0xFF202020.toInt())
        val secret = "SPANNING"
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(first, second), secret))
        assertEquals(2, result.images.size)
        assertEquals(secret, stego.decode(result.images))
        // The first cover alone cannot hold the whole frame -> no end marker -> null.
        assertNull(stego.decode(listOf(result.images.first())))
    }

    @Test
    fun `returns null when no framed payload is present`() = runTest {
        val stego = Steganography()
        // Low 2 bits are 0 -> decoded bytes are 0x00, never the start marker.
        val plain = solidImage(width = 16, height = 16, argb = 0xFF000000.toInt())
        assertNull(stego.decode(listOf(plain)))
    }

    @Test
    fun `fills a cover exactly at capacity`() = runTest {
        val stego = Steganography()
        // 6x2 = 36 channels = 72 usable bits = 9 frame bytes.
        // frame "@!#" + "ABC" + "#!@" = 9 bytes -> exact fit.
        val cover = solidImage(width = 6, height = 2, argb = 0xFF204060.toInt())
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), "ABC"))
        assertEquals("ABC", stego.decode(result.images))
    }

    @Test
    fun `rejects a payload one byte over capacity`() = runTest {
        val stego = Steganography()
        val cover = solidImage(width = 6, height = 2, argb = 0xFF204060.toInt())
        // frame "@!#" + "ABCD" + "#!@" = 10 bytes -> 80 required bits > 72 available.
        val exceeded =
            assertIs<StegoEncodeResult.CapacityExceeded>(stego.encode(listOf(cover), "ABCD"))
        assertEquals(80L, exceeded.requiredBits)
        assertEquals(72L, exceeded.availableBits)
    }
```

- [ ] **Step 12: Run the suite — expect PASS.**

```bash
./gradlew :steganography:jvmTest --tests "com.slothiesmooth.nyx.steganography.SteganographyTest"
```

Expected: `BUILD SUCCESSFUL`; all six tests pass.

- [ ] **Step 13: Commit.**

```bash
git add steganography/src/commonTest
git commit -m "test(steganography): spanning, no-marker, and capacity-boundary cases"
```

- [ ] **Step 14: Add fidelity acceptance tests (LSB delta, alpha, unicode, empty).** Append to
  `SteganographyTest.kt` (inside the class):

```kotlin
    @Test
    fun `changes only the low two bits of each RGB channel`() = runTest {
        val stego = Steganography()
        val cover = solidImage(width = 32, height = 32, argb = 0xFF7F7F7F.toInt())
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), "delta check"))
        val output = result.images.single()
        // Alpha + the high 6 bits of R, G, B must be byte-identical to the cover.
        val preserveMask = 0xFFFCFCFC.toInt()
        cover.pixels.indices.forEach { index ->
            assertEquals(
                cover.pixels[index] and preserveMask,
                output.pixels[index] and preserveMask,
                "pixel $index changed outside the low 2 bits of R/G/B",
            )
        }
    }

    @Test
    fun `never modifies the alpha channel`() = runTest {
        val stego = Steganography()
        // Semi-transparent cover: the algorithm must neither touch nor force alpha opaque.
        val cover = solidImage(width = 32, height = 32, argb = 0x80112233.toInt())
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), "keep alpha"))
        result.images.single().pixels.forEach { pixel ->
            assertEquals(0x80, (pixel ushr 24) and 0xFF, "alpha byte must stay 0x80")
        }
    }

    @Test
    fun `round trips a unicode payload`() = runTest {
        val stego = Steganography()
        val cover = solidImage(width = 64, height = 64, argb = 0xFF446688.toInt())
        val secret = "Rendezvous 🦊🔒 at 07:30"
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), secret))
        assertEquals(secret, stego.decode(result.images))
    }

    @Test
    fun `round trips an empty payload`() = runTest {
        val stego = Steganography()
        val cover = solidImage(width = 8, height = 8, argb = 0xFF123456.toInt())
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), ""))
        assertEquals("", stego.decode(result.images))
    }
```

- [ ] **Step 15: Run the full suite — expect PASS.**

```bash
./gradlew :steganography:jvmTest --tests "com.slothiesmooth.nyx.steganography.SteganographyTest"
```

Expected: `BUILD SUCCESSFUL`; all ten `SteganographyTest` tests pass.

- [ ] **Step 16: Commit.**

```bash
git add steganography/src/commonTest
git commit -m "test(steganography): LSB-delta, alpha-preservation, unicode, and empty payload"
```

---

### Task 4: `:steganography` cross-target verification (jvm + android host + wasm)

**Files:** none (verification only).

**Interfaces:** Consumes the complete `:steganography` module (Tasks 1-3).

**Steps:**

- [ ] **Step 1: detekt clean.** Run:

```bash
./gradlew :steganography:detekt
```

Expected: `BUILD SUCCESSFUL` with zero issues. If any issue appears, fix the root cause (extract a
constant, split a function) — never add a `@Suppress`.

- [ ] **Step 2: jvm tests.** Run:

```bash
./gradlew :steganography:jvmTest
```

Expected: `BUILD SUCCESSFUL`; `PixelImageTest` + `SteganographyTest` all green.

- [ ] **Step 3: Discover and run the android host test task.** Run:

```bash
./gradlew :steganography:tasks --all | grep -i "hosttest"
```

Expected: a task name such as `testAndroidHostTest`. Run whatever it prints, e.g.:

```bash
./gradlew :steganography:testAndroidHostTest
```

Expected: `BUILD SUCCESSFUL`; the same `commonTest` suite runs compiled against the android target
on the JVM host (no emulator).

- [ ] **Step 4: wasm tests in headless Chrome.** With Chrome installed (CI installs it; locally,
  skip this step if you have no Chrome), run:

```bash
./gradlew :steganography:wasmJsBrowserTest
```

Expected: `BUILD SUCCESSFUL`; the suite runs in headless Chrome. If Chrome is absent locally you may
see a Karma "no binary for ChromeHeadless" error — that is an environment gap, not a code failure;
CI covers it.

- [ ] **Step 5: No commit** (verification only). If Steps 1-4 all pass, `:steganography` is done.

---

### Task 5: `:crypto` module wiring and build file

**Files:**
- Modify: `settings.gradle.kts` (verify/add `include(":crypto")`)
- Modify: `gradle/libs.versions.toml` (add cryptography-kotlin entries)
- Create: `crypto/build.gradle.kts`
- Create: `crypto/src/commonMain/kotlin/com/slothiesmooth/nyx/crypto/` and `crypto/src/commonTest/kotlin/com/slothiesmooth/nyx/crypto/` (empty source dirs; real files land in Task 6). Empty dirs are not tracked by git — they get committed once Task 6 adds files, so no `.gitkeep` is needed.

**Interfaces:**
- Consumes: plan 01 catalog/wrapper, Task 1's coroutines/kotlin-test entries, `:steganography` (test-only, wired here).
- Produces: a compiling, empty `:crypto` KMP module (6 targets) for Tasks 6-9.

**Steps:**

- [ ] **Step 1: Add cryptography-kotlin to the catalog.** In `gradle/libs.versions.toml`, verify/add:

```toml
[versions]
cryptography = "0.6.0"

[libraries]
cryptography-core = { module = "dev.whyoleg.cryptography:cryptography-core", version.ref = "cryptography" }
cryptography-provider-optimal = { module = "dev.whyoleg.cryptography:cryptography-provider-optimal", version.ref = "cryptography" }
```

(`kotlin-test`, `kotlinx-coroutines-core`, `kotlinx-coroutines-test`, the two plugin aliases, and
`android-compileSdk`/`android-minSdk` were added in Task 1 Step 1 — reuse them.)

- [ ] **Step 2: Verify/register the module.** Run:

```bash
grep -n '":crypto"' settings.gradle.kts
```

Expected: a line inside `include(...)` containing `":crypto"`. If missing, add it.

- [ ] **Step 3: Create the source directory placeholder.**

```bash
mkdir -p crypto/src/commonMain/kotlin/com/slothiesmooth/nyx/crypto
mkdir -p crypto/src/commonTest/kotlin/com/slothiesmooth/nyx/crypto
```

- [ ] **Step 4: Write the build file.** Create `crypto/build.gradle.kts` (again, a plan 01 convention
  plugin producing the same effective config may be applied instead):

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.slothiesmooth.nyx.crypto"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTestBuilder { }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm()
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.io.encoding.ExperimentalEncodingApi")
            }
        }
        commonMain.dependencies {
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            // Test-only: proves the crypto -> stego -> crypto pipeline (Task 8). Keeps :crypto
            // production code free of any :steganography dependency, so both stay independent engines.
            implementation(projects.steganography)
        }
    }
}
```

iOS note: do NOT add `cryptography-provider-openssl3-prebuilt` (or anything) to `iosMain` — the
`optimal` provider covers AES-GCM (CryptoKit) and PBKDF2 (CommonCrypto) on Apple. iOS is verified on
macOS later; never gate Linux progress on it.

- [ ] **Step 5: Verify the empty module compiles.** Run:

```bash
./gradlew :crypto:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL` (cryptography-kotlin + coroutines aliases resolve, targets configure).

- [ ] **Step 6: Commit.**

```bash
git add settings.gradle.kts gradle/libs.versions.toml crypto/build.gradle.kts
git commit -m "build(crypto): KMP module wiring for cryptography-kotlin AES-GCM engine"
```

---

### Task 6: `NyxCrypto` interface, `DecryptResult`, and constants

**Files:**
- Create: `crypto/src/commonMain/kotlin/com/slothiesmooth/nyx/crypto/DecryptResult.kt`
- Create: `crypto/src/commonMain/kotlin/com/slothiesmooth/nyx/crypto/NyxCrypto.kt`
- Create: `crypto/src/commonMain/kotlin/com/slothiesmooth/nyx/crypto/CryptoConstants.kt`

**Interfaces:**
- Consumes: Task 5 module.
- Produces (00-INDEX contract): `sealed interface DecryptResult { Success(plaintext), WrongPasswordOrTampered, Failure(reason) }`; `interface NyxCrypto { suspend encrypt(plaintext, password): String; suspend decrypt(blob, password): DecryptResult }`; module-internal constants `SALT_SIZE_BYTES = 16`, `PBKDF2_ITERATIONS = 600_000`, `KEY_SIZE_BITS = 256` (+ `NONCE_SIZE_BYTES`, `GCM_TAG_SIZE_BYTES`, `MIN_BLOB_SIZE_BYTES`, failure-reason strings).

**Steps:**

- [ ] **Step 1: Create `DecryptResult.kt`.**

```kotlin
package com.slothiesmooth.nyx.crypto

/** Result of [NyxCrypto.decrypt]. */
sealed interface DecryptResult {

    /** The blob decrypted and authenticated; [plaintext] is the recovered message. */
    data class Success(val plaintext: String) : DecryptResult

    /** The GCM tag failed: wrong password, or the blob was altered after encryption. */
    data object WrongPasswordOrTampered : DecryptResult

    /** The blob could not be parsed (bad Base64, or too short to contain salt + nonce + tag). */
    data class Failure(val reason: String) : DecryptResult
}
```

- [ ] **Step 2: Create `NyxCrypto.kt`.**

```kotlin
package com.slothiesmooth.nyx.crypto

/**
 * Authenticated encryption for Nyx. Produces and consumes a self-describing Base64 blob that the
 * steganography engine hides inside an image.
 *
 * Both operations are `suspend` because the wasmJs provider (WebCrypto) is asynchronous.
 */
interface NyxCrypto {

    /** Returns `Base64(salt(16) || iv(12) || ciphertext || tag(16))`. */
    suspend fun encrypt(plaintext: String, password: String): String

    suspend fun decrypt(blob: String, password: String): DecryptResult
}
```

- [ ] **Step 3: Create `CryptoConstants.kt`.** (`internal const val` so `DefaultNyxCrypto` and the
  `commonTest` suite in the same module can reference them; `const` declarations are exempt from
  detekt `MagicNumber`.)

```kotlin
package com.slothiesmooth.nyx.crypto

internal const val SALT_SIZE_BYTES = 16
internal const val NONCE_SIZE_BYTES = 12
internal const val GCM_TAG_SIZE_BYTES = 16
internal const val PBKDF2_ITERATIONS = 600_000
internal const val KEY_SIZE_BITS = 256
internal const val MIN_BLOB_SIZE_BYTES = SALT_SIZE_BYTES + NONCE_SIZE_BYTES + GCM_TAG_SIZE_BYTES

internal const val REASON_MALFORMED_BASE64 = "blob is not valid Base64"
internal const val REASON_TOO_SHORT = "blob is shorter than salt + nonce + tag (44 bytes)"
```

- [ ] **Step 4: Verify it compiles.** Run:

```bash
./gradlew :crypto:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit.**

```bash
git add crypto/src/commonMain
git commit -m "feat(crypto): NyxCrypto contract, DecryptResult, and crypto constants"
```

---

### Task 7: `DefaultNyxCrypto` implementation and acceptance suite (TDD)

The round-trip test drives the implementation (red → green); the rest of the spec §4 suite plus the
extra cases (empty/unicode plaintext, empty password, malformed/too-short blob) run against the
finished class and are grouped into two commits.

**Files:**
- Test: `crypto/src/commonTest/kotlin/com/slothiesmooth/nyx/crypto/DefaultNyxCryptoTest.kt`
- Create: `crypto/src/commonMain/kotlin/com/slothiesmooth/nyx/crypto/DefaultNyxCrypto.kt`

**Interfaces:**
- Consumes: `NyxCrypto`, `DecryptResult`, constants (Task 6); cryptography-kotlin 0.6.0.
- Produces (00-INDEX contract): `class DefaultNyxCrypto(private val provider: CryptographyProvider = CryptographyProvider.Default) : NyxCrypto`.

**Steps:**

- [ ] **Step 1: Write the failing round-trip test.** Create `DefaultNyxCryptoTest.kt`:

```kotlin
package com.slothiesmooth.nyx.crypto

import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DefaultNyxCryptoTest {

    @Test
    fun `round trips a plaintext with the correct password`() = runTest {
        val crypto = DefaultNyxCrypto()
        val blob = crypto.encrypt("attack at dawn", "correct horse")
        assertEquals(DecryptResult.Success("attack at dawn"), crypto.decrypt(blob, "correct horse"))
    }
}
```

- [ ] **Step 2: Run it — expect a compile failure.**

```bash
./gradlew :crypto:compileTestKotlinJvm
```

Expected: `> Task :crypto:compileTestKotlinJvm FAILED` with `Unresolved reference: DefaultNyxCrypto`.

- [ ] **Step 3: Implement `DefaultNyxCrypto.kt`.** (The library key/cipher types are never named
  explicitly — expression-body helpers infer them — so this compiles regardless of 0.6.0's exact
  type names, as long as the verified call chain is correct.)

```kotlin
package com.slothiesmooth.nyx.crypto

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.io.encoding.Base64

/**
 * AES-256-GCM authenticated encryption with a PBKDF2-HMAC-SHA256 (600k) derived key.
 *
 * Blob = `Base64(salt(16) || cipher.encrypt(plaintext))`, where the library's default
 * `cipher.encrypt` prepends a fresh 12-byte IV: `iv(12) || ciphertext || tag(16)`.
 */
class DefaultNyxCrypto(
    private val provider: CryptographyProvider = CryptographyProvider.Default,
) : NyxCrypto {

    override suspend fun encrypt(plaintext: String, password: String): String {
        val salt = CryptographyRandom.nextBytes(SALT_SIZE_BYTES)
        val cipherOutput = cipherFor(password, salt).encrypt(plaintext.encodeToByteArray())
        return Base64.Default.encode(salt + cipherOutput)
    }

    override suspend fun decrypt(blob: String, password: String): DecryptResult {
        val raw = decodeBlob(blob)
        return if (raw == null || raw.size < MIN_BLOB_SIZE_BYTES) {
            DecryptResult.Failure(failureReason(raw))
        } else {
            val salt = raw.copyOfRange(0, SALT_SIZE_BYTES)
            val cipherOutput = raw.copyOfRange(SALT_SIZE_BYTES, raw.size)
            runDecrypt(password, salt, cipherOutput)
        }
    }

    private suspend fun runDecrypt(
        password: String,
        salt: ByteArray,
        cipherOutput: ByteArray,
    ): DecryptResult {
        val cipher = cipherFor(password, salt)
        val attempt = runCatching { cipher.decrypt(cipherOutput).decodeToString() }
        // Re-throw if the coroutine was cancelled during decrypt (runCatching also catches
        // CancellationException); any other failure is a wrong password or a tampered blob.
        coroutineContext.ensureActive()
        return attempt.fold(
            onSuccess = { DecryptResult.Success(it) },
            onFailure = { DecryptResult.WrongPasswordOrTampered },
        )
    }

    private suspend fun cipherFor(password: String, salt: ByteArray) =
        provider.get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, derivedKeyBytes(password, salt))
            .cipher()

    private suspend fun derivedKeyBytes(password: String, salt: ByteArray): ByteArray =
        provider.get(PBKDF2)
            .secretDerivation(
                digest = SHA256,
                iterations = PBKDF2_ITERATIONS,
                outputSize = KEY_SIZE_BITS.bits,
                salt = salt,
            )
            .deriveSecretToByteArray(password.encodeToByteArray())

    private fun decodeBlob(blob: String): ByteArray? =
        runCatching { Base64.Default.decode(blob) }.getOrNull()

    private fun failureReason(raw: ByteArray?): String =
        if (raw == null) REASON_MALFORMED_BASE64 else REASON_TOO_SHORT
}
```

- [ ] **Step 4: Run the round-trip test — expect PASS.**

```bash
./gradlew :crypto:jvmTest --tests "com.slothiesmooth.nyx.crypto.DefaultNyxCryptoTest"
```

Expected: `BUILD SUCCESSFUL`. If instead you get an unresolved-reference on a cryptography-kotlin
symbol (e.g. `keyDecoder`/`secretDerivation`/`AES.Key.Format.RAW`), the 0.6.0 API differs from the
00-INDEX verified snippet — reconcile against `https://whyoleg.github.io/cryptography-kotlin/` and
keep the exact call chain from "Context" above; do not change the algorithm parameters.

- [ ] **Step 5: Commit.**

```bash
git add crypto/src/commonMain crypto/src/commonTest
git commit -m "feat(crypto): DefaultNyxCrypto AES-GCM + PBKDF2 implementation"
```

- [ ] **Step 6: Add the security acceptance tests (spec §4).** Append to `DefaultNyxCryptoTest.kt`
  (inside the class):

```kotlin
    @Test
    fun `reports wrong password without decrypting`() = runTest {
        val crypto = DefaultNyxCrypto()
        val blob = crypto.encrypt("attack at dawn", "correct horse")
        assertEquals(DecryptResult.WrongPasswordOrTampered, crypto.decrypt(blob, "wrong horse"))
    }

    @Test
    fun `detects a single-byte tamper in the ciphertext region`() = runTest {
        val crypto = DefaultNyxCrypto()
        val blob = crypto.encrypt("attack at dawn", "correct horse")
        val raw = Base64.Default.decode(blob)
        // First byte past salt(16) + nonce(12): the ciphertext-or-tag region.
        val tamperIndex = SALT_SIZE_BYTES + NONCE_SIZE_BYTES
        raw[tamperIndex] = (raw[tamperIndex].toInt() xor 0xFF).toByte()
        val tampered = Base64.Default.encode(raw)
        assertEquals(DecryptResult.WrongPasswordOrTampered, crypto.decrypt(tampered, "correct horse"))
    }

    @Test
    fun `produces a different blob each time for the same input`() = runTest {
        val crypto = DefaultNyxCrypto()
        val first = crypto.encrypt("attack at dawn", "correct horse")
        val second = crypto.encrypt("attack at dawn", "correct horse")
        assertNotEquals(first, second)
        assertEquals(DecryptResult.Success("attack at dawn"), crypto.decrypt(first, "correct horse"))
        assertEquals(DecryptResult.Success("attack at dawn"), crypto.decrypt(second, "correct horse"))
    }

    @Test
    fun `blob decodes to at least salt plus nonce plus tag bytes`() = runTest {
        val crypto = DefaultNyxCrypto()
        val raw = Base64.Default.decode(crypto.encrypt("", "correct horse"))
        assertTrue(raw.size >= MIN_BLOB_SIZE_BYTES, "expected >= $MIN_BLOB_SIZE_BYTES, got ${raw.size}")
    }
```

- [ ] **Step 7: Run — expect PASS.**

```bash
./gradlew :crypto:jvmTest --tests "com.slothiesmooth.nyx.crypto.DefaultNyxCryptoTest"
```

Expected: `BUILD SUCCESSFUL`; five tests green.

- [ ] **Step 8: Commit.**

```bash
git add crypto/src/commonTest
git commit -m "test(crypto): wrong-password, tamper, non-determinism, blob-length"
```

- [ ] **Step 9: Add the edge-case acceptance tests.** Append to `DefaultNyxCryptoTest.kt` (inside the
  class):

```kotlin
    @Test
    fun `round trips an empty plaintext`() = runTest {
        val crypto = DefaultNyxCrypto()
        val blob = crypto.encrypt("", "correct horse")
        assertEquals(DecryptResult.Success(""), crypto.decrypt(blob, "correct horse"))
    }

    @Test
    fun `round trips a unicode plaintext`() = runTest {
        val crypto = DefaultNyxCrypto()
        val secret = "rendezvous 🦊🔒 07:30 — café"
        val blob = crypto.encrypt(secret, "correct horse")
        assertEquals(DecryptResult.Success(secret), crypto.decrypt(blob, "correct horse"))
    }

    @Test
    fun `treats an empty password as a valid user choice`() = runTest {
        // Documented behavior: empty passwords are the user's responsibility, not rejected here.
        val crypto = DefaultNyxCrypto()
        val blob = crypto.encrypt("secret", "")
        assertEquals(DecryptResult.Success("secret"), crypto.decrypt(blob, ""))
        assertEquals(DecryptResult.WrongPasswordOrTampered, crypto.decrypt(blob, "not empty"))
    }

    @Test
    fun `fails cleanly on a non-Base64 blob`() = runTest {
        val crypto = DefaultNyxCrypto()
        val result = crypto.decrypt("this is not base64 @@@", "correct horse")
        assertEquals(REASON_MALFORMED_BASE64, assertIs<DecryptResult.Failure>(result).reason)
    }

    @Test
    fun `fails cleanly on a blob that is too short`() = runTest {
        val crypto = DefaultNyxCrypto()
        // Valid Base64 but only 10 bytes -> below the 44-byte minimum.
        val shortBlob = Base64.Default.encode(ByteArray(10))
        val result = crypto.decrypt(shortBlob, "correct horse")
        assertEquals(REASON_TOO_SHORT, assertIs<DecryptResult.Failure>(result).reason)
    }
```

- [ ] **Step 10: Run the full class — expect PASS.**

```bash
./gradlew :crypto:jvmTest --tests "com.slothiesmooth.nyx.crypto.DefaultNyxCryptoTest"
```

Expected: `BUILD SUCCESSFUL`; all ten `DefaultNyxCryptoTest` tests green.

- [ ] **Step 11: Commit.**

```bash
git add crypto/src/commonTest
git commit -m "test(crypto): empty/unicode plaintext, empty password, malformed and short blobs"
```

---

### Task 8: crypto ↔ steganography integration test

**Files:**
- Test: `crypto/src/commonTest/kotlin/com/slothiesmooth/nyx/crypto/CryptoStegoIntegrationTest.kt`

**Interfaces:**
- Consumes: `DefaultNyxCrypto`, `DecryptResult` (this module); `Steganography`, `PixelImage`, `StegoEncodeResult` (`:steganography`, test-only dependency wired in Task 5 Step 4).
- Produces: proof that `encrypt → encode → decode → decrypt` recovers the original message.

**Steps:**

- [ ] **Step 1: Write the integration test.** Create `CryptoStegoIntegrationTest.kt`:

```kotlin
package com.slothiesmooth.nyx.crypto

import com.slothiesmooth.nyx.steganography.PixelImage
import com.slothiesmooth.nyx.steganography.Steganography
import com.slothiesmooth.nyx.steganography.StegoEncodeResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class CryptoStegoIntegrationTest {

    @Test
    fun `encrypt then hide then reveal then decrypt recovers the original message`() = runTest {
        val crypto = DefaultNyxCrypto()
        val stego = Steganography()
        val secret = "Meet at the north pier 🌊 at 04:15"
        val password = "correct horse battery staple"

        val blob = crypto.encrypt(secret, password)
        // The Base64 blob alphabet excludes the marker characters, so no false end-marker match.
        val cover = PixelImage(width = 96, height = 96, pixels = IntArray(96 * 96) { 0xFF335577.toInt() })

        val encoded = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), blob))
        val revealed = stego.decode(encoded.images)
        assertNotNull(revealed)
        assertEquals(blob, revealed)
        assertEquals(DecryptResult.Success(secret), crypto.decrypt(revealed, password))
    }
}
```

- [ ] **Step 2: Run it — expect PASS.**

```bash
./gradlew :crypto:jvmTest --tests "com.slothiesmooth.nyx.crypto.CryptoStegoIntegrationTest"
```

Expected: `BUILD SUCCESSFUL`. (If `:steganography` types are unresolved, confirm Task 5 Step 4 added
`implementation(projects.steganography)` to `commonTest.dependencies`.)

- [ ] **Step 3: Commit.**

```bash
git add crypto/src/commonTest
git commit -m "test(crypto): end-to-end integration with the steganography engine"
```

---

### Task 9: `:crypto` cross-target verification and the wasm PBKDF2 note

**Files:** none (verification only).

**Interfaces:** Consumes the complete `:crypto` module (Tasks 5-8).

**Steps:**

- [ ] **Step 1: detekt clean.** Run:

```bash
./gradlew :crypto:detekt
```

Expected: `BUILD SUCCESSFUL`, zero issues. If `TooGenericExceptionCaught` or
`SuspendFunSwallowedCancellation` fires, do not suppress — confirm `runDecrypt` uses `runCatching`
plus `coroutineContext.ensureActive()` exactly as written (that is the sanctioned pattern).

- [ ] **Step 2: jvm tests.** Run:

```bash
./gradlew :crypto:jvmTest
```

Expected: `BUILD SUCCESSFUL`; `DefaultNyxCryptoTest` (10) + `CryptoStegoIntegrationTest` (1) green.

- [ ] **Step 3: Discover and run the android host test task.** Run:

```bash
./gradlew :crypto:tasks --all | grep -i "hosttest"
```

Expected: a task like `testAndroidHostTest`. Run it, e.g.:

```bash
./gradlew :crypto:testAndroidHostTest
```

Expected: `BUILD SUCCESSFUL`; the same suite runs against the android target on the JVM host, using
the JDK crypto provider selected by `cryptography-provider-optimal`.

- [ ] **Step 4: wasm tests in headless Chrome (PBKDF2 600k on WebCrypto).** With Chrome installed:

```bash
./gradlew :crypto:wasmJsBrowserTest
```

Expected: `BUILD SUCCESSFUL`. PBKDF2 at 600k iterations runs through the browser's **native**
WebCrypto `deriveBits`, so it completes fast (tens of ms) and well within kotlinx-coroutines-test's
`runTest` timeout — do NOT `@Ignore` this test. If Chrome is absent locally, the task errors with a
Karma launcher message (environment gap, not a code failure); CI installs Chrome and runs it. This
step also closes the crypto_migration_plan open question about wasm PBKDF2 latency: it is a native
WebCrypto call, not pure-Kotlin, so there is no test-time slowness. UX latency on a mid-range device
remains a product-level check for a later phase, not a blocker here.

- [ ] **Step 5: Full engine gate (both modules, one command).** Run:

```bash
./gradlew :crypto:jvmTest :steganography:jvmTest
```

Expected: `BUILD SUCCESSFUL`. Both engines are complete: `:steganography` (LSB, alpha-safe,
multi-image, up-front capacity check) and `:crypto` (AES-256-GCM + PBKDF2, sealed `DecryptResult`)
with full acceptance suites green on jvm and android host, and on wasm where Chrome is available.

- [ ] **Step 6: No commit** (verification only). Phase 02 is done when Tasks 1-9 are all green.
