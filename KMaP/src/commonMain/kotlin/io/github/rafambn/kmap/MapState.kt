package io.github.rafambn.kmap

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.util.lerp
import io.github.rafambn.kmap.utils.Degrees
import io.github.rafambn.kmap.utils.invertFisrt
import io.github.rafambn.kmap.utils.invertSecond
import io.github.rafambn.kmap.utils.lerp
import io.github.rafambn.kmap.utils.loopInRange
import io.github.rafambn.kmap.utils.rotate
import io.github.rafambn.kmap.utils.toCanvasReference
import io.github.rafambn.kmap.utils.toMapReference
import io.github.rafambn.kmap.utils.toPosition
import io.github.rafambn.kmap.utils.toViewportReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.pow

class MapState(
    private val coroutineScope: CoroutineScope,
    initialPosition: Position = Position.Zero,
    initialZoom: Float = 0F,
    initialRotation: Float = 0F,
    maxZoom: Int = 19,
    minZoom: Int = 0,
    val mapProperties: MapProperties = MapProperties(zoomLevels = OSMZoomlevelsRange, mapCoordinatesRange = OSMCoordinatesRange)
) : MotionInterface, CanvasSizeChangeListener {
    private var maxZoom by mutableStateOf(maxZoom.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max))
    private var minZoom by mutableStateOf(minZoom.coerceIn(mapProperties.zoomLevels.min, mapProperties.zoomLevels.max))

    internal var zoom by mutableStateOf(initialZoom)
    internal var angleDegrees by mutableStateOf(initialRotation)
    internal var mapPosition by mutableStateOf(initialPosition)

    internal val zoomLevel
        get() = floor(zoom).toInt()
    private val magnifierScale
        get() = zoom - zoomLevel + 1F
    private var canvasSize by mutableStateOf(Offset.Zero)

    internal val positionOffset by derivedStateOf {
        mapPosition.toCanvasReference(zoomLevel, mapProperties.mapCoordinatesRange)
    }

    internal val viewPort by derivedStateOf {
        canvasSize.toPosition().toViewportReference(magnifierScale, zoomLevel, angleDegrees.toDouble(), OSMCoordinatesRange, mapPosition)
    }

    internal val matrix by derivedStateOf {
        val matrix = Matrix()
        matrix.translate(canvasSize.x / 2, canvasSize.y / 2, 0F)
        matrix.rotateZ(angleDegrees)
        matrix.scale(magnifierScale, magnifierScale, 0F)
        matrix
    }

    private val flingPositionAnimatable = Animatable(0F)
    private val flingZoomAnimatable = Animatable(0f)
    private val flingRotationAnimatable = Animatable(0f)

    fun offsetToMapReference(offset: Offset): Position {
        return offset.toPosition().toMapReference(
            magnifierScale,
            zoomLevel,
            angleDegrees.toDouble(),
            mapProperties.mapCoordinatesRange
        )
    }

    fun screenOffsetToMapReference(offset: Offset): Position {
        return (canvasSize / 2F - offset).toPosition().toMapReference(
            magnifierScale,
            zoomLevel,
            angleDegrees.toDouble(),
            mapProperties.mapCoordinatesRange
        )
    }

//    fun move(position: Position) {
//        mapPosition =
//            (position.toMapReference(
//                magnifierScale,
//                zoomLevel,
//                angleDegrees.toDouble(),
//                mapProperties.mapCoordinatesRange
//            ) + mapPosition).coerceInMap()
//    }

//    fun scale(position: Position, scale: Float) {
//        if (scale != 0F) {
//            val previousMagnifierScale = magnifierScale
//            val previousZoomLevel = zoomLevel
//            zoom = (scale + zoom).coerceZoom()
//            move((position - canvasSize.toPosition() / 2.0) * (1 - ((magnifierScale / previousMagnifierScale) * (2.0.pow(zoomLevel - previousZoomLevel)))))
//        }
//    }
//
//    fun rotate(position: Position, angle: Float) {
//        if (position != Position.Zero) {
//            angleDegrees += angle
//            move(position - (canvasSize.toPosition() / 2.0) + ((canvasSize.toPosition() / 2.0) - position).rotate(angle.toDouble().toRadians()))
//        }
//    }

    private fun Position.coerceInMap(): Position {
        val x = if (mapProperties.boundMap.horizontal == MapBorderType.BOUND)
            horizontal.coerceIn(
                mapProperties.mapCoordinatesRange.longitute.west,
                mapProperties.mapCoordinatesRange.longitute.east
            )
        else
            horizontal.loopInRange(mapProperties.mapCoordinatesRange.longitute)
        val y = if (mapProperties.boundMap.vertical == MapBorderType.BOUND)
            vertical.coerceIn(
                mapProperties.mapCoordinatesRange.latitude.south,
                mapProperties.mapCoordinatesRange.latitude.north
            )
        else
            vertical.loopInRange(mapProperties.mapCoordinatesRange.latitude)
        return Position(x, y)
    }

    private fun Float.coerceZoom(): Float {
        return this.coerceIn(minZoom.toFloat(), maxZoom.toFloat())
    }

    override fun setCenter(position: Position) {
        mapPosition = position.coerceInMap()
    }

    override fun moveBy(position: Position) {
        mapPosition = (mapPosition + position).coerceInMap()
        println(mapPosition)
    }

    override fun animatePositionTo(position: Position, stiffness: Float) {
        coroutineScope.launch {
            val startPosition = mapPosition
            flingPositionAnimatable.snapTo(0F)
            flingPositionAnimatable.animateTo(1F, SpringSpec(stiffness = stiffness)) {
                mapPosition = lerp(startPosition, position, value.toDouble()).coerceInMap()
            }
        }
    }

    override fun setZoom(zoom: Float) {
        this.zoom = zoom.coerceZoom()
    }

    override fun zoomBy(zoom: Float, position: Position?) {
        val previousZoomLevel = zoomLevel
        val previousMagnifierScale = magnifierScale
        this.zoom = (zoom + this.zoom).coerceZoom()
        position?.let {
            moveBy(position * (1 - ((previousMagnifierScale / magnifierScale) * (2.0.pow(previousZoomLevel - zoomLevel)))))
        }
    }

    override fun animateZoomTo(zoom: Float, stiffness: Float, position: Position?) {
        coroutineScope.launch {
            val startZoom = this@MapState.zoom
            flingZoomAnimatable.snapTo(0F)
            flingZoomAnimatable.animateTo(1F, SpringSpec(stiffness = stiffness)) {
                zoomBy(lerp(startZoom, zoom, value) - startZoom, position)
            }
        }
    }

    override fun setRotation(angle: Degrees) {
        angleDegrees = angle.toFloat()
    }

    override fun rotateBy(angle: Degrees, position: Position?) {
        angleDegrees += angle.toFloat()
//        if (position != Position.Zero) {
//            angleDegrees += angle
//            move(position - (canvasSize.toPosition() / 2.0) + ((canvasSize.toPosition() / 2.0) - position).rotate(angle.toDouble().toRadians()))
//        }
    }

    override fun animateRotationTo(angle: Degrees, stiffness: Float, position: Position?) {
        coroutineScope.launch {
            val startRotation = angleDegrees
            flingRotationAnimatable.snapTo(0F)
            flingRotationAnimatable.animateTo(1F, SpringSpec(stiffness = stiffness)) {
                angleDegrees = lerp(startRotation, angle.toFloat(), value)
//        if (position != Position.Zero) {
//            angleDegrees += angle
//            move(position - (canvasSize.toPosition() / 2.0) + ((canvasSize.toPosition() / 2.0) - position).rotate(angle.toDouble().toRadians()))
//        }
            }
        }
    }

    override fun setPosZoomRotate(position: Position, zoom: Float, angle: Degrees) {
        setCenter(position)
        setZoom(zoom)
        setRotation(angle)
    }

    override fun animatePosZoomRotate(position: Position, zoom: Float, angle: Degrees, stiffness: Float) {
        animatePositionTo(position, stiffness)
        animateZoomTo(zoom, stiffness)
        animateRotationTo(angle, stiffness)
    }

    override fun onCanvasSizeChanged(size: Offset) {
        canvasSize = size
    }
}

@Composable
inline fun rememberCameraState(
    coroutineScope: CoroutineScope,
    crossinline init: MapState.() -> Unit = {}
): MapState = remember {
    MapState(coroutineScope).apply(init)
}