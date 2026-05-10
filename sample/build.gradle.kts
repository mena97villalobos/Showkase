plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("shot")
    alias(libs.plugins.kotlin.compose)
}

if (project.hasProperty("useKsp")) {
    apply(plugin = "com.google.devtools.ksp")
    kotlin {
        sourceSets.configureEach {
            kotlin.srcDir("build/generated/ksp/$name/kotlin")
        }
    }
} else {
    apply(plugin = "org.jetbrains.kotlin.kapt")
    extensions.configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
        correctErrorTypes = true
    }
}

android {
    namespace = "com.airbnb.android.showkasesample"

    defaultConfig {
        applicationId = "com.airbnb.android.showkasesample"
        minSdk = 21
        compileSdk = 36
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.karumi.shot.ShotTestRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/gradle/incremental.annotation.processors",
            )
        }
    }
}

if (project.hasProperty("useKsp")) {
    extensions.configure<com.google.devtools.ksp.gradle.KspExtension> {
        arg("skipPrivatePreviews", "true")
    }
} else {
    extensions.configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
        arguments {
            arg("skipPrivatePreviews", "true")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":showkase"))
    implementation(project(":sample-submodule"))
    implementation(project(":sample-submodule-2"))
    if (project.hasProperty("useKsp")) {
        add("ksp", project(":showkase-processor"))
        add("kspAndroidTest", project(":showkase-processor"))
    } else {
        add("kapt", project(":showkase-processor"))
        add("kaptAndroidTest", project(":showkase-processor"))
    }

    implementation(libs.support.appCompat)
    implementation(libs.support.ktx)
    implementation(libs.support.lifecycleComposeViewModel)
    implementation(libs.support.lifecycleComposeRuntime)

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
    androidTestImplementation(libs.compose.uiTest)

    implementation(libs.imageLoading.picasso)

    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.junitImplementation)
    androidTestImplementation(project(":showkase-screenshot-testing-shot"))
}
