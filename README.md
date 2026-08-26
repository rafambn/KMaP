<h1 align="center">KMaP</h1>

<p align="center">A flexible and powerful compose multiplatform mapping library</p>

<p align="center">
  <img src="/mkdocs/docs/assets/KMaP-Logo.svg" alt="KMaP-Logo" width="200" height="200">
</p>

<p align="center">
  <a href="https://search.maven.org/search?q=g:com.rafambn%20AND%20a:KMaP">
    <img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.rafambn/KMaP?label=Maven%20Central">
  </a>
  <a href="./LICENSE">
    <img alt="License" src="https://img.shields.io/badge/license-Apache%202.0-blue.svg">
  </a>
  <img alt="Platform Targets" src="https://img.shields.io/badge/targets-android%20%7C%20jvm%20%7C%20js%20%7C%20wasm%20%7C%20ios%20%7C%20macos-0A7EA4">
</p>

<p align="center">
  KMaP is a Kotlin Compose Multiplatform mapping library designed for shared map UIs across all KMP targets. It gives you a single composable API to build map experiences once in <code>commonMain</code>, while still supporting platform-specific runtime targets.
</p>


<table align="center">
  <tr>
    <td align="center">
      <a href="https://kmap.rafambn.com/kmapdemo/"><strong>KMaP Demo App (WASM)</strong></a>
    </td>
  </tr>
</table>

<table align="center">
  <tr>
    <td align="center">
      <a href="https://kmap.rafambn.com/"><strong>Documentation Page</strong></a>
    </td>
  </tr>
</table>

🧭 **Project Status**

🧪 **Vector tiles experimental**: Compatible `background`, `fill`, and `line`
styles use the Graphite renderer on Android, iOS, JVM macOS/Linux, and WebGPU browsers.
Symbols and maps with Compose overlays continue to use the Compose renderer.
✅ **Raster tiles done**: All raster features are complete and ready to use.

### Key Features

- **Cross-Platform Compatibility**: Use a single KMaP Composable in your common code for a consistent user experience across all platforms.
- **Interactive Elements**: Features like zooming, panning, and rotating maps.
- **Marker and Popup Support**: Adding markers, popups, and tooltips to maps for enhanced interactivity.
- **Layer Management**: Support for multiple layers.
- **Clustering**: Visualizing data density with clustering markers.
- **Offline Maps**: Ability to use maps without an internet connection.
- **Projection Support**: Handling different map projections and coordinate systems.
- **Performance**: Efficient rendering and handling of large datasets.
- **Easy Integration**: Seamlessly integrate KMaP into your existing compose projects.
- **Customizable**: Tailor the map's behavior to fit your needs.

### Setup

Add KMaP to your `commonMain` dependencies:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.rafambn:KMaP:0.4.1")
        }
    }
}
```

### Usage

With KMaP, you implement your map logic once. Provide a `MapProperties` and a `TileSource` and use it across targets:

```kotlin
val mapProperties = /* your MapProperties implementation */
val tileSource = /* your TileSource<RasterTile> implementation */
val mapState = rememberMapState(mapProperties = mapProperties)

KMaP(
    modifier = Modifier.fillMaxSize(),
    mapState = mapState,
) {
    rasterCanvas(
        parameters = RasterCanvasParameters(
            id = 1,
            tileSource = tileSource::getTile,
        ),
        gestureWrapper = MapGestureWrapper(
            onGesture = { centroid, pan, zoom, rotation ->
                mapState.motionController.move {
                    rotateByCentered(rotation.toDouble(), centroid)
                    zoomByCentered(zoom, centroid)
                    positionBy(pan)
                }
            },
        )
    )
}
```
