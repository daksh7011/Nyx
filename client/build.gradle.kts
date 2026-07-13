plugins {
    id("nyx.kmp.library")
    id("nyx.compose")
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("NyxDb") {
            packageName.set("com.slothiesmooth.nyx.client.data.sqldelight")
            generateAsync.set(true)
            dialect(libs.sqldelight.dialect.sqlite338)
        }
    }
}
