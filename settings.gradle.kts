rootProject.name = "KMaP-library-with-app"
include(":composeApp")
include(":KMaP")
includeBuild("convention-plugins")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
