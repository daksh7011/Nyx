import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("io.gitlab.arturbosch.detekt")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "detektPlugins"(libs.findLibrary("detekt-formatting").get())
}

val detektCheck by tasks.registering(Detekt::class) {
    description = "Runs detekt across all module Kotlin sources."
    group = "verification"
    parallel = true
    ignoreFailures = false
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    setSource(files(rootDir))
    include("**/*.kt")
    exclude(
        "**/build/**",
        "**/generated/**",
        "**/resources/**",
        "legacy/**",
        "build-logic/**",
    )
    reports {
        html.required.set(true)
        xml.required.set(false)
        sarif.required.set(false)
        txt.required.set(false)
        md.required.set(false)
    }
}
