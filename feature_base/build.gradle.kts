plugins {
    id("local.library")
}

android {
    namespace = "in.technowolf.nyx.base"
}

dependencies {
    api(libs.kotlin)
    api(libs.playCore)
    api(libs.coreKtx)
    api(libs.fragmentKtx)
    api(libs.viewBindingPropertyDelegate)
    api(libs.constraintLayout)
    api(libs.appCompat)
    api(libs.recyclerView)
    api(libs.coroutines)
    api(libs.material)
    api(libs.composeMaterial)
    api(libs.accompanistFlowLayout)
    api(libs.bundles.koin)
    api(libs.bundles.retrofit)
    api(libs.bundles.navigation)
    api(libs.bundles.lifecycle)
    api(libs.bundles.room)
    api(libs.bundles.compose)
    api(libs.composeRuntime)
    api(projects.utils)

    testImplementation(libs.bundles.test)

    testRuntimeOnly(libs.junitJupiterEngine)
}
