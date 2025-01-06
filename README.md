
# Overview

## **Map everything with KMaP**: A flexible and powerful compose multiplatform mapping library

<div style="text-align: center;">
  <img src="/mkdocs/docs/assets/KMaP-Logo.svg" alt="KMaP-Logo" width="200" height="200">
</div>

## Checkout the demo app on wasm target: [**KMaP Demo**](https://kmap.rafambn.com/kmapdemo/).
## Checkout the documentation also: [**KMaP Page**](https://kmap.rafambn.com/).

[//]: # (!!! note "Current version [here]&#40;https://github.com/rafambn/kmap/releases&#41;.")

### Key Features

- **Cross-Platform Compatibility**: Use a single KMaP Composable in your common code for a consistent user experience across all platforms.
- **Customizable Map Styles**: Ability to customize the appearance of maps, including colors, labels, and themes.
- **Interactive Elements**: Features like zooming, panning, and rotating maps.
- **Marker and Popup Support**: Adding markers, popups, and tooltips to maps for enhanced interactivity.
- **Layer Management**: Support for multiple layers, including vector and raster layers.
- **Clustering**: Visualizing data density with clustering markers.
- **Offline Maps**: Ability to use maps without an internet connection.
- **Projection Support**: Handling different map projections and coordinate systems.
- **Performance**: Efficient rendering and handling of large datasets.
- **Easy Integration**: Seamlessly integrate KMaP into your existing projects.
- **Customizable**: Tailor the map's appearance and behavior to fit your needs.

### Usage Example

With KMaP, you don't need a mapping source for each platform. Here's a simple example to get you started:

```kotlin
val motionController = rememberMotionController()
val mapState = rememberMapState(mapProperties = SimpleMapProperties())

KMaP(
    modifier = Modifier.fillMaxSize(),
    motionController = motionController,
    mapState = mapState,
) {
    canvas(
        tileSource = SimpleMapTileSource()::getTile,
        gestureDetection = {
            detectMapGestures(
                onDrag = { dragAmount -> motionController.move { positionBy(dragAmount) } }
            )
        }
    )
}
```