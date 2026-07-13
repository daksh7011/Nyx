rootProject.name = "nyx"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Module includes are added incrementally in Tasks 10-13.

include(
    ":crypto",
    ":steganography",
    ":shared:data",
    ":shared:test-support",
    ":shared:presentation",
    ":shared:compose-test-support",
    ":shared:design-library",
)
