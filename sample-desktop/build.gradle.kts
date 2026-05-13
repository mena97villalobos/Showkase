plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.airbnb.android.showkasesample.desktop.MainKt")
}

dependencies {
    implementation(project(":showkase"))
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.material3)
    ksp(project(":showkase-processor"))
}

ksp {
    arg("showkase.target", "common")
    arg("skipPrivatePreviews", "true")
}
