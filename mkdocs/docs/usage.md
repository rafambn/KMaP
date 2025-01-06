# Usage

Here is a basic implementation of KMaP where it uses the OpenStreetMaps tile generation to show the map on the screen.

You can encounter a demoApp on the [KMaP Repo](https://github.com/rafambn/KMaP). 

```kotlin
KMaP(
    modifier = Modifier.size(300.dp, 600.dp),
    mapState = mapState,
) {
    canvas(
        tileSource = OSMMapTileSource()::getTile,
        gestureDetection = {
            detectMapGestures(
                onDrag = { dragAmount -> motionController.move { positionBy(dragAmount) } },
                onScroll = { mouseOffset, scrollAmount ->
                    motionController.move { zoomByCentered(scrollAmount, mouseOffset) }
                },
                onCtrlGesture = { rotation -> motionController.move { rotateBy(rotation.toDouble()) } },
            )
        }
    )
}
```

## How it Works

There are 4 components of the KMaP: MotionController, MapState, Tile Canvas, LazyMarkers and the Path Canvas .

* **MotionController**: Handle the movement of the map like zooming, panning, and rotating for either user or app input.
* **MapState**: Control all properties of the map and defines the visible tiles for canvas rendering.
* **Tile Canvas**: Render the visible tiles provide by the MapState based on a define the tile source, in this case above,
using OSMTileSource::getTile example function.
* **Lazy Markers**: Render all visible markers lazily while clustering them if wanted.
* **Path Canvas**: Render all provided path's.


## MapState

This is where almost all the important stuff is. It expects an implementation MapProperties interface that will tell how the map will
behave for example: tile size, zoom level and projection transformation function.
```kotlin
interface MapProperties {
    val boundMap: BoundMapBorder
    val outsideTiles: OutsideTilesType
    val zoomLevels: ZoomLevelRange
    val coordinatesRange: CoordinatesRange
    val tileSize: TileDimension

    fun toTilePoint(coordinates: Coordinates): TilePoint

    fun toCoordinates(tilePoint: TilePoint): Coordinates
}
```
It will also hold the angle, zoom, position variables that can be used by the user display map info.
The MapState also calculate the visible tiles so that each canvas could render its
own images.
```kotlin
data class CameraState(
    val canvasSize: ScreenOffset = ScreenOffset.Zero,
    val zoom: Float = 0F,
    val angleDegrees: Double = 0.0,
    val coordinates: Coordinates,
    internal val tilePoint: TilePoint
)
```

## MotionController

It's responsible for handling the movement of the map like zooming, panning, and rotating for either user input or app input.

It has 3 movement options:

* Move: This will set/add/remove the provided values on the map parameters
* Animate: This will animate the map parameters to the provided value

While Move are Synchronous the Animate is Async and thus must be launched from a coroutine.

### Center Location

Before seeing how to move and animate you have to be aware of the CenterLocation. It's a sealed class of the Motion Controller
that indicates reference points on the screen.

```kotlin
sealed interface Reference

sealed class CenterLocation {
    data class TilePoint(val horizontal: Double, val vertical: Double) : Reference
    data class Coordinates(val longitude: Double, val latitude: Double) : Reference
    data class ScreenOffset(val x: Float, val y: Float) : Reference
    data class DifferentialScreenOffset(val dx: Float, val dy: Float) : Reference
}
```

* TilePoint represents a pixel on the map without projection
* Coordinates represents a coordinates of the map
* ScreenOffset represents a screen offset with 0 - 0 located on the top left part of the screen
* DifferentialScreenOffset is the same as ScreenOffset but is differential

### Move

Move has its own scoped interface so it can only perform the following actions

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

Here are some example for how to Set with MotionController. *See the comments to fully understand what each line does

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

1. Center the current provided point of the screen in the center of the screen
2. Center provided position of the canvas, without projection, in the center of the screen
3. Center provided projection in the center of the screen
4. Set the zoom amount
5. Set the zoom amount, while maintaining the provide point of the same place on the screen
6. Rotate the canvas around a provide pivot point

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

Animate has similar scoped functions to move with a key difference, it adds an animationSpec where you can define how the animation will be performed.

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

With this simple trick you can render any tilled map you want. Maybe you want a free map off the world with OSM, or don't 
like to use Google library but want to use its tiles, or render it on the device, or read from a local files for your 
Skyrim map (I'm old sorry xD) with this you can do it all.

If wanted you can also provide a gesture detection that will be input in a pointer scope internally.

```kotlin
{
    detectMapGestures(
        onDoubleTap = { offset -> motionController.move { zoomByCentered(-1 / 3F, offset) } },
        onTapLongPress = { offset -> motionController.move { positionBy(offset.asDifferentialScreenOffset()) } },
        onTapSwipe = { zoom -> motionController.move { zoomBy(zoom / 100) } },
        onDrag = { dragAmount -> motionController.move { positionBy(dragAmount) } },
        onTwoFingersTap = { offset -> motionController.move { zoomByCentered(1 / 3F, offset) } },
        onGesture = { centroid, pan, zoom, rotation ->
            motionController.move {
                rotateByCentered(rotation.toDouble(), centroid)
                zoomByCentered(zoom / gestureScale, centroid)
                positionBy(pan)
            }
        },
        onScroll = { mouseOffset, scrollAmount -> motionController.move { zoomByCentered(scrollAmount / scrollScale, mouseOffset) } },
        onCtrlGesture = { rotation -> motionController.move { rotateBy(rotation.toDouble()) } },
    )
}
```

See https://wiki.openstreetmap.org/wiki/Slippy_map for better understand how it works.

## Markers

Markers are a powerful way to draw anything on the map, instead of a bitmap or vector, like other libraries, it uses
composable instead this way you draw whatever you want. Declare it on the KMaP Scope for it to work.

```kotlin
markers(
    listOf(
        MarkerParameters(
            Coordinates(-45.949303, -21.424608),
            drawPosition = DrawPosition.BOTTOM_RIGHT,
            rotateWithMap = true,
        ),
        MarkerParameters(
            Coordinates(-46.949303, -21.424608),
            drawPosition = DrawPosition.BOTTOM_RIGHT,
            rotateWithMap = true,
        )
    )
) {
    Image(
        painter = painterResource(Res.drawable.pin),
        contentDescription = "Removable marker",
        modifier = Modifier
            .clickable {
                println("you clicked on the marker")
            }
            .background(Color.Black)
            .size(32.dp)
    )
}
```

Use the markers() API to draw a list of markers with the provided composable, you can set up a bunch of them with this.
There is a lot of options on how to handle your marker. Fell free to toy with them. Bellow is the data class that store 
the options and its default values.

```kotlin
open class MarkerParameters(
    val coordinates: Coordinates,
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val zoomToFix: Float? = null,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
    val clusterId: Int? = null
) : Parameters
```

## Clusters

With cluster, you can merge markers if they are overlying for example. Declare it on the KMaP scope with the clusterId
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

Cluster composable do not behave the same as the markers so you have to provide how it will behave with its parameters.
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

## Path Canvas (under development)

The path canvas ia a mix of a canvas and markers. For each declared path it will create a composable of the size of the map and will draw the path on it.

```kotlin
path(
    origin = Coordinates(0.0, 0.0),
    path = PathData {
        moveTo(0F, 0F)
        lineTo(100F, 100F)
        lineTo(200F, 200F)
        lineTo(100F, 200F)
        lineTo(100F, 100F)
    }.toPath(),
    color = Color.Red,
    style = Stroke(
        width = 4F,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
)
```