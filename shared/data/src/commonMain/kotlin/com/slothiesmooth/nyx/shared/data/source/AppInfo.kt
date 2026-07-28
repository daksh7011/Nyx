package com.slothiesmooth.nyx.shared.data.source

/**
 * Read-only application identity for the About screen. Implemented per platform entry module
 * (Android in this phase; desktop/web/iOS in plan 07) and injected via the platform Koin module.
 */
interface AppInfo {
    val versionName: String
    val platformName: String
}
