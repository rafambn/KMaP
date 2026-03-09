# KMaP

A flexible and powerful compose multiplatform mapping library

<p align="center">
  <img src="/mkdocs/docs/assets/KMaP-Logo.svg" alt="KMaP-Logo" width="200" height="200">
</p>

![Version](https://img.shields.io/maven-central/v/com.rafambn/KMaP?label=Maven%20Central)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
![Platform Targets](https://img.shields.io/badge/targets-android%20%7C%20jvm%20%7C%20js%20%7C%20wasm%20%7C%20ios%20%7C%20macos-0A7EA4)

KMaP is a Kotlin Compose Multiplatform mapping library designed for shared map UIs across all KMP targets. It gives you a single composable API to build map experiences once in `commonMain`, while still supporting platform-specific runtime targets.


## Check out the demo app on wasm target: [**KMaP Demo**](https://kmap.rafambn.com/kmapdemo/).
## Check out the documentation also: [**KMaP Page**](https://kmap.rafambn.com/).

Current version [0.4.1](https://github.com/rafambn/kmap/releases).

🧭 **Project Status**

🚧 **Vector tiles paused**: Work is on hold until Compose provides async measurement + async drawing, which are needed for smooth, non-blocking rendering.  
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

[//]: # (- **Customizable Map Styles**: Ability to customize the appearance of maps, including colors, labels, and themes.)

### Usage Example

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
