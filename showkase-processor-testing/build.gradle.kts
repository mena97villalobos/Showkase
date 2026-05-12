plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.airbnb.android.showkase_processor_testing"

    defaultConfig {
        minSdk = 26
        compileSdk = 36
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
    lint {
        baseline = file("lint-baseline.xml")
    }
}

configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.support.appCompat)

    implementation(project(":showkase"))
    implementation(project(":showkase-processor"))
    implementation(project(":showkase-screenshot-testing"))

    implementation(libs.ksp)

    implementation(libs.compose.activityCompose)
    implementation(libs.compose.composeRuntime)
    implementation(libs.compose.core)
    implementation(libs.compose.foundation)
    implementation(libs.compose.tooling)

    implementation(libs.material.material)

    testImplementation(libs.test.assertJ)
    testImplementation(libs.test.googleTruth)
    testImplementation(libs.test.junit)
    testImplementation(libs.kotlinCompileTesting)
    testImplementation(libs.kotlinCompileTestingKsp)
    testImplementation(project(":showkase-screenshot-testing-roborazzi"))
}
