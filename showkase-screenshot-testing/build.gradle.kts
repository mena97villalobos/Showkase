import com.vanniktech.maven.publish.AndroidMultiVariantLibrary

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.airbnb.android.showkase.screenshot.testing"

    defaultConfig {
        minSdk = 21
        compileSdk = 36
        targetSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/gradle/incremental.annotation.processors",
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
            )
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":showkase"))
    api(libs.compose.uiTest)
    implementation(libs.compose.core)
    implementation(libs.compose.foundation)

    testImplementation(libs.test.assertJ)
    testImplementation(libs.test.googleTruth)
    api(libs.test.junit)
    api(libs.test.androidXTestCore)
    api(libs.test.androidXTestRules)
    api(libs.test.androidxTestRunner)
}

mavenPublishing {
    configure(AndroidMultiVariantLibrary(true, true))
}
