# Usage

Here is a basic implementation of KMaP using a raster tile source.

You can find a demo app (including OpenStreetMap sources) in the [KMaP repo](https://github.com/rafambn/KMaP).

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

## How it Works

There are a few core pieces in KMaP: MotionController, MapState, and KMaPContent.

* **MotionController**: Handles movement like zooming, panning, and rotating for either user or app input.
* **MapState**: Holds map properties and camera state, and resolves visible tiles for rendering.
* **KMaPContent**: The DSL scope where you declare raster/vector canvases, markers, clusters, and paths.

## MapState

This is where most of the important state lives. It expects an implementation of the MapProperties interface that describes how the map
behaves, for example, tile size, zoom range, and projection transforms.
```kotlin
interface MapProperties {
    val boundMap: BoundMapBorder
    val outsideTiles: OutsideTilesType
    val zoomLevels: ZoomLevelRange
    val coordinatesRange: CoordinatesRange
    val tileSize: TileDimension

    fun toProjectedCoordinates(coordinates: Coordinates): ProjectedCoordinates

    fun toCoordinates(projectedCoordinates: ProjectedCoordinates): Coordinates
}
```
It also exposes the camera state (angle, zoom, and position) so you can display map info in your UI.
MapState calculates the visible tiles so each canvas can render its images.
```kotlin
data class CameraState(
    val canvasSize: ScreenOffset = ScreenOffset.Zero,
    val zoom: Float = 0F,
    val angleDegrees: Double = 0.0,
    val coordinates: Coordinates,
)
```

## MotionController

It's responsible for handling the movement of the map like zooming, panning, and rotating for either user input or app input.

It has two movement options:

* Move: Sets or adjusts the provided values on the map parameters.
* Animate: Animates the map parameters to the provided values.

Move is synchronous; Animate is async and must be launched from a coroutine.

### Reference

Before using move and animate, it helps to know the reference system. Reference is the base class for the coordinate types used by the API.

```kotlin
open class Reference

class ScreenOffset : Reference
class TilePoint : Reference
class Coordinates : Reference
class ProjectedCoordinates : Reference
class DifferentialScreenOffset : Reference
```

* ScreenOffset represents a screen offset with (0, 0) located on the top-left part of the main KMaP composable
* TilePoint represents a point on the map with projection and scaled to the tile
* Coordinates represent a coordinate of the map
* ProjectedCoordinates represent a coordinate of the map with its projection
* DifferentialScreenOffset is the same as ScreenOffset but represents a delta

### Move

Move has its own scoped interface, so it can only perform the following actions

```kotlin
interface MoveInterface {
    fun positionTo(center: Reference)
    fun positionBy(center: Reference)
    fun zoomTo(zoom: Float)
    fun zoomBy(zoom: Float)
    fun zoomToCentered(zoom: Float, center: Reference)
    fun zoomByCentered(zoom: Float, center: Reference)
    fun rotateTo(degrees: Double)
    fun rotateBy(degrees: Double)
    fun rotateToCentered(degrees: Double, center: Reference)
    fun rotateByCentered(degrees: Double, center: Reference)
}
```

Here are some examples of how to use MotionController.move. See the comments to understand what each line does.

```kotlin
mapState.motionController.move {
    positionTo(ScreenOffset.Zero)  # (1)!
    positionTo(TilePoint.Zero) # (2)!
    positionTo(Coordinates.Zero) # (3)!
    zoomBy(5F) # (4)!
    zoomToCentered(7F, TilePoint.Zero) # (5)!
    rotateByCentered(45.0, ScreenOffset.Zero) # (6)!
}
```

1. Center the current-provided point of the screen in the center of the screen
2. Center provided position of the canvas, without projection, in the center of the screen
3. Center provided projection in the center of the screen
4. Set the zoom amount
5. Set the zoom amount while maintaining the provided point of the same place on the screen
6. Rotate the canvas around a provided pivot point

### Animate

Now here is the scoped interface with the functions that can be implemented with Animate and an example

```kotlin
interface AnimateInterface {
    suspend fun positionTo(center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun positionBy(center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomTo(zoom: Float, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomBy(zoom: Float, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomToCentered(zoom: Float, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomByCentered(zoom: Float, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateTo(degrees: Double, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateBy(degrees: Double, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateToCentered(degrees: Double, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateByCentered(degrees: Double, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
}
```

Animate has similar scoped functions to move with a key difference; it adds an animationSpec where you can define how the animation will be performed.

```kotlin
scope.launch {
    mapState.motionController.animate {
        positionTo(Coordinates(0.0, 0.0), TweenSpec(2000))
        positionTo(Coordinates(180.0, 90.0), TweenSpec(2000))
        positionTo(Coordinates(45.0, 0.0), TweenSpec(2000))
        zoomBy(1F, TweenSpec(2000))
        zoomBy(-1F, TweenSpec(2000))
        zoomToCentered(1F, Coordinates(0.0, 0.0), TweenSpec(2000))
        rotateBy(360.0, TweenSpec(2000))
    }
    mapState.motionController.animate {
        rotateByCentered(-360.0, Coordinates(0.0, 0.0), TweenSpec(2000))
    }
}
```

## Raster and Vector Canvas

KMaP exposes two canvas types in the KMaP scope:

* **rasterCanvas**: Renders raster tiles.
* **vectorCanvas**: Renders vector tiles and requires a style.

Both rely on a TileSource implementation:

```kotlin
interface TileSource<T : Tile> {
    suspend fun getTile(zoom: Int, row: Int, column: Int): TileResult<T>
}
```

Use `RasterCanvasParameters` or `VectorCanvasParameters` when calling `rasterCanvas` or `vectorCanvas`. Vector canvases also take an `OptimizedStyle`.

With a tile source you can render any tiled map you want: OSM, custom servers, offline tiles, or device-generated tiles.

You can pass a MapGestureWrapper to handle input; KMaP wires it into the pointer input scope:

```kotlin
MapGestureWrapper(
    onDoubleTap = { offset -> mapState.motionController.move { zoomByCentered(-1 / 3F, offset) } },
    onTapSwipe = { zoomChange, rotationChange ->
        mapState.motionController.move {
            zoomBy(zoomChange / 120F)
            rotateBy(rotationChange)
        }
    },
    onTwoFingersTap = { offset -> mapState.motionController.move { zoomByCentered(1 / 3F, offset) } },
    onGesture = { centroid, pan, zoom, rotation ->
        mapState.motionController.move {
            rotateByCentered(rotation.toDouble(), centroid)
            zoomByCentered(zoom, centroid)
            positionBy(pan)
        }
    },
    onScroll = { mouseOffset, scrollAmount ->
        mapState.motionController.move { zoomByCentered(scrollAmount, mouseOffset) }
    },
)
```

See the [Slippy map](https://wiki.openstreetmap.org/wiki/Slippy_map) docs to better understand how it works.

## Markers

Markers are a powerful way to draw anything on the map. Instead of a bitmap or vector like other libraries, KMaP uses
a composable so you can draw whatever you want. Declare it in the KMaP scope.

```kotlin
marker(
    marker = MarkerParameters(
        Coordinates(0.0, 0.0),
        drawPosition = DrawPosition.TOP_RIGHT,
    )
) {
    Text(
        text = "Fixed size",
        modifier = Modifier
            .background(Color.Black)
            .padding(16.dp),
        color = Color.White
    )
}
```

Use the markers() API to draw a list of markers with the provided composable. There are a lot of options for how to
handle markers; feel free to experiment. Below is the data class that stores the options and its default values.

```kotlin
open class MarkerParameters(
    val coordinates: Coordinates,
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val zoomVisibilityRange: ClosedFloatingPointRange<Float> = 0F..Float.MAX_VALUE,
    val zoomToFix: Float? = null,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
    val clusterId: Int? = null
) : Parameters
```

## Clusters

With clusters, you can merge overlapping markers. Declare it in the KMaP scope with the clusterId
of the markers you want to group.

```kotlin
cluster(
    ClusterParameters(id = 1)
) {
    Text(
        text = "Cluster tag 1",
        modifier = Modifier
            .background(Color.Green)
            .padding(16.dp),
        color = Color.White
    )
}
```

Cluster composables do not behave the same as markers, so you have to define their behavior with parameters.
Here is the data class with the options.

```kotlin
open class ClusterParameters(
    val id: Int,
    val alpha: Float = 1F,
    val zIndex: Float = 2F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
) : Parameters
```

## Path

Paths are similar to markers in that they are also composable. The key difference is that to be "clickable" you need a
pointer input scope that shares input with its siblings, otherwise taps outside the path will stop the map from responding.
Check `SharedSuspendingPointerInput.kt` in the project if you want to dive into how it works.

```kotlin
val path1 = PathData {
    moveTo(0F, 0F)
    lineTo(180F, -80F)
    lineTo(90F, -80F)
    lineTo(90F, 0F)
}.toPath()
path(
    parameters = PathParameters(
        path = path1,
        color = Color.Red,
        style = Stroke(
            width = 4F,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        ),
    ),
    gestureWrapper = PathGestureWrapper(
        onTap = {
            markerCoordinates = with(mapState) {
                it.toTilePoint().toCoordinates()
            }
        },
    )
)
```

> **⚠️ Important Note:** 
> When defining path coordinates, you must use **projected coordinates**. The path points need to be in the same coordinate system as your map projection to render correctly on the canvas.


Similar to canvas, there are also gestures for the path that can be used.

```kotlin
data class PathGestureWrapper(
    val onTap: ((ProjectedCoordinates) -> Unit)? = null,
    val onDoubleTap: ((ProjectedCoordinates) -> Unit)? = null,
    val onLongPress: ((ProjectedCoordinates) -> Unit)? = null,
    val onHover: ((ProjectedCoordinates) -> Unit)? = null,
)
```
