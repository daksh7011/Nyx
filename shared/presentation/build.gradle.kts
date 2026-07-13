plugins {
    id("nyx.kmp.library")
    id("nyx.compose")
}

kotlin {
    sourceSets {
        all {
            languageSettings {
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            }
        }
        commonMain.dependencies {
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.navigation.compose)
        }
    }
}
