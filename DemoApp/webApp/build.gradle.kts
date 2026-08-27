@file:OptIn(ExperimentalWasmDsl::class)

import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

val graphiteWebRuntime = configurations.create("graphiteWebRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("graphite-web-runtime"))
    }
}

dependencies {
    add(
        graphiteWebRuntime.name,
        findProject(":graphite-surface") ?: libs.graphite.surface,
    )
}

configurations.configureEach {
    exclude(group = "org.jetbrains.skiko", module = "skiko-js-runtime")
    exclude(group = "org.jetbrains.skiko", module = "skiko-js-wasm-runtime")
}

kotlin {
    js {
        browser {
            commonWebpackConfig {
                configDirectory = projectDir.resolve("webpack.config.d")
            }
        }
        binaries.executable()
    }
    wasmJs {
        browser {
            commonWebpackConfig {
                configDirectory = projectDir.resolve("webpack.config.d")
            }
            @OptIn(ExperimentalDistributionDsl::class)
            distribution {
                outputDirectory = File("../../mkdocs/docs/kmapdemo")
            }
        }
        binaries.executable()
    }

    sourceSets {
        val webMain by creating {
            dependsOn(commonMain.get())
            resources.srcDir(graphiteWebRuntime)
            dependencies {
                implementation(project(":DemoApp:shared"))
                implementation(compose.runtime)
                implementation(compose.ui)
            }
        }
        jsMain {
            dependsOn(webMain)
        }
        wasmJsMain {
            dependsOn(webMain)
        }
    }
}
