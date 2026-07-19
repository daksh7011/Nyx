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
    }
}
