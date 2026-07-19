plugins {
    id("nyx.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.encrypt.client.api)
            api(projects.feature.common.client.api)
            implementation(projects.shared.designLibrary)
        }
    }
}
