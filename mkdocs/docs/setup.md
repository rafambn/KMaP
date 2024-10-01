# Setup

1.  Add Maven Central to your repositories if needed

    ```kotlin
    repositories {
        mavenCentral()
    }
    ```
2. Add the desired dependencies to your module's `build.gradle` file

=== "Dependencies"

    ```kotlin
      sourceSets {
        val kmapVersion ="0.1.0"
        commonMain.dependencies {
            implementation("com.rafambn.kmap:kmap:$kmapVersion")
        }

        androidMain.dependencies {
            implementation("com.rafambn.kmap:kmap-android:$kmapVersion")
        }

        jvmMain.dependencies {
            implementation("com.rafambn.kmap:kmap-jvm:$kmapVersion")
        }

        jsMain.dependencies {
            implementation("com.rafambn.kmap:kmap-js:$kmapVersion")
        }

        iosMain.dependencies {
            implementation("com.rafambn.kmap:kmap-ios:$kmapVersion")
        }
    }

    ```

=== "Version Catalog"

    ```toml
    [versions]
    kmap = "0.1.0"
    
    [libraries]
    kmap = { module = "com.rafambn.kmap:kmap", version.ref = "kmap" }
    kmap-android = { module = "com.rafambn.kmap:kmap-android", version.ref = "kmap" }
    kmap-jvm = { module = "com.rafambn.kmap:kmap-jvm", version.ref = "kmap" }
    kmap-ios = { module = "com.rafambn.kmap:kmap-ios", version.ref = "kmap" }
    kmap-js = { module = "com.rafambn.kmap:kmap-js", version.ref = "kmap" }
    ```

!!! note "Current version [here](https://github.com/rafambn/kmap/releases)."
