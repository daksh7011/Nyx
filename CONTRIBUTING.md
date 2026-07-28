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

`./gradlew build` runs assembly, all host tests, and detekt. If it is green,
your setup is correct.

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
./gradlew detektCheck      # static analysis + formatting — maxIssues=0
./gradlew build            # assemble + all host tests + detekt
./gradlew :shared:design-library:snapshot:verifyPaparazziDebug
```

Ground rules:

- **No lint suppressions to make a build pass.** A detekt finding is a design
  signal — fix the root cause. The single documented exception is
  `NxColors.kt`, the one raw-ARGB token file.
- **detekt is the single formatter/linter.** Its `formatting` ruleset (ktlint
  under the hood, `active: true`) is the source of truth — there is no separate
  spotless step. Do not add a second formatter.
- **Testing stack** is kotlin.test + kotlinx-coroutines-test + turbine + hand-
  written fakes. No mockk, no kotest. Tests serve purpose, not count.
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
