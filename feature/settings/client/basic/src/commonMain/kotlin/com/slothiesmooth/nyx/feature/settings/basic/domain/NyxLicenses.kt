package com.slothiesmooth.nyx.feature.settings.basic.domain

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Hand-maintained list of the third-party libraries Nyx ships. Keep in sync with the version catalog. */
val nyxLicenses: ImmutableList<LicenseEntry> = persistentListOf(
    LicenseEntry("Kotlin", "Apache-2.0", "https://github.com/JetBrains/kotlin"),
    LicenseEntry(
        name = "Jetpack Compose Multiplatform",
        license = "Apache-2.0",
        url = "https://github.com/JetBrains/compose-multiplatform",
    ),
    LicenseEntry("kotlinx.coroutines", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    LicenseEntry("kotlinx.serialization", "Apache-2.0", "https://github.com/Kotlin/kotlinx.serialization"),
    LicenseEntry("kotlinx-datetime", "Apache-2.0", "https://github.com/Kotlin/kotlinx-datetime"),
    LicenseEntry(
        name = "kotlinx.collections.immutable",
        license = "Apache-2.0",
        url = "https://github.com/Kotlin/kotlinx.collections.immutable",
    ),
    LicenseEntry("Koin", "Apache-2.0", "https://github.com/InsertKoinIO/koin"),
    LicenseEntry("SQLDelight", "Apache-2.0", "https://github.com/cashapp/sqldelight"),
    LicenseEntry("cryptography-kotlin", "Apache-2.0", "https://github.com/whyoleg/cryptography-kotlin"),
    LicenseEntry("FileKit", "MIT", "https://github.com/vinceglb/FileKit"),
    LicenseEntry("Kermit", "Apache-2.0", "https://github.com/touchlab/Kermit"),
    LicenseEntry("JetBrains Mono", "OFL-1.1", "https://github.com/JetBrains/JetBrainsMono"),
)
