plugins {
    id("nyx.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.theme.client.api)
            api(projects.feature.common.client.api)
            api(projects.feature.common.client.koin)
            api(projects.shared.presentation)
            api(projects.shared.data)
            implementation(projects.shared.designLibrary)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(projects.shared.testSupport)
            implementation(projects.shared.composeTestSupport)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(projects.shared.testSupport)
        }
    }
}

compose.resources {
    packageOfResClass = "com.slothiesmooth.nyx.feature.theme.basic.resources"
}
