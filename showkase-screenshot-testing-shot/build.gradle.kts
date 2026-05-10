import com.vanniktech.maven.publish.AndroidMultiVariantLibrary

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("shot")
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.airbnb.android.showkase.screenshot.testing.shot"

    defaultConfig {
        testApplicationId = "com.airbnb.android.showkase.screenshot.testing.shot"
        minSdk = 21
        compileSdk = 36
        targetSdk = 33
        testInstrumentationRunner = "com.karumi.shot.ShotTestRunner"
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

shot {
    applicationId = "com.airbnb.android.showkase.screenshot.testing.shot"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":showkase"))
    api(libs.compose.uiTest)
    api(project(":showkase-screenshot-testing"))
    api(libs.test.shotAndroid)

    api(libs.test.androidXTestCore)
    api(libs.test.androidXTestRules)
    api(libs.test.androidxTestRunner)
}

mavenPublishing {
    configure(AndroidMultiVariantLibrary(true, true))
}
