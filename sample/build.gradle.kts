plugins {
    id("com.android.application")
    id("shot")
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.airbnb.android.showkasesample"

    defaultConfig {
        applicationId = "com.airbnb.android.showkasesample"
        minSdk = 23
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

ksp {
    arg("skipPrivatePreviews", "true")
}

kotlin {
    jvmToolchain(21)
    sourceSets.configureEach {
        kotlin.srcDir("build/generated/ksp/$name/kotlin")
    }
}

dependencies {
    implementation(project(":showkase"))
    implementation(project(":sample-submodule"))
    implementation(project(":sample-submodule-2"))
    ksp(project(":showkase-processor"))
    kspAndroidTest(project(":showkase-processor"))

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
    implementation(libs.compose.materialIconsCore)
    implementation(libs.compose.savedInstanceState)
    implementation(libs.compose.uiLiveData)
    androidTestImplementation(libs.compose.uiTest)

    implementation(libs.imageLoading.picasso)

    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.junitImplementation)
    androidTestImplementation(project(":showkase-screenshot-testing-shot"))
}
