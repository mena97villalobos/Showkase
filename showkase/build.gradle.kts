import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.airbnb.android.showkase"

    defaultConfig {
        minSdk = 23
        compileSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("debug") {
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
    lint {
        baseline = file("lint-baseline.xml")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    api(project(":showkase-annotation"))
    api(project(":showkase-models"))

    implementation(libs.support.appCompat)
    implementation(libs.support.ktx)
    implementation(libs.support.lifecycleComposeRuntime)

    implementation(libs.compose.activityCompose)
    implementation(libs.compose.composeRuntime)
    implementation(libs.compose.composeNavigation)
    implementation(libs.compose.core)
    implementation(libs.compose.foundation)
    implementation(libs.compose.tooling)
    implementation(libs.compose.layout)
    implementation(libs.compose.material3)
    implementation(libs.compose.materialIconsCore)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.googleTruth)
}

mavenPublishing {
    configure(AndroidMultiVariantLibrary(JavadocJar.Empty(), SourcesJar.Sources()))
}
