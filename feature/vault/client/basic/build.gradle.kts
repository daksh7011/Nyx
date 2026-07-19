plugins {
    id("nyx.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.vault.client.api)
            api(projects.feature.common.client.api)
            implementation(projects.shared.designLibrary)
        }
    }
}
