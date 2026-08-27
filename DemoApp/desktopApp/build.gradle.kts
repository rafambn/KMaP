import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

val graphiteSurfaceDirectory = providers.gradleProperty("graphiteSurfacePath").orNull
    ?.let(rootProject::file)
    ?: rootProject.file("../GraphiteSurface")
val localSkikoDirectory = graphiteSurfaceDirectory.resolve("skiko-fork/skiko/skiko")
val localSkikoAwtJar = localSkikoDirectory.resolve(
    "build/libs/skiko-awt-0.0.0-SNAPSHOT.jar",
)
val graphiteVulkanHostJar = tasks.register<Jar>("graphiteVulkanHostJar") {
    dependsOn(":KMaP:jvmJar")
    archiveFileName.set("graphite-vulkan-host.jar")
    destinationDirectory.set(layout.buildDirectory.dir("generated/graphiteVulkanHost"))
    from(zipTree(localSkikoAwtJar)) {
        include("org/jetbrains/skiko/graphite/**")
    }
}
val desktopOsName = when {
    System.getProperty("os.name").contains("mac", ignoreCase = true) -> "macos"
    System.getProperty("os.name").contains("win", ignoreCase = true) -> "windows"
    else -> "linux"
}
val desktopArchName = when (System.getProperty("os.arch").lowercase()) {
    "aarch64", "arm64" -> "arm64"
    else -> "x64"
}
val desktopTargetId = "$desktopOsName-$desktopArchName"
val composeSkiko = configurations.create("composeSkiko") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    resolutionStrategy.useGlobalDependencySubstitutionRules = false
}

dependencies {
    composeSkiko("org.jetbrains.skiko:skiko-awt:0.150.1")
    composeSkiko("org.jetbrains.skiko:skiko-awt-runtime-$desktopTargetId:0.150.1")
}

kotlin {
    jvmToolchain(17)
    jvm()

    sourceSets {
        jvmMain {
            dependencies {
                implementation(project(":DemoApp:shared"))
                implementation(compose.desktop.common)
                implementation(compose.desktop.currentOs)
                implementation(libs.logback.classic)
                implementation(libs.kotlinx.coroutines.swing)
                if (localSkikoDirectory.isDirectory) {
                    implementation(
                        files(
                            composeSkiko,
                            graphiteVulkanHostJar,
                            localSkikoDirectory.resolve(
                                "skiko-graphite/build/libs/skiko-graphite-awt-0.0.0-SNAPSHOT.jar",
                            ),
                            localSkikoDirectory.resolve(
                                "skiko-graphite/build/libs/" +
                                    "skiko-graphite-0.0.0-SNAPSHOT-$desktopTargetId.jar",
                            ),
                        )
                    )
                }
            }
        }
    }
}

val desktopOperatingSystem = when (desktopOsName) {
    "macos" -> OperatingSystemFamily.MACOS
    "windows" -> OperatingSystemFamily.WINDOWS
    else -> OperatingSystemFamily.LINUX
}
val desktopArchitecture = when (desktopArchName) {
    "arm64" -> MachineArchitecture.ARM64
    else -> MachineArchitecture.X86_64
}

configurations.named("jvmRuntimeClasspath") {
    attributes {
        attribute(
            OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE,
            objects.named(desktopOperatingSystem),
        )
        attribute(
            MachineArchitecture.ARCHITECTURE_ATTRIBUTE,
            objects.named(desktopArchitecture),
        )
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.rafambn.kmapdemo.desktopApp"
            packageVersion = "1.0.0"
        }
    }
}
