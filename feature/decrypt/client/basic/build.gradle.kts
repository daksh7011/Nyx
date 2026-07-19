plugins {
    id("nyx.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.decrypt.client.api)
            api(projects.feature.common.client.api)
            implementation(projects.feature.common.client.koin)
            implementation(projects.shared.designLibrary)
            implementation(projects.shared.data)
            implementation(projects.crypto)
            implementation(projects.steganography)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
        }
        commonTest.dependencies {
            implementation(projects.shared.testSupport)
        }
    }
}

compose.resources {
    packageOfResClass = "com.slothiesmooth.nyx.feature.decrypt.basic.resources"
}
