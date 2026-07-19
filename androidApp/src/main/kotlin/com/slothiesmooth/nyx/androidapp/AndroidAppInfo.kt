package com.slothiesmooth.nyx.androidapp

import android.content.Context
import com.slothiesmooth.nyx.shared.data.source.AppInfo

private const val UNKNOWN_VERSION = "unknown"

/**
 * Android [AppInfo]: reads the real installed package's version name via [Context.getPackageManager]
 * at runtime, so no Gradle `BuildConfig` field needs to be generated for this.
 */
class AndroidAppInfo(context: Context) : AppInfo {
    override val versionName: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: UNKNOWN_VERSION
    override val platformName: String = "Android"
}
