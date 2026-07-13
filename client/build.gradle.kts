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
