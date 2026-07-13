plugins {
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

dependencies {
    "commonMainImplementation"(compose.runtime)
    "commonMainImplementation"(compose.foundation)
    "commonMainImplementation"(compose.material3)
    "commonMainImplementation"(compose.ui)
    "commonMainImplementation"(compose.components.resources)
    "commonMainImplementation"(compose.components.uiToolingPreview)
}

compose.resources {
    publicResClass = true
    generateResClass = always
}
