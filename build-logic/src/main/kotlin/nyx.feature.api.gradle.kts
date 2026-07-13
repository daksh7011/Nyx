import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("nyx.kmp.library")
    id("nyx.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "commonMainApi"(libs.findLibrary("koin-core").get())
    "commonMainApi"(libs.findLibrary("jetbrains-navigation-compose").get())
    "commonMainApi"(libs.findLibrary("kotlinx-collections-immutable").get())
    "commonMainImplementation"(libs.findLibrary("kotlinx-serialization-core").get())
}
