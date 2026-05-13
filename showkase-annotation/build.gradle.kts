import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvmToolchain(21)

    @Suppress("OPT_IN_USAGE")
    android {
        namespace = "com.airbnb.android.showkase.annotation"
        compileSdk = 36
        minSdk = 23
    }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}

mavenPublishing {
    configure(KotlinMultiplatform(JavadocJar.Empty(), SourcesJar.Sources()))
}
