plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
}

if (project.hasProperty("useKsp")) {
    apply(plugin = "com.google.devtools.ksp")
    extensions.configure<com.google.devtools.ksp.gradle.KspExtension> {
        arg("skipPrivatePreviews", "true")
    }
} else {
    apply(plugin = "org.jetbrains.kotlin.kapt")
    extensions.configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
        correctErrorTypes = true
        arguments {
            arg("skipPrivatePreviews", "true")
        }
    }
}

android {
    namespace = "com.airbnb.android.showkase_browser_testing"

    defaultConfig {
        minSdk = 26
        compileSdk = 36
        targetSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        buildConfigField(
            "boolean",
            "IS_RUNNING_KSP",
            if (project.hasProperty("useKsp")) "true" else "false",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
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

configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.support.appCompat)
    implementation(project(":showkase-browser-testing-submodule"))
    implementation(project(":showkase-browser-testing-submodule-2"))

    implementation(project(":showkase"))
    if (project.hasProperty("useKsp")) {
        add("ksp", project(":showkase-processor"))
    } else {
        add("kapt", project(":showkase-processor"))
    }
    implementation(project(":showkase-processor"))
    implementation(project(":showkase-screenshot-testing"))

    implementation(libs.compose.activityCompose)
    implementation(libs.compose.composeRuntime)
    implementation(libs.compose.core)
    implementation(libs.compose.foundation)
    implementation(libs.compose.tooling)
    androidTestImplementation(libs.compose.uiTest)

    implementation(libs.material.material)
    implementation(libs.material.mdcComposeThemeAdapter)

    androidTestImplementation(libs.test.junitImplementation)
    androidTestImplementation(libs.test.androidXTestCore)
    androidTestImplementation(libs.test.androidXTestRules)
    androidTestImplementation(libs.test.androidxTestRunner)
}
