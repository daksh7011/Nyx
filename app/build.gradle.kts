import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.util.Locale

plugins {
    id("local.app")
}

val versionMajor = 0
val versionMinor = 2
val versionPatch = 0
val versionBuild = 0 // bump for dogfood builds, public betas, etc.

android {
    val catalogs = extensions.getByType<VersionCatalogsExtension>()
    val libs = catalogs.named("libs")

    namespace = "in.technowolf.nyx"

    compileSdk = libs.findVersion("compileSdk").get().toString().toInt()

    defaultConfig {
        applicationId = "in.technowolf.nyx"

        minSdk = libs.findVersion("minSdk").get().toString().toInt()

        versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild

        val patch = if (versionPatch == 0) "" else ".$versionPatch"
        versionName = "$versionMajor.$versionMinor" + patch

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        multiDexEnabled = true

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigFieldFromGradleProperty("accessKey")
        buildConfigFieldFromGradleProperty("privateKey")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles("proguard-android.txt", "proguard-rules.pro")
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.findVersion("kotlinCompilerExtensionVersion").get().toString()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.material)
    implementation(libs.constraintLayout)
    implementation(libs.navigationFragment)
    implementation(libs.navigationUiKtx)
    implementation(libs.navigationFragment)
    implementation(libs.navigationUiKtx)

    implementation(libs.timber)

    implementation(libs.coroutines)

    implementation(libs.cardview)

    implementation(libs.photopicker)

    implementation(libs.roomRuntime)
    ksp(libs.roomCompiler)
    implementation(libs.roomKtx)

    implementation(libs.coil)

    implementation(libs.android.lottie)

    implementation(libs.koin)

    androidTestImplementation(libs.androidJunit)
    androidTestImplementation(libs.espresso)
}

/**
 * Takes value from Gradle project property and sets it as Android build config property.
 */
fun ApplicationDefaultConfig.buildConfigFieldFromGradleProperty(gradlePropertyName: String) {
    val propertyValue = project.properties[gradlePropertyName] as? String
    checkNotNull(propertyValue) { "Gradle property $gradlePropertyName is null" }

    val androidResourceName = "GRADLE_${gradlePropertyName.toSnakeCase()}".uppercase(Locale.getDefault())
    buildConfigField("String", androidResourceName, propertyValue)
}

/**
 * Takes value from local project property and sets it as Android build config property
 */
fun ApplicationDefaultConfig.buildConfigFieldFromLocalProperty(localPropertyName: String) {
    val propertyValue: String? = gradleLocalProperties(rootDir).getProperty(localPropertyName)
    checkNotNull(propertyValue) { "Local property $localPropertyName is null" }

    val androidResourceName = "LOCAL_${localPropertyName.toSnakeCase()}".uppercase(Locale.getDefault())
    buildConfigField("String", androidResourceName, propertyValue)
}

fun String.toSnakeCase() = this.split(Regex("(?=[A-Z])")).joinToString("_") { it.lowercase(Locale.getDefault()) }
