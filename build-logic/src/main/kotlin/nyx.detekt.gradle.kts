import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
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
