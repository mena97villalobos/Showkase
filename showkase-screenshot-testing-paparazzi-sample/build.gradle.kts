import com.android.build.gradle.AbstractAppExtension
import com.android.build.gradle.LibraryExtension

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.airbnb.android.showkase.screenshot.testing.paparazzi"

    defaultConfig {
        minSdk = 21
        compileSdk = 36
        targetSdk = 33
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
    jvmToolchain(17)
}

afterEvaluate {
    /**
     * KSP does not currently register kotlin generated sources.
     * https://github.com/google/ksp/issues/37
     */
    fun registerKspSources(variantName: String, addToModel: (java.io.File) -> Unit) {
        val outputFolder = file("build/generated/ksp/$variantName/kotlin")
        addToModel(outputFolder)
        android.sourceSets.getByName(variantName).java.srcDir(outputFolder)

        // eg "debugUnitTest"
        val testDirectoryName = "${variantName}UnitTest"
        // eg "testDebug"
        val testSourceSetName = "test${variantName.replaceFirstChar { it.uppercase() }}"

        val testSourceSet = android.sourceSets.findByName(testSourceSetName) ?: return
        val testOutputFolder = file("build/generated/ksp/$testDirectoryName/kotlin")
        testSourceSet.withGroovyBuilder {
            "kotlin" {
                invokeMethod("srcDir", testOutputFolder)
            }
        }

        // AGP lint tasks read the KSP-generated test sources but Gradle can't infer
        // the producer/consumer relationship from a srcDir registration alone. Wire
        // the dependency explicitly to satisfy validation.
        val variantCapitalized = variantName.replaceFirstChar { it.uppercase() }
        val kspTestTaskName = "ksp${variantCapitalized}UnitTestKotlin"
        listOf(
            "generate${variantCapitalized}UnitTestLintModel",
            "lintAnalyze${variantCapitalized}UnitTest",
        ).forEach { consumer ->
            tasks.matching { it.name == consumer }.configureEach {
                dependsOn(kspTestTaskName)
            }
        }
    }

    val libraryExtension = extensions.findByType(LibraryExtension::class.java)
    val appExtension = extensions.findByType(AbstractAppExtension::class.java)
    when {
        libraryExtension != null -> {
            libraryExtension.libraryVariants.all {
                registerKspSources(name) { addJavaSourceFoldersToModel(it) }
            }
        }
        appExtension != null -> {
            appExtension.applicationVariants.all {
                registerKspSources(name) { addJavaSourceFoldersToModel(it) }
            }
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
    implementation(libs.compose.savedInstanceState)
    implementation(libs.compose.uiLiveData)

    implementation(libs.imageLoading.picasso)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.junitImplementation)
    implementation(libs.test.testParameterInjector)
    testImplementation(libs.compose.uiTest)
    testImplementation(project(":showkase-screenshot-testing-paparazzi"))
}
