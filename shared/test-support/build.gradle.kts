plugins {
    id("nyx.kmp.library")
}

kotlin {
    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
        // Shared by jvm + android: their `createTestSqlDriver` actual both use JdbcSqliteDriver.
        // Android host (JVM) unit tests are the only android consumer of this test-support module.
        val androidJvmMain = create("androidJvmMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        jvmMain.get().dependsOn(androidJvmMain)
        androidMain.get().dependsOn(androidJvmMain)

        commonMain.dependencies {
            api(projects.shared.data)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.coroutines.core)
            api(libs.sqldelight.runtime)
            api(libs.sqldelight.async.extensions)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}
