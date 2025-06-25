import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
    id("com.vanniktech.maven.publish") version "0.32.0"
}

group = "com.rafambn"
version = "0.2.0"

kotlin {
    jvmToolchain(17)

    androidTarget{ publishLibraryVariants("release") }
    jvm()
    js(IR) {
        browser {
            webpackTask {
                mainOutputFileName = "KMaP.js"
            }
        }
        nodejs()
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
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
            baseName = "KMaP"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].compileTaskProvider.configure {
            compilerOptions {
                freeCompilerArgs.add("-Xexport-kdoc")
            }
        }
    }
}

android {
    namespace = "com.rafambn"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
}

mavenPublishing {
    coordinates(
        groupId = "com.rafambn",
        artifactId = "KMaP",
        version = "0.3.0"
    )

// Configure POM metadata for the published artifact
    pom {
        name.set("KMaP")
        description.set("A flexible and powerful compose multiplatform mapping library.")
        url.set("https://kmap.rafambn.com")

        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("rafambn")
                name.set("Rafael Mendonca")
                email.set("rafambn@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/rafambn/KMaP")
        }
    }

// Configure publishing to Maven Central
    publishToMavenCentral(host = SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)

// Enable GPG signing for all publications
    signAllPublications()

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
            androidVariantsToPublish = listOf("release"),
        )
    )
}
