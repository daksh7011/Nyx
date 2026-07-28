@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

val nyxAndroidCompileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
val nyxAndroidMinSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
val nyxJvmToolchain = 21

// A metadata (commonMain) klib's `unique_name` is the Kotlin module name, which KGP derives as
// `archivesName.orElse(project.name)` + "_<sourceSet>" — WITHOUT the "$group:" prefix that native,
// JS and JVM klibs get. Every ":feature:*:client:api" module is named "api" and every
// ":feature:*:client:basic" is named "basic", so they all emitted `unique_name=api_commonMain` /
// `basic_commonMain` and the KLIB loader warned about the collision. Project paths are unique, so
// deriving the archive base name from the path makes every metadata klib unique.
// Only local archive FILE names change: nothing here is published to Maven, and consumers use
// `projects.*` dependencies, which Gradle resolves by variant metadata rather than by file name.
base {
    archivesName.set(project.path.removePrefix(":").replace(":", "-"))
}

fun defaultAndroidNamespace(projectPath: String): String {
    val segments = projectPath
        .removePrefix(":")
        .split(":")
        .filter { it != "client" }
        .map { it.replace("-", "") }
    // ":client" filters down to no segments; it keeps the "client" leaf (matches the 00-INDEX table).
    val suffix = if (segments.isEmpty()) "client" else segments.joinToString(".")
    return "com.slothiesmooth.nyx.$suffix"
}

kotlin {
    android {
        namespace = defaultAndroidNamespace(project.path)
        compileSdk = nyxAndroidCompileSdk
        minSdk = nyxAndroidMinSdk
        androidResources.enable = true
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {}.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    jvm()

    // iosX64 (Intel-Mac iOS simulator) is intentionally omitted: Compose Multiplatform 1.11.1
    // and filekit 0.14.2 no longer publish an iosX64 variant, so its compilation cannot resolve
    // those artifacts. Apple Silicon simulators use iosSimulatorArm64; real devices use iosArm64.
    iosArm64()
    iosSimulatorArm64()

    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    applyDefaultHierarchyTemplate()

    jvmToolchain(nyxJvmToolchain)

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.findLibrary("kotlinx-coroutines-test").get())
            implementation(libs.findLibrary("turbine").get())
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.findLibrary("androidx-test-runner").get())
            implementation(libs.findLibrary("androidx-test-ext-junit").get())
            implementation(libs.findLibrary("junit4").get())
        }
    }
}
