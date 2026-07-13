plugins {
    id("nyx.kmp.library")
}

kotlin {
    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
        commonMain.dependencies {
            api(projects.shared.data)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.coroutines.core)
            api(libs.sqldelight.runtime)
            api(libs.sqldelight.async.extensions)
        }
    }
}
