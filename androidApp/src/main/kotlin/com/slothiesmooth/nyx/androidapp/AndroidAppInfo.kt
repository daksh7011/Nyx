package com.slothiesmooth.nyx.androidapp

import com.slothiesmooth.nyx.shared.data.source.AppInfo

/** Android [AppInfo]: [versionName] is the compile-time `BuildConfig.VERSION_NAME`. */
class AndroidAppInfo(override val versionName: String) : AppInfo {
    override val platformName: String = "Android"
}
