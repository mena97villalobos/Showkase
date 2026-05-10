plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
}

if (project.hasProperty("useKsp")) {
    apply(plugin = "com.google.devtools.ksp")
} else {
    apply(plugin = "org.jetbrains.kotlin.kapt")
    extensions.configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
        correctErrorTypes = true
        arguments {
            arg("multiPreviewType", "com.airbnb.android.submodule.showkasesample.LocalePreview")
            arg("multiPreviewType", "com.airbnb.android.submodule.showkasesample.FontPreview")
        }
    }
}

android {
    namespace = "com.airbnb.android.submodule.showkasesample"

    defaultConfig {
        minSdk = 21
        compileSdk = 36
        targetSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            excludes += listOf("META-INF/AL2.0", "META-INF/LGPL2.1")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.support.appCompat)
    implementation(libs.support.ktx)
    implementation(libs.support.lifecycleComposeViewModel)
    implementation(libs.support.lifecycleComposeRuntime)

    implementation(project(":showkase"))
    if (project.hasProperty("useKsp")) {
        add("ksp", project(":showkase-processor"))
    } else {
        add("kapt", project(":showkase-processor"))
    }

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
}
