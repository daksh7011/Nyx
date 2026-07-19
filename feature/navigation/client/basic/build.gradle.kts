plugins {
    id("nyx.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.navigation.client.api)
            api(projects.feature.common.client.api)
            api(projects.feature.common.client.koin)
            implementation(projects.shared.designLibrary)
            implementation(libs.koin.compose.viewmodel)
        }
    }
}
