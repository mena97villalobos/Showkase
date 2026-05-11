buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${libs.versions.agp.get()}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
        classpath("com.karumi:shot:${libs.versions.shot.get()}")
        classpath("com.vanniktech:gradle-maven-publish-plugin:${libs.versions.mavenPublish.get()}")
    }
}

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotlin.compose)
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(files("$rootDir/detekt/detekt.yml"))
    }
    dependencies {
        "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
    }
}
