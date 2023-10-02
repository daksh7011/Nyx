plugins {
    id("local.noui.library")
}

android {
    namespace = "in.technowolf.nyx.steganography"
}

dependencies {
    implementation(libs.coroutines)

    implementation(projects.utils)
}
