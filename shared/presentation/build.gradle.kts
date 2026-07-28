plugins {
    id("nyx.kmp.library")
    id("nyx.compose")
}

kotlin {
    sourceSets {
        // Intermediate source set: iOS + JVM + wasmJs share one skiko ImageBitmap decoder.
        val skikoMain = create("skikoMain") { dependsOn(commonMain.get()) }
        iosMain.get().dependsOn(skikoMain)
        jvmMain.get().dependsOn(skikoMain)
        wasmJsMain.get().dependsOn(skikoMain)
        all {
            languageSettings {
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            }
        }
        commonMain.dependencies {
            // api (not implementation): BaseViewModel publicly extends androidx.lifecycle.ViewModel and
            // the NavController extensions expose androidx.navigation.NavController receivers, so
            // downstream feature modules need these types on their compile classpath.
            api(libs.jetbrains.lifecycle.viewmodel.compose)
            api(libs.jetbrains.navigation.compose)
            // UiText.Resource.args is a public ImmutableList, so downstream modules need the type.
            api(libs.kotlinx.collections.immutable)
            // LocalLifecycleOwner (androidx.lifecycle.compose) used inside BaseViewModel.bind().
            implementation(libs.jetbrains.lifecycle.runtime.compose)
        }
    }
}

compose.resources {
    packageOfResClass = "com.slothiesmooth.nyx.shared.presentation.resources"
}
