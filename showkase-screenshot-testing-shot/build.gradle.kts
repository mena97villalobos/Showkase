import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("com.android.library")
    id("shot")
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.airbnb.android.showkase.screenshot.testing.shot"

    defaultConfig {
        testApplicationId = "com.airbnb.android.showkase.screenshot.testing.shot"
        minSdk = 23
        compileSdk = 36
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
    lint {
        baseline = file("lint-baseline.xml")
    }
}

shot {
    applicationId = "com.airbnb.android.showkase.screenshot.testing.shot"
}

kotlin {
    jvmToolchain(21)
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
    configure(AndroidMultiVariantLibrary(JavadocJar.Empty(), SourcesJar.Sources()))
}
