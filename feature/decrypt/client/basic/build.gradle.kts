plugins {
    id("nyx.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.decrypt.client.api)
            api(projects.feature.common.client.api)
            implementation(projects.shared.designLibrary)
            implementation(projects.shared.data)
            implementation(projects.crypto)
            implementation(projects.steganography)
        }
        commonTest.dependencies {
            implementation(projects.shared.testSupport)
        }
    }
}
