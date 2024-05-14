plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    id("convention.publication")
}

group = "io.github.rafambn"
version = "1.0"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm()

    js {
        browser {
            webpackTask {
                mainOutputFileName = "KMaP.js"
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
            baseName = "KMaP"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.ktor.client.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

    }

    //https://kotlinlang.org/docs/native-objc-interop.html#export-of-kdoc-comments-to-generated-objective-c-headers
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].compilerOptions.options.freeCompilerArgs.add("-Xexport-kdoc")
    }

}

android {
    namespace = "io.github.rafambn.KMaP"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

gradle.projectsEvaluated {
    val signTasks = listOf(
        "signAndroidReleasePublication",
        "signJsPublication",
        "signJvmPublication",
        "signKotlinMultiplatformPublication"
    )

    val publishTasks = listOf(
        "publishAndroidReleasePublicationToSonatypeRepository",
        "publishJsPublicationToSonatypeRepository",
        "publishJvmPublicationToSonatypeRepository",
        "publishKotlinMultiplatformPublicationToSonatypeRepository"
    )


    //TODO check is the same logic is necessary for ios
    publishTasks.forEach { publishTask ->
        tasks.named(publishTask) {
            signTasks.forEach { signTask ->
                mustRunAfter(signTask)
            }
        }
    }
}
