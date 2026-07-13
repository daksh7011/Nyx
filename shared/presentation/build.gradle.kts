plugins {
    id("nyx.kmp.library")
    id("nyx.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
        }
    }
}
