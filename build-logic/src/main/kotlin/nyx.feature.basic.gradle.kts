import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("nyx.kmp.library")
    id("nyx.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "commonMainImplementation"(libs.findLibrary("koin-core").get())
    "commonMainImplementation"(libs.findLibrary("jetbrains-navigation-compose").get())
    "commonMainImplementation"(libs.findLibrary("kotlinx-collections-immutable").get())
    "commonMainImplementation"(libs.findLibrary("kotlinx-coroutines-core").get())
    "commonMainImplementation"(libs.findLibrary("kermit").get())
}
