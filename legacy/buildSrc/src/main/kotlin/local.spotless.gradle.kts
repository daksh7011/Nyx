plugins {
    id("com.diffplug.spotless")
}

spotless {
    kotlin {
        target("**/*.kt", "**/*.kts")
        targetExclude("**/buildSrc/build/**/*.*")

        val disabledRules = mapOf(
            "ktlint_standard_package-name" to "disabled", // disable this rule for using `in`.technowolf.nyx package name.
        )
        ktlint()
            .editorConfigOverride(disabledRules)

        indentWithSpaces()
        endWithNewline()
    }

    // Don't add spotless as dependency for the Gradle's check task to facilitate separated codebase checks
    isEnforceCheck = false
}
