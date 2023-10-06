rootProject.name = "nyx"

include(
    ":app",
    ":steganography",
    ":utils",
    ":feature_base",
    ":feature_encryption",
    ":feature_decryption",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        // Added for testing local Konsist artifacts
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// Generate type safe accessors when referring to other projects eg.
// Before: implementation(project(":feature_album"))
// After: implementation(projects.featureAlbum)
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
