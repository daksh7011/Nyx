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
            // api (not implementation): BaseViewModel publicly extends androidx.lifecycle.ViewModel and
            // the NavController extensions expose androidx.navigation.NavController receivers, so
            // downstream feature modules need these types on their compile classpath.
            api(libs.jetbrains.lifecycle.viewmodel.compose)
            api(libs.jetbrains.navigation.compose)
        }
    }
}
