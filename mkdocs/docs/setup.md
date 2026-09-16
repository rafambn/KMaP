# Setup

The Kotlin Toolchain uses Maven Central by default. Add KMaP to your module's `module.yaml`:

```yaml
dependencies:
  - com.rafambn:KMaP:0.5.0
```

For Gradle-based consumers, use the equivalent `commonMain` dependency:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.rafambn:KMaP:0.5.0")
        }
    }
}
```

!!! note "Current version: 0.5.0 ([releases](https://github.com/rafambn/kmap/releases))."
