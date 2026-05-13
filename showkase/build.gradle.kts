import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvmToolchain(21)

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("nonAndroid") {
                withJvm()
                group("ios") {
                    withIosX64()
                    withIosArm64()
                    withIosSimulatorArm64()
                }
            }
        }
    }

    @Suppress("OPT_IN_USAGE")
    android {
        namespace = "com.airbnb.android.showkase"
        compileSdk = 36
        minSdk = 23
        withHostTest {}
    }
    jvm("desktop")
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        freeCompilerArgs.add("-opt-in=androidx.compose.ui.ExperimentalComposeUiApi")
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":showkase-annotation"))
            api(project(":showkase-models"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation("org.jetbrains.compose.ui:ui-backhandler:${libs.versions.composeMultiplatform.get()}")
            implementation(libs.compose.navigation.cmp)
        }
        androidMain.dependencies {
            implementation(libs.support.appCompat)
            implementation(libs.support.ktx)
            implementation(libs.support.lifecycleComposeRuntime)
            implementation(libs.compose.activityCompose)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.test.junit)
            implementation(libs.test.googleTruth)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.airbnb.android.showkase.resources"
}

mavenPublishing {
    configure(KotlinMultiplatform(JavadocJar.Empty(), SourcesJar.Sources()))
}
