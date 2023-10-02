plugins {
    id("com.android.library")
    id("local.kotlin")
}

android {
    val catalogs = extensions.getByType<VersionCatalogsExtension>()
    val libs = catalogs.named("libs")

    namespace = "in.technowolf.nyx"
    compileSdk = libs.findVersion("compileSdk").get().toString().toInt()

    defaultConfig {
        minSdk = libs.findVersion("minSdk").get().toString().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/licenses/**",
            "**/attach_hotspot_windows.dll",
            "META-INF/LGPL2.1",
        )
    }
}
