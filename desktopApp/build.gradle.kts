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
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "Nyx"
            packageVersion = "1.0.0"
        }
    }
}
