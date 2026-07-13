plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.slothiesmooth.nyx.designlibrary.snapshot"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
