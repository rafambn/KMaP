# Usage

Here is a basic implementation of KMaP where it uses the OpenStreetMap tile generation to show the map on the screen.

You can encounter a demoApp on the [KMaP Repo](https://github.com/rafambn/KMaP). 

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

## How it Works

There are four parts of the KMaP: MotionController, MapState, LazyCanvas, and the KMaPContent.

* **MotionController**: Handle the movement of the map like zooming, panning, and rotating for either user or app input.
* **MapState**: Control all properties of the map and defines the visible tiles for canvas rendering.
* **LazyCanvas**: Place each composable defined on KMaPContent on the layout.
* **KMaPContent**: Based on the user provided components: canvas, marker, cluster, and path. Creates the necessary composable for a map to work.

## MapState

This is where almost all the important stuff is. It expects an implementation MapProperties interface that will tell how the map will
behave, for example, tile size, zoom level and projection transformation function.
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
It will also hold the angle, zoom, position variables that can be used by the user to display map info.
The MapState also calculates the visible tiles so that each canvas could render its
own images.
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

It has three movement options:

* Move: This will set/add/remove the provided values on the map parameters
* Animate: This will animate the map parameters to the provided value

While Move is Synchronous the Animate is Async and thus must be launched from a coroutine.

### Reference

Before seeing how to move and animate, you have to be aware of how the reference system works. It's an open class that implements all necessary types
of reference points on the screen.

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
* DifferentialScreenOffset is the same as ScreenOffset but is differential

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

Here are some examples of how to Set with MotionController. *See the comments to fully understand what each line does

``` yaml
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

``` kotlin
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

## Canvas

The Canvas function usable in the KMaP scope is just a wrapper on the native canvas so that we can handle the render 
process needing you to provide the source tile implementing the following api.

```kotlin
interface TileSource {
    suspend fun getTile(zoom: Int, row: Int, column: Int): TileRenderResult
}
```

With this function you can render any tilled map you want. Maybe you want a free map of the world with OSM, or don't 
like to use a Google library but want to use its tiles, or render it on the device, or read from a local file for your 
Skyrim map (I'm old sorry xD) with this you can do it all.

It's provided a MapGestureWrapper with a some gesture that later will be attributed on a pointerInputScope

```kotlin
MapGestureWrapper(
    onDoubleTap = { offset -> motionController.move { zoomByCentered(-1 / 3F, offset) } },
    onTapSwipe = { zoomChange, rotationChange ->
        motionController.move {
            zoomBy(zoomChange / 120)
            rotateBy(rotationChange)
        }
    },
    onTwoFingersTap = { offset -> motionController.move { zoomByCentered(1 / 3F, offset) } },
    onGesture = { centroid, pan, zoom, rotation ->
        motionController.move {
            rotateByCentered(rotation.toDouble(), centroid)
            zoomByCentered(zoom / gestureScale, centroid)
            positionBy(pan)
        }
    },
    onScroll = { mouseOffset, scrollAmount -> motionController.move { zoomByCentered(scrollAmount / scrollScale, mouseOffset) } },
)
```

See https://wiki.openstreetmap.org/wiki/Slippy_map to better understand how it works.

## Markers

Markers are a powerful way to draw anything on the map, instead of a bitmap or vector, like other libraries, it uses
composable instead this way you draw whatever you want. Declare it on the KMaP Scope for it to work.

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

Use the markers() API to draw a list of markers with the provided composable, you can set up a bunch of them with this.
There are a lot of options on how to handle your marker. Fell free to toy with them. Bellow is the data class that stores  
the options and its default values.

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

With cluster, you can merge markers if they are overlying, for example. Declare it on the KMaP scope with the clusterId
of the markers you want to cluster for it to work.

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

Cluster composable do not behave the same as the markers, so you have to provide how it will behave with its parameters.
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

Paths are similar to markers in regards that they are also composable. The key difference is that for it to be "clickable" due to the nature of Compose, you need it to 
have a pointerInputScope that also shares its inputs with its siblings, otherwise you will click outside the path and the map won't respond to your click. 
Check the SharedSuspendingPointerInput file on the project if you want to dive onto how it works.

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
