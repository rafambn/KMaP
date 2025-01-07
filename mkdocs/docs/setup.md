# Setup

1. Add Maven Central to your repositories if needed

    ```kotlin
    repositories {
        mavenCentral()
    }
    ```

2. Add the desired dependencies to your module's `build.gradle` file

=== "Dependencies"

    ```kotlin
    sourceSets {
        commonMain.dependencies {
            implementation("com.rafambn:kmap:0.1.0")
        }
    }
    ```

=== "Version Catalog"

    ```toml

    [versions]

    kmap = "0.1.0"

    
    [libraries]

    kmap = { module = "com.rafambn:KMaP", version.ref = "kmap" }

    ```

!!! note "Current version [here](https://github.com/rafambn/kmap/releases)."
