plugins {
    id("nyx.feature.api")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.common.client.api)
            api(projects.shared.designLibrary)
            api(projects.shared.presentation)
        }
    }
}
