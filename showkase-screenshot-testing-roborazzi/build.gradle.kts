import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.airbnb.android.showkase.screenshot.testing.roborazzi"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":showkase"))
    api(libs.compose.foundation)

    // Roborazzi APIs are exposed via the generated test impl in consumers, but the
    // hand-written wrapper types here also reference them at compile time, so they
    // need to be on the api classpath of the consumer's test source set.
    api(libs.test.roborazzi)
    api(libs.test.roborazziCompose)
    api(libs.test.roborazziJunitRule)
    api(libs.test.robolectric)
    api(libs.test.junit)
    api(libs.compose.uiTest)
}

mavenPublishing {
    configure(AndroidMultiVariantLibrary(JavadocJar.Empty(), SourcesJar.Sources()))
}
