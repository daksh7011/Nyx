plugins {
    id("nyx.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.settings.client.api)
            api(projects.feature.common.client.api)
            api(projects.feature.common.client.koin)
            api(projects.shared.presentation)
            implementation(projects.shared.designLibrary)
            implementation(projects.shared.data)
            implementation(projects.feature.theme.client.api) // ThemeRoute (settings -> theme route-only edge)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(projects.shared.testSupport)
        }
    }
}
