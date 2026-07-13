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
    // androidx.compose.ui.tooling.preview.* (Preview/PreviewParameter*). The
    // org.jetbrains.compose.ui.tooling.preview.* variants (components-ui-tooling-preview) are
    // deprecated in CMP 1.11.
    "commonMainImplementation"(libs.findLibrary("compose-ui-tooling-preview").get())
}

compose.resources {
    publicResClass = true
    generateResClass = always
}
