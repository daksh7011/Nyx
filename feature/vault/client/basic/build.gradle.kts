plugins {
    id("nyx.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.vault.client.api)
            api(projects.feature.common.client.api)
            implementation(projects.shared.data)
            implementation(projects.shared.designLibrary)
            implementation(projects.feature.encrypt.client.api) // EncryptRoute (empty-state CTA)
            implementation(projects.feature.decrypt.client.api) // DecryptRoute (detail: decrypt this)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(projects.shared.testSupport)
        }
    }
}
