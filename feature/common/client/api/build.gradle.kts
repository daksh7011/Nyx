plugins {
    id("nyx.feature.api")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.presentation)
        }
    }
}
