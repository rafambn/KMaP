# Setup

1. Add Maven Central to your repositories if needed

    ```kotlin
    repositories {
        mavenCentral()
    }
    ```

2. Add the desired dependencies to your module's `build.gradle.kts` file

=== "Dependencies"

    ```kotlin
    sourceSets {
        commonMain.dependencies {
            implementation("com.rafambn:KMaP:0.4.0")
        }
    }
    ```

=== "Version Catalog"

    ```toml

    [versions]

    kmap = "0.4.0"

    
    [libraries]

    kmap = { module = "com.rafambn:KMaP", version.ref = "kmap" }

    ```

!!! note "Current version: 0.4.0 ([releases](https://github.com/rafambn/kmap/releases))."
