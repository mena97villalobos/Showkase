plugins {
    id("com.android.library")
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.airbnb.android.showkase.screenshot.testing.paparazzi"

    defaultConfig {
        minSdk = 23
        compileSdk = 36
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

// https://github.com/cashapp/paparazzi/issues/409
tasks.withType<Test>().configureEach {
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    )
}

kotlin {
    jvmToolchain(21)
    // KSP-generated kotlin sources aren't auto-registered for kotlinc.
    // See https://github.com/google/ksp/issues/37.
    sourceSets.configureEach {
        kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/$name/kotlin"))
    }
}

// AGP lint tasks read KSP-generated test sources, but Gradle can't infer
// the producer/consumer relationship from a srcDir registration alone.
androidComponents {
    onVariants { variant ->
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
    kspAndroidTest(project(":showkase-processor"))
    kspTest(project(":showkase-processor"))
    api(project(":showkase-screenshot-testing"))

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
    implementation(libs.test.testParameterInjector)
    testImplementation(libs.compose.uiTest)
    testImplementation(libs.support.lifecycleComposeRuntime)
    testImplementation(project(":showkase-screenshot-testing-paparazzi"))
}
