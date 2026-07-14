import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

// The compose.* dependency accessors are deprecated in Compose Multiplatform 1.11
// ("Specify dependency directly"), so we resolve the artifacts from the version catalog instead.
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "commonMainImplementation"(libs.findLibrary("compose-runtime").get())
    "commonMainImplementation"(libs.findLibrary("compose-foundation").get())
    "commonMainImplementation"(libs.findLibrary("compose-material3").get())
    "commonMainImplementation"(libs.findLibrary("compose-ui").get())
    "commonMainImplementation"(libs.findLibrary("compose-components-resources").get())
    "commonMainImplementation"(libs.findLibrary("compose-ui-tooling-preview").get())
    // ComposeViewAdapter, which Android Studio's preview renderer loads; android-only.
    "androidMainImplementation"(libs.findLibrary("compose-ui-tooling").get())
}

compose.resources {
    publicResClass = true
    generateResClass = always
}
