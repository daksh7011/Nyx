import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.client)
    implementation(compose.desktop.currentOs)
    implementation(libs.koin.core)
    implementation(libs.sqldelight.sqlite.driver)
    implementation(libs.sqldelight.async.extensions)
    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs)
    // JVM Main dispatcher (Swing EDT): BaseViewModel uses Dispatchers.Main.immediate, which throws
    // "Module with the Main dispatcher had failed to initialize" without kotlinx-coroutines-swing.
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.slothiesmooth.nyx.desktop.MainKt"

        // Release hardening via ProGuard. obfuscate=false on purpose: JVM bytecode decompiles trivially
        // so obfuscation buys no security while adding ServiceLoader/reflection crash risk and unreadable
        // stack traces. The Compose Desktop plugin auto-applies its own default rules (skiko, coroutines
        // incl. the Swing Main dispatcher, kotlinx-serialization, Material3) — compose-desktop.pro only
        // adds the app-specific keeps those defaults do NOT cover (SQLite JDBC, JDK crypto, DataStore proto).
        buildTypes.release.proguard {
            isEnabled.set(true)
            obfuscate.set(false)
            optimize.set(true)
            configurationFiles.from(project.file("compose-desktop.pro"))
        }

        nativeDistributions {
            // Exe = consumer double-click installer, Msi = Windows Installer database (both jpackage/WiX,
            // Windows-only), Deb = Linux, Dmg = macOS. Declaring a format the host cannot build is safe:
            // the plugin sets `packageTask.enabled = targetFormat.isCompatibleWithCurrentOS`, so e.g.
            // packageReleaseDmg is reported SKIPPED off macOS rather than failing the Linux/Windows jobs.
            targetFormats(TargetFormat.Deb, TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Msi)
            packageName = "Nyx"
            // Single source of truth (libs.versions.toml). Strip any pre-release qualifier: MSI/EXE
            // require a numeric MAJOR.MINOR.BUILD, whereas nyx-version may one day carry "-rc1".
            // NOTE: version validation runs for ALL declared formats on EVERY host, so the macOS rule
            // now applies on Linux/Windows too. Keep MAJOR >= 1 — jpackage rejects a leading 0
            // ("CFBundleVersion cannot be zero"), and that would fail ONLY the macOS job, late.
            packageVersion = libs.versions.nyx.version.get().substringBefore('-')
            // jlink omits java.sql from the runtime image by default, so the packaged app throws
            // NoClassDefFoundError: java/sql/DriverManager the moment it opens the SQLDelight vault DB
            // (SQLDelight sqlite-driver -> DriverManager). java.naming is a frequent transitive of java.sql.
            modules("java.sql", "java.naming")

            macOS {
                // Not strictly required for an unsigned DMG (CFBundleIdentifier would otherwise be
                // derived from mainClass), but pinned so the app identity cannot silently change if
                // mainClass is ever refactored, and because it is a prerequisite for signing later.
                bundleID = "com.slothiesmooth.nyx"
                dockName = "Nyx"
                appCategory = "public.app-category.utilities"
                // The generated Info.plist otherwise claims 10.13, which is untrue for a JDK 21
                // runtime image in an arm64-only bundle.
                minimumSystemVersion = "12.0"
                // Deliberately NO signing{} / notarization{} block: there is no Apple Developer
                // membership yet. The plugin then applies an ad-hoc signature on arm64, which is
                // load-bearing (Apple silicon refuses to run wholly unsigned arm64 code) even though
                // Gatekeeper still blocks the app. This is why the DMG is built on an arm64 runner
                // only — see the desktop-macos job in release.yml.
            }
        }
    }
}
