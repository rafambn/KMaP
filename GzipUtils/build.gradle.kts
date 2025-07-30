@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
}

group = "com.rafambn"
version = "0.1.0"

kotlin {
    jvmToolchain(17)

    androidTarget()
    jvm()
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useFirefox()
                }
            }
        }
        nodejs()
        binaries.executable()
    }
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useFirefox()
                }
            }
        }
        binaries.executable()
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "GzipUtils"
            isStatic = true
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jsMain.dependencies {
            implementation(npm("pako", "2.1.0"))
        }

        wasmJsMain.dependencies {
            implementation(npm("pako", "2.1.0"))
        }
    }
}

android {
    namespace = "com.rafambn.gziputils"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
}
