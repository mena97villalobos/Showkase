import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=androidx.room.compiler.processing.ExperimentalProcessingApi",
            "-opt-in=com.squareup.kotlinpoet.javapoet.KotlinPoetJavaPoetPreview",
        )
    }
}

dependencies {
    implementation(project(":showkase-annotation"))

    implementation(libs.kotlinPoet)
    implementation(libs.kotlinJavaPoetInterop)
    implementation(libs.ksp)
    implementation(libs.xprocessing)

    testImplementation(libs.test.strikt)
    testImplementation(libs.test.junit)
    testImplementation(libs.xprocessingTesting)
}

mavenPublishing {
    configure(JavaLibrary(JavadocJar.Javadoc(), true))
}
