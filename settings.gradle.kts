rootProject.name = "KMaP"

val graphiteSurfaceDirectory = providers.gradleProperty("graphiteSurfacePath").orNull
    ?.let(::file)
    ?: file("../GraphiteSurface").takeIf { it.resolve("settings.gradle.kts").isFile }

graphiteSurfaceDirectory?.let { directory ->
    include(":graphite-surface")
    project(":graphite-surface").projectDir =
        directory.resolve("graphite-surface/graphite-surface")

    include(":graphite-engine")
    project(":graphite-engine").projectDir =
        directory.resolve("graphite-surface/graphite-engine")

    includeBuild(directory.resolve("skiko-fork/skiko/skiko")) {
        name = "skiko"
        dependencySubstitution {
            substitute(module("org.jetbrains.skiko:skiko")).using(project(":"))
            substitute(module("org.jetbrains.skiko:skiko-js")).using(project(":"))
            substitute(module("org.jetbrains.skiko:skiko-wasm-js")).using(project(":"))
            substitute(module("org.jetbrains.skiko:skiko-graphite"))
                .using(project(":skiko-graphite"))
            substitute(module("org.jetbrains.skiko:skiko-graphite-js"))
                .using(project(":skiko-graphite"))
            substitute(module("org.jetbrains.skiko:skiko-graphite-wasm-js"))
                .using(project(":skiko-graphite"))
            substitute(module("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64"))
                .using(project(":"))
            substitute(module("org.jetbrains.skiko:skiko-graphite-awt-runtime-macos-arm64"))
                .using(project(":skiko-graphite"))
            substitute(module("org.jetbrains.skiko:skiko-awt-runtime-macos-x64"))
                .using(project(":"))
            substitute(module("org.jetbrains.skiko:skiko-graphite-awt-runtime-macos-x64"))
                .using(project(":skiko-graphite"))
            substitute(module("org.jetbrains.skiko:skiko-awt-runtime-linux-arm64"))
                .using(project(":"))
            substitute(module("org.jetbrains.skiko:skiko-graphite-awt-runtime-linux-arm64"))
                .using(project(":skiko-graphite"))
            substitute(module("org.jetbrains.skiko:skiko-awt-runtime-linux-x64"))
                .using(project(":"))
            substitute(module("org.jetbrains.skiko:skiko-graphite-awt-runtime-linux-x64"))
                .using(project(":skiko-graphite"))
            substitute(module("org.jetbrains.skiko:skiko-awt-runtime-windows-arm64"))
                .using(project(":"))
            substitute(module("org.jetbrains.skiko:skiko-graphite-awt-runtime-windows-arm64"))
                .using(project(":skiko-graphite"))
            substitute(module("org.jetbrains.skiko:skiko-awt-runtime-windows-x64"))
                .using(project(":"))
            substitute(module("org.jetbrains.skiko:skiko-graphite-awt-runtime-windows-x64"))
                .using(project(":skiko-graphite"))
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
