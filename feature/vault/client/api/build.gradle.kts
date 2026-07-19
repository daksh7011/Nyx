plugins {
    id("nyx.feature.api")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.common.client.api)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
