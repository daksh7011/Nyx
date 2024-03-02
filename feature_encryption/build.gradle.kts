plugins {
    id("local.library")
}

android {
    namespace = "in.technowolf.nyx.encryption"
}

dependencies {
    api(libs.kotlin)
    api(libs.koin)
    api(libs.bundles.compose)
}
