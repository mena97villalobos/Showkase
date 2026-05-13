import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvmToolchain(21)

    @Suppress("OPT_IN_USAGE")
    android {
        namespace = "com.airbnb.android.showkase.models"
        compileSdk = 36
        minSdk = 23
    }
    jvm("desktop")
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":showkase-annotation"))
            api(compose.runtime)
            api(compose.ui)
        }
    }
}

mavenPublishing {
    configure(KotlinMultiplatform(JavadocJar.Empty(), SourcesJar.Sources()))
}
