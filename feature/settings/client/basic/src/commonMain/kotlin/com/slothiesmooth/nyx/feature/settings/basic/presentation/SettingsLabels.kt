package com.slothiesmooth.nyx.feature.settings.basic.presentation

private const val APP_NAME = "Nyx"

/** Combines the app name, [versionName], and [platformName] into the About row's single-line label. */
fun versionLabel(versionName: String, platformName: String): String = "$APP_NAME $versionName · $platformName"
