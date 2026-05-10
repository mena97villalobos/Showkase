import com.vanniktech.maven.publish.AndroidMultiVariantLibrary

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

configurations.all {
    exclude(module = "httpclient")
    exclude(module = "commons-logging")
    exclude(module = "protobuf-lite")
    exclude(module = "javax.activation")
    exclude(module = "hamcrest-core")
    exclude(module = "annotation")
}

android {
    namespace = "com.airbnb.android.showkase.screenshot.testing.paparazzi"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        targetSdk = 33
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

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":showkase"))
    api(libs.compose.foundation)
    api(libs.compose.activityCompose)
    compileOnly(libs.test.paparazzi)

    api(libs.test.testParameterInjector)
    api(libs.test.androidXTestRules)
    api(libs.test.androidxTestRunner)
}

mavenPublishing {
    configure(AndroidMultiVariantLibrary(true, true))
}
