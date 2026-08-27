rootProject.name = "KMaP"

val graphiteSurfaceDirectory = providers.gradleProperty("graphiteSurfacePath").orNull
    ?.let(::file)
    ?: file("../GraphiteSurface").takeIf { it.resolve("settings.gradle.kts").isFile }

graphiteSurfaceDirectory?.let { directory ->
    includeBuild(directory) {
        dependencySubstitution {
            substitute(module("com.rafambn:graphite-surface"))
                .using(project(":graphite-surface"))
        }
    }
}

include(":KMaP")
include(":DemoApp:shared")
include(":DemoApp:androidApp")
include(":DemoApp:desktopApp")
include(":DemoApp:webApp")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("android.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("android.*")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
}
