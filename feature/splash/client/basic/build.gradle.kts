plugins {
    id("nyx.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.splash.client.api)
            api(projects.feature.common.client.api)
            api(projects.feature.common.client.koin)
            api(projects.shared.presentation)
            implementation(projects.shared.designLibrary)
            implementation(libs.koin.compose.viewmodel)
        }
    }
}

compose.resources {
    packageOfResClass = "com.slothiesmooth.nyx.feature.splash.basic.resources"
}
