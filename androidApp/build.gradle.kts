import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// Single source of truth for the app version: gradle/libs.versions.toml -> [versions] nyx-version.
// Bump it manually on develop before merging to master. versionCode is derived from it below.
val nyxVersionName: String = libs.versions.nyx.version.get()

// Derives a strictly-monotonic Play versionCode from a MAJOR.MINOR.PATCH string.
// Formula: major*10000 + minor*100 + patch (each of minor/patch is a 2-digit "slot", 0..99).
// 1.0.0 -> 10000, 1.1.0 -> 10100, 1.10.0 -> 11000, 2.0.0 -> 20000 — always increasing under semver.
// A pre-release qualifier (e.g. "-rc1") is ignored for the code. The require() guards make a bad
// bump fail the build instead of being permanently rejected by Google Play.
fun deriveVersionCode(semVer: String): Int {
    val parts = semVer.substringBefore('-').split('.')
    require(parts.size == 3) { "nyx-version must be MAJOR.MINOR.PATCH, got '$semVer'" }
    val (major, minor, patch) = parts.map(String::toInt)
    require(minor in 0..99 && patch in 0..99) {
        "nyx-version minor/patch must be in 0..99 to keep versionCode monotonic (got '$semVer')"
    }
    require(major in 0..209_999) { "nyx-version major too large: versionCode would exceed Play's 2,100,000,000 cap" }
    return major * 10_000 + minor * 100 + patch
}

// Release signing, resolved with this precedence: CI env vars first, then a git-ignored local
// keystore.properties, else null. When null the release build stays UNSIGNED (so local/CI builds
// without the key still succeed) rather than silently falling back to the debug key.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) FileInputStream(keystorePropertiesFile).use { load(it) }
}

fun signingValue(propertyKey: String, envKey: String): String? =
    (System.getenv(envKey) ?: keystoreProperties.getProperty(propertyKey))?.takeIf(String::isNotBlank)

val releaseStoreFile: File? =
    signingValue("storeFile", "ANDROID_KEYSTORE_PATH")?.let(::file)?.takeIf(File::exists)

android {
    namespace = "com.slothiesmooth.nyx"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.slothiesmooth.nyx"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        versionCode = deriveVersionCode(nyxVersionName)
        versionName = nyxVersionName
    }

    signingConfigs {
        releaseStoreFile?.let { storeFileValue ->
            create("release") {
                storeFile = storeFileValue
                storePassword = signingValue("storePassword", "ANDROID_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "ANDROID_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "ANDROID_KEY_PASSWORD")
                // minSdk 31: every target device supports v2/v3, so drop legacy v1 (JAR) signing.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        getByName("release") {
            // R8: shrink + obfuscate + optimize (proguard-android-optimize.txt is required on AGP 9;
            // proguard-android.txt was removed). Keep rules live in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            releaseStoreFile?.let { signingConfig = signingConfigs.getByName("release") }
        }
    }

    buildFeatures {
        buildConfig = true
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
    implementation(projects.client)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.core)
    implementation(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.async.extensions)
    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs)
}
