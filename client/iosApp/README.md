# Nyx — iOS host app

The Xcode host that renders the shared Compose UI (`MainViewController` from `:client` iosMain).
It is **built on macOS only** — Kotlin/Native iOS targets do not compile on Linux, where
`kotlin.native.ignoreDisabledTargets=true` skips them.

## Build (macOS + Xcode required)

```bash
brew install xcodegen                 # once
cd client/iosApp
xcodegen generate                     # creates iosApp.xcodeproj from project.yml
open iosApp.xcodeproj                  # build & run in Xcode, or:
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15' build
```

The Kotlin `App.framework` is produced by `./gradlew :client:embedAndSignAppleFrameworkForXcode`
(wired as a pre-build script in `project.yml`).

## Verify the Kotlin/Native side directly (macOS)

```bash
./gradlew :client:compileKotlinIosSimulatorArm64
./gradlew :client:linkDebugFrameworkIosSimulatorArm64
```

`iosApp.xcodeproj` is intentionally NOT committed — it is regenerated from `project.yml` so the
repo carries a reviewable spec, not a fragile binary project file.
