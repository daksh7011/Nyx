plugins {
    id("nyx.kmp.library")
    id("nyx.compose")
}

kotlin {
    android {
        namespace = "com.slothiesmooth.nyx.designlibrary"
    }

    sourceSets {
        all {
            languageSettings {
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.collections.immutable)
        }
    }
}

compose.resources {
    packageOfResClass = "com.slothiesmooth.nyx.designlibrary.resources"
}
