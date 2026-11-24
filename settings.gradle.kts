pluginManagement {
    repositories {
        // Resolve Kotlin plugin marker artifacts from the Plugin Portal first.
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") { content { includeGroup("com.github.topjohnwu.libsu") } }
    }
}
rootProject.name = "AndroidHarness"
include(":app")
if (providers.gradleProperty("distribution").orNull == "play") include(":local_gpu")
