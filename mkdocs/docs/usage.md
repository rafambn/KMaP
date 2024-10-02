# Usage

Here is a basic implementation of KMaP where it uses the OpenStreetMaps tile generation to show the map on the screen.

The following Kotlin code demonstrates how to set up a motion controller and map state using the KMaP component. 

```kotlin
val motionController = rememberMotionController()
val mapState = rememberMapState(mapProperties = OSMMapProperties())
KMaP(
    modifier = Modifier.size(300.dp, 600.dp),
    motionController = motionController,
    mapState = mapState,
    canvasGestureListener = DefaultCanvasGestureListener()
) {
    canvas(tileSource =  OSMTileSource::getTile)
}
```

## How it Works

There are 4 components of the KMaP: MotionController, MapState, CanvasGestureListener and the Canvas.

* **MotionController**: Handle the movement of the map like zooming, panning, and rotating for either user input or app input.
* **MapState**: Control all properties of the map and defines the visible tiles for canvas rendering.
* **CanvasGestureListener**: Takes user inputs and process it to perform an action on the MotionController.
* **Canvas**: Render the visible tiles provide by the MapState based on a define the tile source, in this case above,
using OSMTileSource::getTile built-in function.


## MapState

This is where all the magic happen. It expects an implementation MapProperties interface that will tell how the map will
behave for example: tile size, zoom level and projection transformation function.
```kotlin
interface MapProperties {
    val boundMap: BoundMapBorder
    val outsideTiles: OutsideTilesType
    val zoomLevels: MapZoomLevelsRange
    val mapCoordinatesRange: MapCoordinatesRange
    val tileSize: Int

    fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition

    fun toProjectedCoordinates(canvasPosition: CanvasPosition): ProjectedCoordinates
}
```
It will hold the angle, zoom, position variables and calculate the visible tiles so that each canvas could render its 
own images.

## GestureListener

This open class is a basic implementation for handling user input, on the canvas only, like tap's, drag's and hover's, 
and will interface with the MotionController to perform the desired action. If the developer desires it can override 
this class and add its own custom actions while retaining if wanted the other functionalities.

## MotionController

It's responsible for handling the movement of the map like zooming, panning, and rotating for either user input or app input.

It has 3 movement options:

* Set: This will set the map parameters with the provided values
* Scroll: This will add/remove the provided values from the map parameters
* Animate: This will animate the map parameters to the provided value

While Set and Scroll are Synchronous the Animate is Async and thus must be launched from a coroutine.

### Center Location

Before seeing how to set, scroll and animate you have to be aware of the CenterLocation. It's a sealed class of the Motion Controller
that indicates reference points on the screen.

```kotlin
sealed class CenterLocation {
    data class Position(val position: CanvasPosition) : CenterLocation()
    data class Coordinates(val projectedCoordinates: ProjectedCoordinates) : CenterLocation()
    data class Offset(val offset: ScreenOffset) : CenterLocation()
}
```

* Position represents a coordinates of the map without applying the projection
* Coordinates represents a coordinates of the map
* Offset represents a screen offset with 0 - 0 located on the top left part of the screen

### Set and Scroll

Set and Scroll has its own scoped interface so it can only perform the following actions

```kotlin
interface MoveInterface {
        fun center(center: CenterLocation)
        fun zoom(zoom: Float)
        fun zoomCentered(zoom: Float, center: CenterLocation)
        fun angle(degrees: Double)
        fun rotateCentered(degrees: Double, center: CenterLocation)
    }
```

Here are some example for how to Set with MotionController. *See the comments to fully understand what each line does

``` yaml
motionController.set { 
   center(CenterLocation.Offset(ScreenOffset.Zero))  # (1)!
   center(CenterLocation.Position(CanvasPosition.Zero)) # (2)!
   center(CenterLocation.Coordinates(ProjectedCoordinates.Zero)) # (3)!
   zoom(5F) # (4)!
   zoomCentered(7F, CenterLocation.Position(CanvasPosition.Zero)) # (5)!
   rotateCentered(45.0, CenterLocation.Offset(ScreenOffset.Zero)) # (6)!
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
interface AnimationInterface {
    suspend fun positionTo(center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
    suspend fun positionBy(center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
    suspend fun zoomTo(zoom: Float, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
    suspend fun zoomBy(zoom: Float, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
    suspend fun zoomToCentered(zoom: Float, center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
    suspend fun zoomByCentered(zoom: Float, center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
    suspend fun angleTo(degrees: Double, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
    suspend fun angleBy(degrees: Double, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
    suspend fun rotateToCentered(degrees: Double, center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
    suspend fun rotateByCentered(degrees: Double, center: CenterLocation, decayRate: Double = 5.0, duration: MilliSeconds = 1000)
}
```

Animate has similar scoped functions to set and scroll with a key difference. While Set and scroll have separated function for 
moving to a place and moving by an amount, animate doesn't instead this difference can be seen in the scoped function key works to and by.

``` kotlin
CoroutineScope(Dispatchers.Default).launch {
    motionController.animate {
        positionTo(CenterLocation.Offset(Offset(5F,5F)))
        positionBy(CenterLocation.Offset(Offset(10F, 10F)))
    }
}
```

### **Behavior**

Because the Motion controller is initiated before the KMaP composable it creates scenarios in witch you mighty execute some 
movement without the KMaP response. So to fix this all Synchronous functions, off type Set and Scroll, are placed in a
queue, in the order you coded, and the last coroutine job declared is also stored to be executed when the composable comes
to be. When this happens the Motion Controller will execute all Set's and Scroll's first them the Animate Job.

After this initial phase, the movement will be executed on demand with a single difference that is if an Animate job is
executing, and you Set or Scroll that this job will be cancelled and your Synchronous function will be executed.

## Canvas

The Canvas function usable in the KMaP scope is just a wrapper on the native canvas so that we can handle the render 
process the only thing you need to do its provide the source tile implementing the following api.

```kotlin
interface TileSource {
    suspend fun getTile(zoom: Int, row: Int, column: Int): ResultTile
}
```

With this simple trick you can render any tilled map you want. Maybe you want a free map off the world with OSM, or don't 
like to use Google library but want to use its tiles, or render it on the device, or read from a local files for your 
Skyrim map (I'm old sorry) with this you can do it all. 

See https://wiki.openstreetmap.org/wiki/Slippy_map for better understand how it works.

### Built-in Sources

* OSM Mapnik

### Offline

For offline use you can either read from a .map file or a repo and render it with MapsForge or create your own renderer.

Future versions will have both built-in

## Markers

