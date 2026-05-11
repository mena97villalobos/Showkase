import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.airbnb.android.showkase.models"

    defaultConfig {
        minSdk = 23
        compileSdk = 36
    }

    buildFeatures {
        compose = true
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":showkase-annotation"))
    api(libs.compose.composeRuntime)
    api(libs.compose.core)
}

mavenPublishing {
    configure(AndroidMultiVariantLibrary(JavadocJar.Empty(), SourcesJar.Sources()))
}
