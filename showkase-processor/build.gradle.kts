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
        )
    }
}

dependencies {
    implementation(project(":showkase-annotation"))

    implementation(libs.kotlinPoet)
    implementation(libs.kotlinPoetKsp)
    implementation(libs.ksp)

    testImplementation(libs.test.strikt)
    testImplementation(libs.test.junit)
    testImplementation(libs.kotlinCompileTesting)
    testImplementation(libs.kotlinCompileTestingKsp)
}

mavenPublishing {
    configure(JavaLibrary(JavadocJar.Javadoc(), true))
}
