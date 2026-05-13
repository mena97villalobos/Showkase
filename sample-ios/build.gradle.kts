plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

kotlin {
    jvmToolchain(21)

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "SampleIos"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":showkase"))
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.ui)
        }
    }
}

dependencies {
    listOf("kspIosX64", "kspIosArm64", "kspIosSimulatorArm64").forEach { configName ->
        add(configName, project(":showkase-processor"))
    }
}

ksp {
    arg("showkase.target", "common")
    arg("skipPrivatePreviews", "true")
}
