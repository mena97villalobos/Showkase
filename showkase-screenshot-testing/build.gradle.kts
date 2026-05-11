import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.airbnb.android.showkase.screenshot.testing"

    defaultConfig {
        minSdk = 23
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
}

kotlin {
    jvmToolchain(21)
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
    configure(AndroidMultiVariantLibrary(JavadocJar.Empty(), SourcesJar.Sources()))
}
