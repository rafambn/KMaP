---
hide: toc
---

# Overview

## **Map everything with KMaP**: A flexible and powerful compose multiplatform mapping library

<div style="text-align: center;">
  <img src="assets/KMaP-Logo.svg" alt="KMaP-Logo" width="200" height="200">
</div>

## Checkout the demo app on wasm target: [**KMaP Demo**](https://kmap.rafambn.com/kmapdemo/).

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

With KMaP, you don't need a mapping source for each platform. Here's a simple example to get you started:

```kotlin
val mapState = rememberMapState(mapProperties = SimpleMapProperties())
KMaP(
    modifier = Modifier.fillMaxSize(),
    mapState = mapState,
) {
    canvas(
        parameters = CanvasParameters(id = 1, tileSource = SimpleMapTileSource()::getTile),
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
