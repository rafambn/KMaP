@file:OptIn(ExperimentalWasmDsl::class)

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "com.rafambn"
version = "0.4.2"
val graphiteSurfaceDependency: Any =
    findProject(":graphite-surface") ?: libs.graphite.surface

kotlin {
    jvmToolchain(17)
    android {
        namespace = "com.rafambn.KMaP"
        compileSdk = 37
        minSdk = 24
    }
    jvm()
    js {
        browser()
        nodejs()
        compilerOptions { useEsClasses = true }
    }
    wasmJs {
        browser()
        nodejs()
        d8()
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "KMaP"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.animation)
            implementation(libs.foundation)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.protobuf)
            implementation(libs.kotlinx.serialization.json)
        }

        val graphiteMain = create("graphiteMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(graphiteSurfaceDependency)
            }
        }

        androidMain.get().dependsOn(graphiteMain)
        jvmMain.get().dependsOn(graphiteMain)
        jsMain.get().dependsOn(graphiteMain)
        wasmJsMain.get().dependsOn(graphiteMain)
        macosArm64Main.get().dependsOn(graphiteMain)
        val iosMain = maybeCreate("iosMain").apply {
            dependsOn(graphiteMain)
        }
        iosArm64Main.get().dependsOn(iosMain)
        iosSimulatorArm64Main.get().dependsOn(iosMain)

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
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

mavenPublishing {
    coordinates(
        groupId = "com.rafambn",
        artifactId = "KMaP",
        version = "0.4.2"
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
    publishToMavenCentral(automaticRelease = false)

// Enable GPG signing for all publications
    signAllPublications()

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = SourcesJar.Sources(),
        )
    )
}
