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
            // DMG dropped for now (macOS shipping is deferred). Exe = consumer double-click installer,
            // Msi = Windows Installer database; both are jpackage/WiX outputs and build only on Windows.
            targetFormats(TargetFormat.Deb, TargetFormat.Exe, TargetFormat.Msi)
            packageName = "Nyx"
            // Single source of truth (libs.versions.toml). Strip any pre-release qualifier: MSI/EXE
            // require a numeric MAJOR.MINOR.BUILD, whereas nyx-version may one day carry "-rc1".
            packageVersion = libs.versions.nyx.version.get().substringBefore('-')
            // jlink omits java.sql from the runtime image by default, so the packaged app throws
            // NoClassDefFoundError: java/sql/DriverManager the moment it opens the SQLDelight vault DB
            // (SQLDelight sqlite-driver -> DriverManager). java.naming is a frequent transitive of java.sql.
            modules("java.sql", "java.naming")
        }
    }
}
