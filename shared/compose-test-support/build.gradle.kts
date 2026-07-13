plugins {
    id("nyx.kmp.library")
    id("nyx.compose")
}

kotlin {
    android {
        namespace = "com.slothiesmooth.nyx.shared.composetestsupport"
    }

    sourceSets {
        all {
            languageSettings {
                optIn("androidx.compose.ui.test.ExperimentalTestApi")
            }
        }
        commonMain.dependencies {
            api(projects.shared.data)
            api(projects.shared.testSupport)
            api(libs.koin.core)
            implementation("org.jetbrains.compose.ui:ui-test:${libs.versions.compose.multiplatform.get()}")
        }
    }
}
