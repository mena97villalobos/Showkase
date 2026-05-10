import com.vanniktech.maven.publish.AndroidMultiVariantLibrary

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.airbnb.android.showkase.models"

    defaultConfig {
        minSdk = 21
        compileSdk = 36
        targetSdk = 33
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":showkase-annotation"))
    api(libs.compose.composeRuntime)
    api(libs.compose.core)
}

mavenPublishing {
    configure(AndroidMultiVariantLibrary(true, true))
}
