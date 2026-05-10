pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Showkase"
include(
    ":showkase",
    ":showkase-processor",
    ":showkase-processor-testing",
    ":showkase-annotation",
    ":sample",
    ":sample-submodule",
    ":sample-submodule-2",
    ":showkase-screenshot-testing",
    ":showkase-browser-testing",
    ":showkase-browser-testing-submodule",
    ":showkase-browser-testing-submodule-2",
    ":showkase-screenshot-testing-shot",
    ":showkase-screenshot-testing-paparazzi-sample",
    ":showkase-screenshot-testing-paparazzi",
)
