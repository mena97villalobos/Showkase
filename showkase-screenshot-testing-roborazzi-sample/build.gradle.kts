import com.android.build.api.variant.HasHostTests

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.airbnb.android.showkase.screenshot.testing.roborazzi.sample"

    defaultConfig {
        minSdk = 23
        compileSdk = 36
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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

kotlin {
    jvmToolchain(21)
}

// KSP-generated kotlin output isn't auto-registered with AGP's source model
// (https://github.com/google/ksp/issues/37). Wire each variant's main and
// host-test (unit-test) sources via the modern androidComponents API. The
// older variant.unitTest accessor was generalized to HasHostTests in AGP 9.
androidComponents {
    onVariants { variant ->
        variant.sources.java?.addStaticSourceDirectory(
            layout.buildDirectory.dir("generated/ksp/${variant.name}/kotlin")
                .get().asFile.absolutePath
        )
        (variant as? HasHostTests)?.hostTests?.values?.forEach { hostTest ->
            hostTest.sources.java?.addStaticSourceDirectory(
                layout.buildDirectory.dir("generated/ksp/${hostTest.name}/kotlin")
                    .get().asFile.absolutePath
            )
        }

        // AGP lint tasks read KSP-generated test sources, but Gradle can't infer
        // the producer/consumer relationship from a srcDir registration alone.
        val variantCap = variant.name.replaceFirstChar { it.uppercase() }
        val kspTestTask = "ksp${variantCap}UnitTestKotlin"
        listOf(
            "generate${variantCap}UnitTestLintModel",
            "lintAnalyze${variantCap}UnitTest",
        ).forEach { consumer ->
            tasks.matching { it.name == consumer }.configureEach { dependsOn(kspTestTask) }
        }
    }
}

dependencies {
    implementation(project(":showkase"))
    ksp(project(":showkase-processor"))
    kspTest(project(":showkase-processor"))
    api(project(":showkase-screenshot-testing"))
    api(project(":showkase-screenshot-testing-roborazzi"))

    implementation(libs.compose.activityCompose)
    implementation(libs.compose.composeRuntime)
    implementation(libs.compose.constraintLayout)
    implementation(libs.compose.core)
    implementation(libs.compose.foundation)
    implementation(libs.compose.tooling)
    implementation(libs.compose.layout)
    implementation(libs.compose.material)
    implementation(libs.compose.materialIconsCore)
    implementation(libs.compose.savedInstanceState)
    implementation(libs.compose.uiLiveData)

    implementation(libs.imageLoading.picasso)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.junitImplementation)
    testImplementation(libs.test.roborazzi)
    testImplementation(libs.test.roborazziCompose)
    testImplementation(libs.test.roborazziJunitRule)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.compose.uiTest)
}
