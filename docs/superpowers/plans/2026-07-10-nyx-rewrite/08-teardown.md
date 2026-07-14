# Teardown & Docs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete every remnant of the legacy 2020-era Android tree so the repo contains ONLY the new Compose Multiplatform world, rewrite README/CONTRIBUTING for the new architecture, modernize the Renovate config, set the Android version identity, apply the final CI polish, and prove the final tree green end-to-end.

**Architecture:** This is a pure teardown-and-docs phase — no new Kotlin code. It removes the old `app/` + `buildSrc/` build tree and GitLab-era config with `git rm`, gates stray design-asset deletion behind an explicit user checkpoint, and finishes with the final CI polish plus a full-suite verification (build + Paparazzi verify + CI-parity run) on the final tree.

**Tech Stack:** git, Gradle 9.4.1, GitHub Actions (verification only), Renovate, Markdown.

## Global Constraints

Copied verbatim from `00-INDEX.md` (the whole index applies implicitly; these are the load-bearing lines for this phase):

- Kotlin 2.3.21, AGP 9.2.0, Gradle 9.4.1, Compose Multiplatform 1.10.3, JVM target 21,
  compileSdk 36, targetSdk 36, minSdk 24.
- KMP targets on every KMP module: `androidTarget` (via `com.android.kotlin.multiplatform.library`,
  configured as `kotlin { android {} }`), `iosX64`, `iosArm64`, `iosSimulatorArm64`, `jvm`,
  `wasmJs`. `applyDefaultHierarchyTemplate()`. iOS compiles only on macOS — never gate Linux
  progress on iOS; `kotlin.native.ignoreDisabledTargets=true`.
- US English in all identifiers/comments/docs/commits. No `@Suppress`-style gate-passers —
  the single documented exception: `NxColors.kt` may suppress MagicNumber (the one raw-ARGB file).
- Commit after every green test cycle (conventional commits).

Phase-specific constraints:

- **Entry gate:** execute this plan only after plan 07 is complete and `./gradlew build` is green
  on the pre-teardown tree with a clean working tree.
- **All deletions of tracked files go through `git rm`** (history-preserving), never bare `rm`.
- **The entire deletion list is confirmed with the user before deleting** (spec §12: "Entire
  list confirmed with user at the final phase before deleting") — Task 1 Step 3 presents the
  deletion inventory and waits for a go-ahead before any `git rm` runs.
- **Stray design assets** (`*.ai` files, root images) are deleted ONLY after explicit user
  approval via an AskUserQuestion checkpoint (Task 2). Never assume approval. Spec §12:
  "stray `.ai`/image design assets touched only with explicit approval."
- **`images/nyx-logo.png` must survive teardown** — the rewritten README (Task 4) references it
  by relative path.
- **Keep unchanged:** `LICENSE` (MIT, Copyright (c) 2020 TechnoWolf FOSS), `CODE_OF_CONDUCT.md`,
  `.editorconfig` (spec §3: kept).
- **`local.properties` is never committed** (verified in Task 1).

## Coordination note: the `steganography/` directory collision (READ FIRST)

The old repo has an Android-library module at `steganography/` (old layout: `steganography/src/main/...`).
The new `:steganography` KMP module (plan 02) claims the SAME path with the new layout
(`steganography/src/commonMain/...`). Two trees cannot occupy one directory, so this was resolved
at plan-set orchestration time:

- **Plan 01 (foundation) `git rm`s the old `steganography/`, `utils/`, and `feature_base/`
  contents immediately** — the new `:steganography` module needs the path, and `utils/` +
  `feature_base/` only exist to serve the old `app/`.
- **`app/` and `buildSrc/` survive on disk until THIS phase** (no path collision — no new module
  claims either path). Note: plan 01 also replaced the root `build.gradle.kts` (which loaded
  buildSrc's `local.detekt`/`local.spotless` plugins), so buildSrc has been build-inert since
  phase 01 even though the directory remained.

**REQUIREMENT ON PLAN 01 (cross-plan contract):** `01-foundation.md` MUST include the early
`git rm -r steganography utils feature_base` (before creating the new `:steganography` module
skeleton) and MUST leave `app/` and `buildSrc/` on disk for this phase. Verifying this is
MANDATORY before executing either plan — run:
```bash
grep -n 'git rm -r.*steganography' docs/superpowers/plans/2026-07-10-nyx-rewrite/01-foundation.md
```
Expected: at least one hit, located in a task that runs BEFORE the `:steganography` skeleton is
created (open the file and confirm the ordering). If the file does not exist yet or has no such
step, STOP and flag it to the user before executing either plan — the defensive STOP in Task 1
Step 2 below is a last-resort detection net, not a substitute for this check.

This plan is written **defensively**: every deletion uses `git rm --ignore-unmatch` plus an
existence check, so it succeeds regardless of exactly which leftovers phase 01 already removed.
Task 1 Step 2 additionally STOPS the run if old-layout sources are found where the new
`:steganography` module lives (that would mean the 01 coordination failed — escalate to the user
instead of deleting new code).

## Deletion inventory (what goes, what stays)

| Path | Action | When |
|---|---|---|
| `app/` | `git rm -r` | Task 1 |
| `buildSrc/` | `git rm -r` | Task 1 |
| `utils/`, `feature_base/`, old `steganography/` contents | already removed by plan 01; `--ignore-unmatch` safety net | Task 1 |
| `.gitlab-ci.yml` | `git rm` | Task 1 |
| `crypto_migration_plan.md` | `git rm` (superseded by spec + implementation) | Task 1 |
| `.github/workflows/check.yml` | `git rm` IF it is still the old content (references `konsist`/`:app:assembleDebug`); keep if plan 01 replaced it in place | Task 1 |
| root `detekt.yml` | `git rm` IF unreferenced by the new build; keep if plan 01 reuses the path | Task 1 |
| legacy `gradle.properties` keys (`accessKey`, `privateKey`, `kapt.*`, `enableJetifier`) | remove lines if plan 01 left any | Task 1 |
| `.github/stale.yml` (dead probot config) | `git rm` | Task 6 |
| `app-featured-image.ai`, `applogo-export.ai`, `applogo.ai`, `app-featured-image.jpg`, `applogo.png` | USER CHECKPOINT — delete only what the user approves | Task 2 |
| `images/nyx-logo.png` | **KEEP** (README dependency) | — |
| `LICENSE`, `CODE_OF_CONDUCT.md`, `.editorconfig`, `.gitignore` | **KEEP** | — |
| `README.md`, `CONTRIBUTING.md`, `renovate.json` | rewritten, not deleted | Tasks 4–6 |

## Decisions recorded in this plan

1. **Android version identity: `versionCode = 1`, `versionName = "1.0.0"`** (Task 3). Plain
   integer like pawdex — no `major*10000 + minor*100 + patch` formula. Rationale: single-store
   app, no multi-APK/ABI splits, Play only requires monotonic increase; a formula is complexity
   with zero payoff at v1. Bump manually per release.
2. **Renovate preset: `config:recommended`** (Task 6) — `config:base` is the deprecated alias.
   Renovate's `gradle` manager (enabled by the preset) natively reads
   `gradle/libs.versions.toml`, so the version catalog is covered with no extra config.
3. **FileKit is pinned below 0.14.0 via a Renovate `packageRules` entry** (Task 6) — 00-INDEX:
   FileKit 0.14.x is Kotlin-2.4-compiled and drops `iosX64`. Renovate JSON allows no comments;
   the rule carries a `description` field instead.
4. **CI badge / task-name substitution rule:** plan 01 owns the CI workflow filename and the
   aggregate lint task names. This plan assumes `ci.yml`, `detektCheck`, `spotlessCheck` (spec §3
   names) and includes verification steps that check the real names and substitute them in the
   docs if they differ. That check-and-substitute is part of the steps — do not skip it.

---

### Task 1: Legacy build-tree deletion

**Files:**
- Delete: `app/` (entire tree), `buildSrc/` (entire tree), `.gitlab-ci.yml`, `crypto_migration_plan.md`
- Delete (conditional): `.github/workflows/check.yml`, `detekt.yml`
- Modify (conditional): `gradle.properties`
- Test: `./gradlew build` on the post-deletion tree

**Interfaces:**
- Consumes: the complete new build from plans 01–07 — new root `build.gradle.kts` /
  `settings.gradle.kts` / `gradle/libs.versions.toml` / `build-logic/` (plan 01), new CI workflow
  in `.github/workflows/` (plan 01), all modules through `:desktopApp`/`:webApp` (plan 07).
- Produces: a repo tree whose only build roots are the new world; consumed by every later task.

**Steps:**

- [ ] **Step 1: Entry gate — prove the pre-teardown tree is green and clean.**
  Run:
  ```bash
  cd /home/slothie/StudioProjects/Nyx
  git status --porcelain
  ./gradlew build
  ```
  Expected: `git status --porcelain` prints nothing; gradle ends with `BUILD SUCCESSFUL`.
  If either fails, STOP — phase 07 is not actually complete; do not tear anything down on a
  broken tree.

- [ ] **Step 2: Verify the steganography path holds the NEW module (collision safety check).**
  Run:
  ```bash
  ls steganography/src/
  ```
  Expected: the new KMP layout — `commonMain` (plus `commonTest` and platform source sets).
  FAILURE MODE: if you see `main/` (old Android layout) or both layouts mixed, STOP immediately
  and report to the user — the plan-01 collision resolution did not happen and deleting here
  risks destroying new engine code. Do not improvise.

- [ ] **Step 3: Inventory what is left of the old tree, then CONFIRM the full deletion list
  with the user.**
  Run:
  ```bash
  ls -d app buildSrc utils feature_base 2>/dev/null
  git ls-files | grep -cE '^(app|buildSrc|utils|feature_base)/'
  ```
  Expected: `app` and `buildSrc` listed (`utils`/`feature_base` already gone via plan 01); the
  count equals the number of tracked files under the listed dirs. Any combination is fine — the
  next step is idempotent.
  Then — spec §12 requires the ENTIRE deletion list confirmed with the user at this phase —
  present the "Deletion inventory" table from the top of this plan (adjusted to what actually
  remains per the commands above) via AskUserQuestion (or plain text if unavailable) and WAIT
  for a go-ahead. Options: (1) proceed with the listed deletions, (2) stop and discuss. Do not
  run Step 4 without approval. The Task 2 asset checkpoint remains a separate, additional gate.

- [ ] **Step 4: Delete the old build tree and GitLab-era files.**
  Run:
  ```bash
  git rm -r --ignore-unmatch app buildSrc utils feature_base
  git rm --ignore-unmatch .gitlab-ci.yml crypto_migration_plan.md
  ```
  Expected: `rm 'app/...'` lines for every tracked file; no errors. `--ignore-unmatch` makes the
  already-deleted paths a no-op.

- [ ] **Step 5: Handle the old GitHub workflow (conditional).**
  Run:
  ```bash
  ls .github/workflows/
  grep -l 'konsist' .github/workflows/*.yml || echo "NO_OLD_WORKFLOW"
  ```
  Decision rule: the old `check.yml` is identifiable by its `konsist` job (which runs
  `./gradlew konsist_test:test`) and its `:app:assembleDebug` step. If `grep -l` names a file,
  it is the old workflow — run
  `git rm .github/workflows/check.yml` (use the actual matched filename). If it prints
  `NO_OLD_WORKFLOW`, plan 01 replaced `check.yml` in place — keep it.
  Then verify at least one workflow file remains:
  ```bash
  ls .github/workflows/*.yml
  ```
  Expected: exactly the new CI workflow(s) from plan 01. If NOTHING remains, STOP and escalate —
  the repo would have no CI, which violates the phase deliverable.

- [ ] **Step 6: Handle the old root `detekt.yml` (conditional).**
  Run:
  ```bash
  grep -rn 'detekt\.yml' build.gradle.kts settings.gradle.kts gradle/ build-logic/ 2>/dev/null || echo "UNREFERENCED"
  ```
  Decision rule — the question is whether some reference resolves to the REPO-ROOT `detekt.yml`
  specifically, so INSPECT THE MATCHED LINES; the mere presence of a match is not enough (a hit
  like `config/detekt/detekt.yml` references a different file and must not save the root one):
  - KEEP the root file only if at least one matched line resolves to the root path — forms like
    `rootProject.file("detekt.yml")`, `rootDir.resolve("detekt.yml")`, `"$rootDir/detekt.yml"`,
    or a bare relative `"detekt.yml"` inside the ROOT `build.gradle.kts`. That means plan 01
    reused the path with new content. Record which line justified the keep.
  - Otherwise — output is `UNREFERENCED`, or every match points at a non-root path (e.g.
    `config/detekt/detekt.yml`) or at pure `buildUponDefaultConfig` usage — the root file is
    legacy; run:
    ```bash
    git rm --ignore-unmatch detekt.yml
    ```
  Expected: either a justified keep (citing the matched line), or `rm 'detekt.yml'`.

- [ ] **Step 7: Sweep `gradle.properties` for legacy keys.**
  Run:
  ```bash
  grep -nE 'accessKey|privateKey|kapt\.|enableJetifier' gradle.properties || echo "CLEAN"
  ```
  Expected: `CLEAN` (plan 01 rewrote the file). If any line matches — these are Unsplash-era
  fake API keys and kapt/jetifier flags no module uses anymore — delete exactly those lines with
  the Edit tool, then prove the build still configures:
  ```bash
  ./gradlew help
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Verify `local.properties` hygiene.**
  Run:
  ```bash
  git ls-files local.properties
  git check-ignore local.properties && echo "IGNORED_OK"
  ```
  Expected: first command prints NOTHING (untracked); second prints `local.properties` then
  `IGNORED_OK`. If it were tracked (it is not, verified at plan-writing time): run
  `git rm --cached local.properties` and confirm `.gitignore` contains the `local.properties`
  line (it does — line "Local configuration file" block).

- [ ] **Step 9: Full build on the post-deletion tree.**
  Deleting `buildSrc/` changes real build behavior (Gradle auto-compiles a `buildSrc` directory
  whenever one exists), so a full re-verification is mandatory, not paranoia:
  ```bash
  ./gradlew build
  ```
  Expected: `BUILD SUCCESSFUL`, same task set as Step 1 minus nothing (no module lived in the
  deleted dirs — plan 01 already dropped them from `settings.gradle.kts`).
  FAILURE MODE: any `Could not find ...` / unresolved-reference pointing at `app/`, `buildSrc/`,
  or `local.*` plugin ids means a stale reference survived phases 01–07 — fix the referencing
  file (remove the stale reference), re-run, and note the fix in the commit body.

- [ ] **Step 10: Commit the teardown.**
  ```bash
  git add -A
  git commit -m "chore: delete legacy android build tree and gitlab-era config

Removes app/, buildSrc/, .gitlab-ci.yml, crypto_migration_plan.md and
remaining old-workflow/detekt/gradle.properties remnants. The Compose
Multiplatform tree is now the only world in the repo."
  ```
  Expected: commit created; `git status --porcelain` prints nothing.

---

### Task 2: Design-asset checkpoint (EXPLICIT USER APPROVAL REQUIRED)

**Files:**
- Delete (only per user approval, any subset): `app-featured-image.ai`, `applogo-export.ai`,
  `applogo.ai`, `app-featured-image.jpg`, `applogo.png`
- Never delete: `images/nyx-logo.png` (Task 4's README references it)
- Test: `git status` + file-existence checks

**Interfaces:**
- Consumes: nothing from other plans.
- Produces: the final asset set; Task 4's README depends on `images/nyx-logo.png` surviving.

**Steps:**

- [ ] **Step 1: Inventory the candidates with sizes.**
  Run:
  ```bash
  ls -la app-featured-image.ai applogo-export.ai applogo.ai app-featured-image.jpg applogo.png images/
  ```
  Expected (sizes verified at plan-writing time): three `.ai` Illustrator sources (~350–375 KB
  each), `app-featured-image.jpg` (~42 KB), `applogo.png` (~8 KB), and `images/nyx-logo.png`.

- [ ] **Step 2: CHECKPOINT — ask the user; DO NOT PROCEED without an answer.**
  Use the AskUserQuestion tool (or, if unavailable, stop and ask in plain text and wait for the
  reply). Present exactly this:
  > Teardown found these design assets at the repo root. `images/nyx-logo.png` is kept regardless
  > (the new README uses it). Which of the rest should be deleted?
  > 1. Delete the three `.ai` Illustrator sources + `app-featured-image.jpg`, keep `applogo.png`
  > 2. Delete all five (`.ai` files, `app-featured-image.jpg`, `applogo.png`)
  > 3. Keep everything
  This is a hard gate from spec §12 ("stray `.ai`/image design assets touched only with explicit
  approval"). No timeout-and-assume. If the user is unreachable, mark this task blocked and
  continue with Task 3 — Task 7's final sweep does not depend on this outcome.

- [ ] **Step 3: Apply exactly the approved deletions.**
  For option 1:
  ```bash
  git rm app-featured-image.ai applogo-export.ai applogo.ai app-featured-image.jpg
  ```
  For option 2, additionally `git rm applogo.png`. For option 3, do nothing.
  Verify the README dependency survived:
  ```bash
  test -f images/nyx-logo.png && echo "LOGO_OK"
  ```
  Expected: `LOGO_OK`.

- [ ] **Step 4: Commit (only if something was deleted).**
  ```bash
  git commit -m "chore: remove retired design source assets

User-approved teardown of Illustrator sources and unused exports.
images/nyx-logo.png is kept as the README logo."
  ```
  Expected: commit created, clean tree.

---

### Task 3: Android version identity — 1.0.0 (versionCode 1)

**Files:**
- Modify: `androidApp/build.gradle.kts`
- Test: `grep` assertion + `./gradlew :androidApp:assembleDebug`

**Interfaces:**
- Consumes: `:androidApp` module and its `defaultConfig` block (plan 05, Android entry app;
  `applicationId = "com.slothiesmooth.nyx"` per the 00-INDEX namespace table).
- Produces: release identity `versionCode = 1`, `versionName = "1.0.0"` — referenced by the
  README "Building" section (Task 4) only descriptively; nothing else consumes it.

**Decision (stated per orchestration):** plain `versionCode = 1` like pawdex — the
`major*10000`-style formula was considered and rejected (no multi-APK, no ABI splits, nothing to
derive; Play requires only a monotonically increasing integer). `versionName = "1.0.0"` — full
semver, since the rewrite is a clean v1.

**Steps:**

- [ ] **Step 1: Read the current values (failing check first).**
  Run:
  ```bash
  grep -n 'versionCode\|versionName' androidApp/build.gradle.kts
  ```
  Expected: both keys present inside `defaultConfig` with whatever values plan 05 set
  (pawdex-ported default is `versionCode = 1`, `versionName = "1.0"`). This is the "failing
  test": the values do not yet read `1` / `"1.0.0"`.

- [ ] **Step 2: Edit to the decided identity.**
  Using the Edit tool on `androidApp/build.gradle.kts`, set inside `defaultConfig`:
  ```kotlin
  versionCode = 1
  versionName = "1.0.0"
  ```
  (Edit only these two assignments; leave the rest of `defaultConfig` untouched.)

- [ ] **Step 3: Verify.**
  Run:
  ```bash
  grep -n 'versionCode = 1$\|versionName = "1.0.0"' androidApp/build.gradle.kts
  ./gradlew :androidApp:assembleDebug
  ```
  Expected: exactly two matching lines; then `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit.**
  ```bash
  git add androidApp/build.gradle.kts
  git commit -m "chore: set android version identity to 1.0.0 (versionCode 1)"
  ```
  Expected: commit created.

---

### Task 4: README.md rewrite

**Files:**
- Modify: `README.md` (full replacement)
- Test: stale-content greps + referenced-file existence checks

No Screenshots section ships in this rewrite: no captures exist yet, and a table of
"coming soon" cells is placeholder content in the flagship doc. Add the section in a normal
docs PR once real screenshots are captured.

**Interfaces:**
- Consumes: CI workflow filename (plan 01 — verified in Step 2), module set and gradle task names
  from plans 01–07 (`:androidApp:assembleDebug`, `:desktopApp:run`,
  `:webApp:wasmJsBrowserDevelopmentRun`, `:shared:design-library:snapshot:verifyPaparazziDebug`),
  iosApp scaffold location (plan 07).
- Produces: the shipped `README.md`; Task 5's CONTRIBUTING is linked from it.

**Steps:**

- [ ] **Step 1: Failing check — prove the current README is stale.**
  Run:
  ```bash
  grep -c 'Unsplash\|gitlab.com/technowolf' README.md
  ```
  Expected: a nonzero count (the old README advertises the removed Unsplash feature and links
  GitLab). This is the condition the rewrite must drive to zero.

- [ ] **Step 2: Pin down the real CI workflow filename, the iosApp path, and what
  `./gradlew build` actually runs.**
  Run:
  ```bash
  ls .github/workflows/
  ls -d iosApp client/iosApp 2>/dev/null
  ./gradlew build --dry-run | grep -q 'verifyPaparazzi' && echo "PAPARAZZI_IN_BUILD" || echo "PAPARAZZI_SEPARATE"
  ```
  The README content below assumes the workflow is `ci.yml` and the Xcode scaffold is at
  repo-root `iosApp/`. If either differs, substitute the actual name/path in the content in the
  next step (two badge URLs; one sentence in "Building"). Do not publish a badge that 404s.
  The third command settles what the docs may claim about `./gradlew build` — whether plan 01/04
  wired Paparazzi verification into `check` was unknown at plan-writing time, and README and
  CONTRIBUTING must ship the SAME claim:
  - `PAPARAZZI_SEPARATE` (the default the content below assumes): ship the content as written —
    the `build` comment reads `assemble + tests + detekt + spotless`, Paparazzi runs as its own
    listed command.
  - `PAPARAZZI_IN_BUILD`: change the README "Building" comment to
    `# everything: assemble + tests + detekt + spotless + paparazzi verify`, and apply the
    matching CONTRIBUTING substitution in Task 5 Step 2.

- [ ] **Step 3: Write the complete new README.**
  Replace `README.md` with exactly this content (Write tool):

`````markdown
<div align="center">
    <img src="images/nyx-logo.png" alt="Nyx logo" width="180">
    <h1>Nyx</h1>
    <h4>Do you have a secret to share? Hide it inside an image, locked with a password.</h4>
    <p>
        <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-brightgreen.svg" alt="MIT license"></a>
        <a href="https://github.com/daksh7011/Nyx/actions/workflows/ci.yml"><img src="https://github.com/daksh7011/Nyx/actions/workflows/ci.yml/badge.svg?branch=develop" alt="CI status"></a>
    </p>
</div>

## Overview

Nyx hides an encrypted text message inside the pixels of an ordinary image.
The message is sealed with AES-256-GCM using a key derived from your password,
written into the least significant bits of the image's RGB channels, and
exported as a lossless PNG. To everyone else it is just a picture; with the
password, Nyx recovers the exact message.

Nyx is a Compose Multiplatform app: one Kotlin codebase runs on Android,
desktop (Linux/macOS/Windows), the web (Kotlin/Wasm), and iOS.

## How it works

```text
message ──(AES-256-GCM)──> encrypted blob ──(LSB embed, 2 bits per R/G/B channel)──> stego PNG
          key = PBKDF2-HMAC-SHA256(password, 600,000 iterations, random 16-byte salt)
```

1. **Encrypt** — the plaintext is sealed with AES-256-GCM. Every message gets a
   fresh random salt and nonce, so encrypting the same text twice produces
   different blobs. GCM is authenticated: a wrong password or a tampered image
   fails cleanly — you never get garbage output.
2. **Embed** — the encrypted blob is marker-framed and spread across the two
   least significant bits of each pixel's red, green, and blue channels.
   Payloads too large for one image can span multiple images.
3. **Export** — the result is always PNG (lossless — required for the payload
   to survive). Decrypting reverses the pipeline: extract, authenticate, decrypt.

## Features

- **Encrypt wizard** — pick a cover image (file picker on every platform,
  camera on Android/iOS), type a message and password, get a stego PNG saved
  to your vault and ready to share.
- **Vault** — a grid of your stego images with detail view, share/export,
  archive, and delete. Metadata lives in SQLDelight; image bytes live as PNG
  files on disk.
- **Decrypt** — open a vault image or pick any image, enter the password,
  reveal and copy the hidden message. Wrong password and no-hidden-message are
  reported honestly and distinctly.
- **Themes** — five palettes (Midnight, Espresso, Nardo, Cream, Mist) with a
  system/light/dark mode override, persisted across launches.
- **Settings** — about, open-source licenses, wipe vault, app version.
- **Ad-free, account-free, network-free** — Nyx makes zero network calls.
  Your images and secrets never leave your device.

## Platforms

| Platform | Vault persistence | Camera capture | Notes |
|---|---|---|---|
| Android 7.0+ (API 24) | Yes | Yes | Primary target |
| Desktop JVM (Linux / macOS / Windows) | Yes | No | Compose for Desktop |
| Web (Kotlin/Wasm) | Session-only | No | Encrypt / decrypt / export fully work; the vault list is in-memory for now |
| iOS (device + simulators) | Yes | Yes | Builds on the macOS lane only |

## Architecture

```text
:androidApp        :desktopApp        :webApp        iosApp/ (Xcode, macOS only)
      \                 |                /
       +----------------+---------------+
                        |
                     :client            global DI (initKoin), App(), SqlDelight NyxDb,
                        |               platform source implementations
   :feature:{navigation,splash,theme,vault,encrypt,decrypt,settings}:client:{api,basic}
                        |               plumbing: :feature:common:client:{api,koin}
      +---------+-------+------+------------------+----------------------+
      |         |              |                  |                      |
   :crypto   :steganography  :shared:data   :shared:presentation  :shared:design-library
   AES-GCM   pure-Kotlin     results/sources  BaseViewModel,        Nx* components + tokens
   + PBKDF2  LSB engine      events/clock/ids ViewState/UiState      \- :snapshot (Paparazzi)

   test infra: :shared:test-support, :shared:compose-test-support
```

- **Feature modules** (`:feature:X:client:{api,basic}`): `api` exposes a
  `Feature` interface plus type-safe `@Serializable` routes; `basic` implements
  it behind an isolated Koin container. Features never depend on another
  feature's `api` — cross-feature signaling goes through a `DomainEventBus`.
- **Engines**: `:crypto` (cryptography-kotlin, AES-256-GCM + PBKDF2) and
  `:steganography` (stdlib-only LSB codec over `PixelImage`) depend on no other
  project module and are consumed through use cases.
- **`:client`** owns global DI, the SqlDelight database, and the per-platform
  implementations of the source interfaces declared in `:shared:data`.
- **Design system**: `Nx*` atomic components with theme tokens, previews for
  every visual state, and Paparazzi golden tests in
  `:shared:design-library:snapshot`.
- **Convention plugins** in `build-logic/` keep module build scripts
  declarative; `gradle/libs.versions.toml` is the single version source.

## Building

Prerequisites: JDK 21 and an Android SDK with API 36. iOS additionally
requires macOS with Xcode.

```bash
./gradlew build                                    # everything: assemble + tests + detekt + spotless
./gradlew :androidApp:assembleDebug                # Android APK
./gradlew :androidApp:installDebug                 # install on a connected device
./gradlew :desktopApp:run                          # run the desktop app
./gradlew :webApp:wasmJsBrowserDevelopmentRun      # dev server for the web app
./gradlew :shared:design-library:snapshot:verifyPaparazziDebug   # design-system goldens
```

**Linux / Windows note:** iOS targets are skipped automatically
(`kotlin.native.ignoreDisabledTargets=true`); everything else builds and tests
locally. iOS compilation and the `iosApp/` Xcode project are validated on the
macOS lane.

## Security model (please read)

- **The security layer is the cryptography, not the steganography.** Messages
  are encrypted with AES-256-GCM; keys derive from your password via
  PBKDF2-HMAC-SHA256 with 600,000 iterations and a random 16-byte salt, with a
  fresh random nonce per message. GCM is authenticated encryption — a wrong
  password or a modified image is detected and rejected, never silently
  decrypted into noise.
- **LSB steganography is an obscurity layer.** Statistical steganalysis can
  reveal that an image likely carries a payload. Assume a capable adversary can
  detect that something is hidden; what they cannot do without your password is
  read it.
- **PNG only survives lossless channels.** Messaging apps and social networks
  that recompress images (usually to JPEG) destroy the payload. Share the stego
  image as a file, not as an inline photo.
- **Your password is the whole game.** A weak password falls to offline
  guessing regardless of iteration count. There is no recovery mechanism: lose
  the password, lose the message.
- **Clean break from pre-rewrite Nyx.** Images produced by the old (pre-2026)
  Android app use an abandoned format and cannot be decrypted by this version.

Nyx is an experimental open-source project provided under the MIT license,
without warranty of any kind. It builds on well-reviewed primitives via
[cryptography-kotlin](https://github.com/whyoleg/cryptography-kotlin), but it
has not been independently audited. Do not rely on it as your only protection
for high-stakes secrets.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for dev setup, architecture
conventions, and the add-a-feature checklist. Contribution is not limited to
code — feature ideas, docs, UI design ideas, and even typo fixes are welcome.
You are just an issue away: [issue tracker](https://github.com/daksh7011/Nyx/issues).

This project follows a [code of conduct](CODE_OF_CONDUCT.md).

## Emailware

Nyx is an emailware. Which means, if you liked using this app or it has helped
you in any way, I'd like you to send me an email at
[daksh@technowolf.in](mailto:daksh@technowolf.in) about anything you'd want to
say about this software. I'd really appreciate it! Plus I would be more than
happy to know my initiative helped someone. :)

## License

[MIT License](LICENSE) — Copyright (c) 2020 TechnoWolf FOSS
`````

- [ ] **Step 4: Verify the rewrite.**
  Run:
  ```bash
  grep 'Unsplash\|gitlab.com/technowolf\|saythanks.io\|paypal.me' README.md || echo "STALE_GONE"
  for f in images/nyx-logo.png LICENSE CONTRIBUTING.md CODE_OF_CONDUCT.md; do test -f "$f" && echo "OK $f" || echo "MISSING $f"; done
  ```
  Expected: exactly `STALE_GONE` (no matched lines printed — grep without `-c` prints nothing
  on zero matches, then exits 1 and triggers the marker) and four `OK` lines. `MISSING` on any
  referenced file means a broken README link — fix the reference before committing.

- [ ] **Step 5: Commit.**
  ```bash
  git add README.md
  git commit -m "docs: rewrite README for the compose multiplatform rewrite

New overview, feature list, platform matrix, module graph, per-target
build instructions, and an honest security-model section. Drops the
GitLab-era badges and the removed Unsplash feature."
  ```
  Expected: commit created.

---

### Task 5: CONTRIBUTING.md rewrite

**Files:**
- Modify: `CONTRIBUTING.md` (full replacement)
- Test: stale-content grep + referenced-path existence + gate-task-name verification

**Interfaces:**
- Consumes: convention plugin ids `nyx.kmp.library`, `nyx.compose`, `nyx.feature.api`,
  `nyx.feature.basic` (plan 01, from spec §3); aggregate gate task names `detektCheck` /
  `spotlessCheck` (plan 01, spec §3 CI list — verified in Step 2); Paparazzi task names
  `recordPaparazziDebug`/`verifyPaparazziDebug` on `:shared:design-library:snapshot` (plan 04);
  the feature anatomy from 00-INDEX contracts (`Feature`, `KoinFeatureProvider`, `BaseViewModel`,
  `DomainEventBus`, source interfaces in `:shared:data`).
- Produces: the shipped `CONTRIBUTING.md` (linked from README Task 4).

**Steps:**

- [ ] **Step 1: Failing check — prove the current file is stale.**
  Run:
  ```bash
  grep -c 'master branch\|Branch of the' CONTRIBUTING.md
  ```
  Expected: nonzero (the old file tells contributors to PR against a nonexistent `master`).

- [ ] **Step 2: Verify the quality-gate task names and the `build` claim the doc will print.**
  Run:
  ```bash
  ./gradlew tasks --all | grep -iE '^(detektcheck|spotlesscheck|detekt)\b' | head
  ./gradlew build --dry-run | grep -q 'verifyPaparazzi' && echo "PAPARAZZI_IN_BUILD" || echo "PAPARAZZI_SEPARATE"
  ```
  (The task-name grep anchors at line start with a word boundary because `gradlew tasks` output
  suffixes task names with ` - <description>` — a plain `detekt$` alternative would never match
  and the fallback detection of an aggregate `detekt` task would silently not fire.)
  The content below prints `./gradlew detektCheck` and `./gradlew spotlessCheck` (spec §3 names).
  If plan 01 registered different aggregate names (e.g. plain `detekt`), substitute the real
  names in the content before writing. A doc that prints commands that do not run is worse than
  no doc.
  The second command must produce the SAME result as Task 4 Step 2. The content below assumes
  `PAPARAZZI_SEPARATE` ("runs assembly, all host tests, detekt, and spotless"). If it prints
  `PAPARAZZI_IN_BUILD`, change that sentence to "runs assembly, all host tests, detekt,
  spotless, and the Paparazzi golden verification" — and confirm the README (Task 4) makes the
  matching claim. Exactly one claim ships across both docs.

- [ ] **Step 3: Write the complete new CONTRIBUTING.**
  Replace `CONTRIBUTING.md` with exactly this content (Write tool; apply Step 2 substitutions
  if any):

`````markdown
# Contributing to Nyx

Contributions of any kind are welcome — code, bug reports, docs, UI ideas,
typo fixes. You are one issue away. This project follows a
[code of conduct](CODE_OF_CONDUCT.md).

## Development setup

1. Install **JDK 21** (any distribution; Temurin works well).
2. Install **Android Studio** (latest stable — the project uses AGP 9.2, older
   IDE versions will refuse to sync) with an Android SDK for **API 36**.
3. Clone and build:

   ```bash
   git clone git@github.com:YOUR-USERNAME/Nyx.git
   cd Nyx
   ./gradlew build
   ```

`./gradlew build` runs assembly, all host tests, detekt, and spotless. If it
is green, your setup is correct.

### Linux / Windows caveat (iOS)

Every KMP module declares iOS targets, but they only compile on macOS with
Xcode. On other hosts they are skipped automatically via
`kotlin.native.ignoreDisabledTargets=true` — Android, desktop, and web build
and test locally. Do not try to fix iOS compile errors blind from Linux; that
work happens on the macOS lane.

## Build logic: convention plugins

Module build scripts stay tiny because shared configuration lives in the
`build-logic/` included build:

| Plugin id | Used by | What it does |
|---|---|---|
| `nyx.kmp.library` | every KMP module | declares the six targets (android, iosX64, iosArm64, iosSimulatorArm64, jvm, wasmJs), default hierarchy template, opt-ins |
| `nyx.compose` | UI modules | Compose Multiplatform + compose-resources configuration |
| `nyx.feature.api` | `:feature:*:client:api` | KMP library + minimal api-surface dependencies |
| `nyx.feature.basic` | `:feature:*:client:basic` | KMP + Compose + DI + test wiring |

Change build behavior in `build-logic/`, never by copy-pasting into module
scripts. Versions live in exactly one place: `gradle/libs.versions.toml`.

## Adding a feature (checklist)

Features follow an api/basic split. For a new feature `foo`:

1. Create `feature/foo/client/api` and `feature/foo/client/basic`; include
   both in `settings.gradle.kts`.
2. **api module** (package `com.slothiesmooth.nyx.feature.foo.api`):
   - `interface FooFeature : Feature` — the only surface other modules may see.
   - `@Serializable` route types (`data object FooRoute`, or a `data class`
     for routes with arguments). No string routes.
3. **basic module** (package `com.slothiesmooth.nyx.feature.foo.basic`):
   - `BasicFooProvider : KoinFeatureProvider`, implementing `FooFeature` —
     registers repositories/use cases/view models in `onProvideDI`, screens in
     `provideNavigation`.
   - Domain: single-purpose use case classes with `operator fun invoke`,
     `factoryOf`-registered. Repository writes return `AppResult`; reads
     return `Flow`.
   - Presentation: view models extend `BaseViewModel`; screen state is a
     `@Stable` read-only interface plus a mutable implementation; all
     collections use kotlinx-immutable types.
   - UI: composables are dumb — no filtering/sorting/mapping/pluralization in
     UI; the view model exposes render-ready state. Every screen gets previews
     covering every visual state (loading, error, success, empty).
4. Wire it in `:client`'s app module:
   `single<FooFeature> { BasicFooProvider(...) }` and add it to the ordered
   `List<Feature>`.
5. **Never** depend on another feature's `api` from a feature module.
   Cross-feature signaling uses `DomainEventBus`; cross-feature reads use
   projection interfaces declared in `:shared:data` and wired in `:client`.
6. Data access goes through source interfaces (`VaultSource`,
   `SettingsSource`, ...) declared in `:shared:data` and implemented in
   `:client` / platform modules — features never touch drivers directly.
7. Tests: use cases and view models against hand-written fakes.

## Quality gates

Every PR must pass all of these (CI runs them; run locally first):

```bash
./gradlew detektCheck      # static analysis — maxIssues=0
./gradlew spotlessCheck    # formatting
./gradlew build            # assemble + all host tests + gates
./gradlew :shared:design-library:snapshot:verifyPaparazziDebug
```

Ground rules:

- **No lint suppressions to make a build pass.** A detekt finding is a design
  signal — fix the root cause. The single documented exception is
  `NxColors.kt`, the one raw-ARGB token file.
- **Testing stack** is kotlin.test + kotlinx-coroutines-test + hand-written
  fakes. No mockk, no kotest, no turbine. Tests serve purpose, not count.
- **Paparazzi goldens are font-sensitive.** Record on Linux (matches the
  ubuntu CI runner) with
  `./gradlew :shared:design-library:snapshot:recordPaparazziDebug` and commit
  the PNGs.
- **US English** everywhere — identifiers, comments, docs, commit messages.

## Commit style

Conventional Commits: `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`,
`test:`, `build:`, `ci:`. Keep commits small — one green change per commit.

## Pull requests

1. Fork this repository and branch off `develop`.
2. Make your change; keep all quality gates green.
3. Open a pull request against `develop`. We usually comment within a few
   days and may suggest changes or alternatives.
`````

- [ ] **Step 4: Verify the rewrite.**
  Run:
  ```bash
  grep 'master branch\|Branch of the' CONTRIBUTING.md || echo "STALE_GONE"
  test -d build-logic && test -f gradle/libs.versions.toml && echo "PATHS_OK"
  ```
  Expected: exactly `STALE_GONE` (grep without `-c` prints nothing on zero matches, then the
  marker) and `PATHS_OK`. If `build-logic/` or the catalog is missing, the doc
  lies about the repo — STOP and reconcile with what plan 01 actually built.

- [ ] **Step 5: Commit.**
  ```bash
  git add CONTRIBUTING.md
  git commit -m "docs: rewrite CONTRIBUTING for the new toolchain and architecture

JDK 21 setup, Linux iOS caveat, convention-plugin guide, add-a-feature
checklist, quality gates, and conventional-commit style."
  ```
  Expected: commit created.

---

### Task 6: Renovate modernization + probot stale removal

**Files:**
- Modify: `renovate.json` (full replacement)
- Delete: `.github/stale.yml`
- Test: `python3 -m json.tool` parse + git status

**Interfaces:**
- Consumes: `gradle/libs.versions.toml` (plan 01) — Renovate's `gradle` manager (enabled by
  `config:recommended`) reads version catalogs natively, no extra config needed; the FileKit
  pin rationale from 00-INDEX ("DO NOT bump to 0.14.x — Kotlin 2.4-compiled, drops iosX64").
- Produces: the shipped `renovate.json`.

**Steps:**

- [ ] **Step 1: Failing check — the current config uses the deprecated preset.**
  Run:
  ```bash
  grep -c 'config:base' renovate.json
  ```
  Expected: `1`. (`config:base` is Renovate's deprecated alias; `config:recommended` replaces it.)

- [ ] **Step 2: Write the new config.**
  Replace `renovate.json` with exactly this content (Write tool). Notes baked into the config:
  Renovate JSON forbids comments, so machine-readable `description` fields carry the rationale
  (self-contained — no repo-path references that could dangle for other contributors);
  FileKit's Maven group is `io.github.vinceglb`; toolchain artifacts (Kotlin, AGP, Compose,
  Paparazzi) move together per 00-INDEX pins, so they are excluded from bot bumps. The Kotlin
  exclusion regex is anchored with a `[.:]` terminator — a bare `^org\.jetbrains\.kotlin`
  prefix would also swallow every `org.jetbrains.kotlinx:*` library (coroutines, serialization,
  datetime, collections-immutable), which 00-INDEX treats as normal bot-bumpable catalog
  entries. No `lockFileMaintenance` block: this repo uses no Gradle dependency locking (no
  `gradle.lockfile` files), so the setting would be inert noise.

```json
{
  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
  "extends": ["config:recommended"],
  "packageRules": [
    {
      "description": "Automerge non-major updates of stable (>=1.0) dependencies once CI is green",
      "matchUpdateTypes": ["minor", "patch"],
      "matchCurrentVersion": "!/^0/",
      "automerge": true
    },
    {
      "description": "DO-NOT-BUMP-PAST-0.13: FileKit 0.14.x is compiled with Kotlin 2.4 and drops the iosX64 target, which this project ships",
      "matchPackageNames": ["/^io\\.github\\.vinceglb:filekit/"],
      "allowedVersions": "<0.14.0"
    },
    {
      "description": "Toolchain versions move together (Kotlin, AGP, Compose plugins/runtime, Paparazzi); bump manually as a verified set. The [.:] terminator keeps org.jetbrains.kotlinx libraries bot-bumpable",
      "matchPackageNames": [
        "/^org\\.jetbrains\\.kotlin[.:]/",
        "/^com\\.android\\.tools\\.build:gradle/",
        "/^com\\.android\\.(application|library|kotlin\\.multiplatform\\.library)/",
        "/^org\\.jetbrains\\.compose/",
        "/^app\\.cash\\.paparazzi/"
      ],
      "enabled": false
    }
  ]
}
```

- [ ] **Step 3: Verify the JSON parses, the preset flipped, and the exclusion regex is
  correctly scoped.**
  Run:
  ```bash
  python3 -m json.tool renovate.json > /dev/null && echo "JSON_OK"
  grep -c 'config:recommended' renovate.json
  grep 'config:base' renovate.json || echo "BASE_GONE"
  python3 -c 'import re; p = re.compile(r"^org\.jetbrains\.kotlin[.:]"); toolchain = ["org.jetbrains.kotlin:kotlin-gradle-plugin", "org.jetbrains.kotlin.multiplatform", "org.jetbrains.kotlin.plugin.compose", "org.jetbrains.kotlin.plugin.serialization"]; kotlinx = ["org.jetbrains.kotlinx:kotlinx-coroutines-core", "org.jetbrains.kotlinx:kotlinx-serialization-json", "org.jetbrains.kotlinx:kotlinx-datetime", "org.jetbrains.kotlinx:kotlinx-collections-immutable"]; assert all(p.search(n) for n in toolchain), "toolchain name escaped the exclusion"; assert not any(p.search(n) for n in kotlinx), "kotlinx library wrongly excluded from bot bumps"; print("REGEX_OK")'
  ```
  Expected: `JSON_OK`, then `1`, then `BASE_GONE`, then `REGEX_OK`. An `AssertionError` means
  the exclusion regex regressed — kotlinx libraries must stay bot-bumpable while the Kotlin
  toolchain stays excluded.

- [ ] **Step 4: Delete the dead probot config.**
  The `probot/stale` GitHub App this file configured was deprecated years ago; the file is inert.
  ```bash
  git rm .github/stale.yml
  ```
  Expected: `rm '.github/stale.yml'`.

- [ ] **Step 5: Commit.**
  ```bash
  git add renovate.json
  git commit -m "ci: modernize renovate config and drop probot stale

config:recommended (catalog-aware gradle manager), automerge for stable
non-major updates, FileKit pinned below 0.14 (Kotlin 2.4 / iosX64 drop),
toolchain set excluded from bot bumps."
  ```
  Expected: commit created.

---

### Task 7: Final CI polish + full-suite verification (CI parity on the final tree)

**Files:**
- Modify (conditional): `.github/workflows/*.yml` (Step 5 polish items, only if missing),
  `README.md` (badge alignment, only if wrong); otherwise fix-forward only if a check fails
- Test: full gradle suite + Paparazzi verify + CI-parity run + CI polish checklist +
  legacy-reference sweep

**Interfaces:**
- Consumes: everything plans 01–07 built; the CI workflow (plan 01); this plan's Tasks 1–6.
- Produces: the phase deliverable — "repo contains ONLY the new world; docs current; CI green
  on the final tree."

**Steps:**

- [ ] **Step 1: Full build on the final tree.**
  ```bash
  ./gradlew build
  ```
  Expected: `BUILD SUCCESSFUL`. Any failure here is a regression introduced by Tasks 1–6 —
  use superpowers:systematic-debugging; the usual suspect is a stale reference to a deleted path.

- [ ] **Step 2: Paparazzi goldens verify.**
  ```bash
  ./gradlew :shared:design-library:snapshot:verifyPaparazziDebug
  ```
  Expected: `BUILD SUCCESSFUL`. If deltas appear, something in this phase touched rendering —
  it must not have; investigate rather than re-record.

- [ ] **Step 3: CI-parity run — execute exactly what the workflow executes.**
  Enumerate the workflow's gradle invocations, then run each one locally:
  ```bash
  grep -hoE '\./gradlew [^"]*' .github/workflows/*.yml || echo "NO_LITERAL_GRADLEW_LINES"
  ```
  If the output is `NO_LITERAL_GRADLEW_LINES`, the workflow does not invoke Gradle via literal
  `./gradlew` run lines (e.g. it uses `gradle/actions/setup-gradle` with an `arguments:` input,
  `gradle/gradle-build-action`, or a reusable workflow) — in that case OPEN each workflow file
  and enumerate the gradle invocations manually from the action inputs. Running zero commands
  because the grep found nothing is a FAILURE of this step, not a pass.
  Run every enumerated command (spec §3 set: assemble, host tests, `verifyPaparazzi`,
  `detektCheck`, `spotlessCheck` — the exact list is whatever plan 01's workflow contains).
  Expected: every command ends `BUILD SUCCESSFUL`. This is the local proof that CI will be green
  on the final tree.

- [ ] **Step 4: Workflow syntax sanity (best-effort, non-blocking).**
  ```bash
  command -v actionlint >/dev/null && actionlint .github/workflows/*.yml || echo "ACTIONLINT_NOT_INSTALLED (skip - Step 3 already ran the commands; syntax is proven on first push)"
  ```
  Expected: no findings, or the documented skip message. Do not install tooling just for this.

- [ ] **Step 5: Final CI polish (the 00-INDEX row-08 deliverable).**
  Plan 01 authored the workflow; this phase ships its final form. Check three concrete polish
  items and fix-forward whatever is missing:
  ```bash
  grep -n 'pull_request\|push\|branches' .github/workflows/*.yml
  grep -n 'concurrency' .github/workflows/*.yml || echo "NO_CONCURRENCY_GROUP"
  grep -n 'actions/workflows' README.md
  ```
  1. **Triggers** cover PR + push to `develop` (spec §3: "GitHub Actions on PR + push to
     develop"). If not, fix the `on:` block to:
     ```yaml
     on:
       push:
         branches: [develop]
       pull_request:
     ```
  2. **Concurrency**: superseded runs on the same ref should cancel. If
     `NO_CONCURRENCY_GROUP`, add at the workflow top level (below `on:`):
     ```yaml
     concurrency:
       group: ${{ github.workflow }}-${{ github.ref }}
       cancel-in-progress: true
     ```
  3. **Badge alignment**: the README badge URLs (Task 4) name the actual workflow file and the
     `develop` branch shown by the first grep. If they diverge, fix the README.
  If anything changed, re-run the Step 3 parity commands for the touched workflow and commit:
  ```bash
  git add .github/workflows/ README.md
  git commit -m "ci: final workflow polish (triggers/concurrency/badge alignment)"
  ```
  If all three items already hold, this step is a recorded no-op — note it and move on.

- [ ] **Step 6: Legacy-reference sweep — the old world is really gone.**
  ```bash
  git ls-files | grep -E '^(app|buildSrc|utils|feature_base)/' || echo "OLD_TREE_GONE"
  git grep -nE 'in\.technowolf|gitlab\.com/technowolf|konsist|feature_base' -- ':!docs/superpowers' || echo "NO_LEGACY_REFS"
  git ls-files | cut -d/ -f1 | sort -u
  ```
  Expected: `OLD_TREE_GONE`, `NO_LEGACY_REFS`, and a top-level listing containing only the new
  world — root config files (`.editorconfig`, `.gitignore`, `README.md`, `CONTRIBUTING.md`,
  `CODE_OF_CONDUCT.md`, `LICENSE`, `renovate.json`, `settings.gradle.kts`, `build.gradle.kts`,
  `gradle.properties`, `gradlew`, `gradlew.bat`), `.github`, `androidApp`, `build-logic`,
  `client`, `crypto`, `desktopApp`, `docs`, `feature`, `gradle`, `images`, `shared`,
  `steganography`, `webApp`, the iosApp scaffold, and whichever root assets the user kept in
  Task 2. Anything else on the list needs an explanation or a deletion.

- [ ] **Step 7: Confirm the tree is fully committed and report completion.**
  ```bash
  git status --porcelain
  git log --oneline -10
  ```
  Expected: empty status; the log shows this phase's conventional commits on top. If any check
  in Steps 1–6 required a fix, commit it as
  `git commit -m "chore: fix final-tree verification finding (<short description>)"`.
  Phase 08 — and the rewrite plan set — is complete. Branch integration (merge/PR to `develop`)
  is the user's call: follow superpowers:finishing-a-development-branch; do not push or open a
  PR without being asked.
