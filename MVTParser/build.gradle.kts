@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

group = "com.rafambn"
version = "0.1.0"

kotlin {
    jvmToolchain(17)

    androidTarget()
    jvm()
    js(IR) {
        browser()
        nodejs()
        binaries.executable()
    }
    wasmJs {
        browser()
        nodejs()
        binaries.executable()
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "MVTParser"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.protobuf)
            implementation(libs.kotlinx.coroutines.core)
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
    namespace = "com.rafambn.mvtparser"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
}
