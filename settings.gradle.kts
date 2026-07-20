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
    ":client",
    ":feature:common:client:api",
    ":feature:common:client:koin",
    ":feature:navigation:client:api",
    ":feature:navigation:client:basic",
    ":feature:splash:client:api",
    ":feature:splash:client:basic",
    ":feature:theme:client:api",
    ":feature:theme:client:basic",
    ":feature:vault:client:api",
    ":feature:vault:client:basic",
    ":feature:encrypt:client:api",
    ":feature:encrypt:client:basic",
    ":feature:decrypt:client:api",
    ":feature:decrypt:client:basic",
    ":feature:settings:client:api",
    ":feature:settings:client:basic",
    ":androidApp",
    ":desktopApp",
    ":webApp",
    ":shared:design-library:snapshot",
)
