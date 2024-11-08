# Setup

For now the library isn't yet published because im waiting for the new version of KMP libs. So to test it in one of your projects do the 
following steps.

1.  Clone to main branch of the repo to a folder of your desire
2. Add the Project as a Subproject in settings.gradle
    ```kotlin
    include(":KMaP")
    project(":KMaP").projectDir = new File(settingsDir, "../path/to/KMaP")
    ```
3. Add as a Dependency to build.gradle
    ```kotlin
    dependencies { 
        implementation(project(":KMaP"))
    }
    ```

[//]: # (1.  Add Maven Central to your repositories if needed)

[//]: # ()
[//]: # (    ```kotlin)

[//]: # (    repositories {)

[//]: # (        mavenCentral&#40;&#41;)

[//]: # (    })

[//]: # (    ```)

[//]: # (2. Add the desired dependencies to your module's `build.gradle` file)

[//]: # ()
[//]: # (=== "Dependencies")

[//]: # ()
[//]: # (    ```kotlin)

[//]: # (      sourceSets {)

[//]: # (        commonMain.dependencies {)

[//]: # (            implementation&#40;"com.rafambn.kmap:kmap:0.1.0"&#41;)

[//]: # (        })

[//]: # (    })

[//]: # ()
[//]: # (    ```)

[//]: # ()
[//]: # (=== "Version Catalog")

[//]: # ()
[//]: # (    ```toml)

[//]: # (    [versions])

[//]: # (    kmap = "0.1.0")

[//]: # (    )
[//]: # (    [libraries])

[//]: # (    kmap = { module = "com.rafambn.kmap:kmap", version.ref = "kmap" })

[//]: # (    ```)

[//]: # ()
[//]: # (!!! note "Current version [here]&#40;https://github.com/rafambn/kmap/releases&#41;.")
