plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.airbnb.android.submodule.showkasesample"

    defaultConfig {
        minSdk = 23
        compileSdk = 36
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
    jvmToolchain(21)
}

dependencies {
    implementation(libs.support.appCompat)
    implementation(libs.support.ktx)
    implementation(libs.support.lifecycleComposeViewModel)
    implementation(libs.support.lifecycleComposeRuntime)

    implementation(project(":showkase"))
    ksp(project(":showkase-processor"))

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
