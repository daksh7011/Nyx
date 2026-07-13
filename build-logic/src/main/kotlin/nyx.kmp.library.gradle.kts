@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val nyxAndroidCompileSdk = 36
val nyxAndroidMinSdk = 24
val nyxJvmToolchain = 21

fun defaultAndroidNamespace(projectPath: String): String {
    val segments = projectPath
        .removePrefix(":")
        .split(":")
        .filter { it != "client" }
        .map { it.replace("-", "") }
    return "com.slothiesmooth.nyx." + segments.joinToString(".")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    android {
        namespace = defaultAndroidNamespace(project.path)
        compileSdk = nyxAndroidCompileSdk
        minSdk = nyxAndroidMinSdk
        withHostTestBuilder {}.configure {}
    }

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate()

    jvmToolchain(nyxJvmToolchain)

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
