plugins {
    id("nyx.kmp.library")
}

kotlin {
    sourceSets {
        all {
            languageSettings.optIn("kotlin.io.encoding.ExperimentalEncodingApi")
        }
        commonMain.dependencies {
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            // Test-only: proves the crypto -> stego -> crypto pipeline in Task 8. Keeps :crypto
            // production code free of any :steganography dependency; both stay independent engines.
            implementation(projects.steganography)
        }
    }
}
