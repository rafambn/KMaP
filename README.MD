# Compose Multiplatform Application

## Before running!
 - check your system with [KDoctor](https://github.com/Kotlin/kdoctor)
 - install JDK 17 on your machine
 - add `local.properties` file to the project root and set a path to Android SDK there

### Android
To run the application on android device/emulator:  
 - open project in Android Studio and run imported android run configuration

To build the application bundle:
 - run `./gradlew :composeApp:assembleDebug`
 - find `.apk` file in `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

### Desktop
Run the desktop application: `./gradlew :composeApp:run`

### iOS
To run the application on iPhone device/simulator:
 - Open `iosApp/iosApp.xcproject` in Xcode and run standard configuration
 - Or use [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio

### Browser
Run the browser application: `./gradlew :composeApp:jsBrowserDevelopmentRun`

# Template library with App

Kotlin Multiplatform Library

### Publish to MavenCentral

1) Registering a Sonatype account as described here:
   https://dev.to/kotlin/how-to-build-and-publish-a-kotlin-multiplatform-library-going-public-4a8k
2) Add developer id, name, email and the project url to
   `/convention-plugins/src/main/kotlin/convention.publication.gradle.kts`
3) Add the secrets to `local.properties`:

```
signing.keyId=...
signing.password=...
signing.secretKeyRingFile=...
ossrhUsername=...
ossrhPassword=...
```

4) Run `./gradlew :templateLibrary:publishAllPublicationsToSonatypeRepository`

### Build platform artifacts

#### Android aar

- Run `./gradlew :templateLibrary:assembleRelease`
- Output: `/templateLibrary/build/outputs/aar/templateLibrary-release.aar`

#### JVM jar

- Run `./gradlew :templateLibrary:jvmJar`
- Output: `/templateLibrary/build/libs/templateLibrary-jvm-1.0.jar`

#### iOS Framework

- Run `./gradlew :templateLibrary:linkReleaseFrameworkIosArm64`
- Output: `/templateLibrary/build/bin/iosArm64/releaseFramework/templateLibrary.framework`

#### JS file

- Run `./gradlew :templateLibrary:jsBrowserProductionWebpack`
- Output: `/templateLibrary/build/dist/js/productionExecutable/templateLibrary.js`
