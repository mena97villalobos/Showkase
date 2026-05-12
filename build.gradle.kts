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

    plugins.withId("com.vanniktech.maven.publish.base") {
        extensions.configure<PublishingExtension>("publishing") {
            repositories {
                maven {
                    name = "GitHubPackages"
                    val repoSlug = System.getenv("GITHUB_REPOSITORY")
                        ?: providers.gradleProperty("githubPackagesRepository").orNull
                        ?: "mena97villalobos/Showkase"
                    url = uri("https://maven.pkg.github.com/$repoSlug")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                            ?: providers.gradleProperty("gpr.user").orNull
                                    ?: ""
                        password = System.getenv("GITHUB_TOKEN")
                            ?: providers.gradleProperty("gpr.key").orNull
                                    ?: ""
                    }
                }
            }
        }
    }
}
