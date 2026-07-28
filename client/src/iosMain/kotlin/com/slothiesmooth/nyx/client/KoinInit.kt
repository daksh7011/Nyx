package com.slothiesmooth.nyx.client

/** Called once from the Swift `App.init()` before the first [MainViewController] is shown. */
fun initKoinIos() {
    initKoin(iosPlatformModule())
}
