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

kotlin {
    sourceSets {
        // Intermediate source set: iOS + JVM + wasmJs share the skiko-based image codec
        // (skiko ships with Compose Multiplatform on every non-android target). Never android.
        val skikoMain by creating { dependsOn(commonMain.get()) }
        iosMain.get().dependsOn(skikoMain)
        jvmMain.get().dependsOn(skikoMain)
        wasmJsMain.get().dependsOn(skikoMain)
        all {
            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
        commonMain.dependencies {
            api(projects.shared.data)
            api(projects.crypto)
            api(projects.steganography)
            api(projects.shared.presentation)
            api(projects.shared.designLibrary)
            api(projects.feature.common.client.api)
            api(projects.feature.common.client.koin)
            api(projects.feature.theme.client.api)
            implementation(projects.feature.theme.client.basic)
            api(projects.feature.navigation.client.api)
            implementation(projects.feature.navigation.client.basic)
            api(projects.feature.splash.client.api)
            implementation(projects.feature.splash.client.basic)
            api(projects.feature.vault.client.api)
            implementation(projects.feature.vault.client.basic)
            api(projects.feature.encrypt.client.api)
            implementation(projects.feature.encrypt.client.basic)
            api(projects.feature.decrypt.client.api)
            implementation(projects.feature.decrypt.client.basic)
            api(projects.feature.settings.client.api)
            implementation(projects.feature.settings.client.basic)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.sqldelight.async.extensions)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
        // wasmJsMain: no SqlDelight driver in v1 (web vault is in-memory).
        commonTest.dependencies {
            implementation(projects.shared.testSupport)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(projects.shared.testSupport)
        }
    }
}
