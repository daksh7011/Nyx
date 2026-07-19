plugins {
    id("nyx.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.navigation.client.api)
            api(projects.feature.common.client.api)
            implementation(projects.shared.designLibrary)
        }
    }
}
