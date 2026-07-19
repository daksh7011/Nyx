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
            implementation(projects.feature.theme.client.api) // ThemeRoute (open theme screen)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(projects.shared.testSupport)
        }
    }
}

compose.resources {
    packageOfResClass = "com.slothiesmooth.nyx.feature.settings.basic.resources"
}
