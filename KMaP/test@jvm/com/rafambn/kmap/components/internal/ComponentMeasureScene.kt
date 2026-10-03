@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package com.rafambn.kmap.components.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.KMaP
import com.rafambn.kmap.MapState
import com.rafambn.kmap.camera.CameraState
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import com.rafambn.kmap.geometry.plane.ScreenOffset
import com.rafambn.kmap.geometry.plane.TilePoint
import com.rafambn.kmap.geometry.plane.toCoordinates
import com.rafambn.kmap.geometry.plane.toTilePoint
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.BoundaryMode
import com.rafambn.kmap.mapProperties.border.MapBoundaryBehavior
import com.rafambn.kmap.mapProperties.border.TileRepeatMode
import com.rafambn.kmap.mapProperties.coordinates.CoordinatesRange
import com.rafambn.kmap.mapProperties.coordinates.Latitude
import com.rafambn.kmap.mapProperties.coordinates.Longitude
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

internal class ComponentMeasureScene(
    camera: CameraState = CameraState(tilePoint = TilePoint(50.0, 50.0)),
    density: Float = 1F,
    repeatMode: TileRepeatMode = TileRepeatMode.NONE,
) : AutoCloseable {
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val properties = object : MapProperties {
        override val boundaryBehavior = MapBoundaryBehavior(BoundaryMode.CLAMP, BoundaryMode.CLAMP)
        override val tileRepeatMode = repeatMode
        override val zoomLevels = ZoomLevelRange(0, 4)
        override val coordinatesRange = object : CoordinatesRange {
            override val latitude = Latitude(north = 0.0, south = 100.0)
            override val longitude = Longitude(west = 0.0, east = 100.0)
        }
        override val tileSize = TileDimension(100.dp, 100.dp)

        override fun toProjectedCoordinates(coordinates: Coordinates) = ProjectedCoordinates(coordinates.x, coordinates.y)
        override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) = Coordinates(projectedCoordinates.x, projectedCoordinates.y)
    }
    val mapState = MapState(properties, initialCameraState = camera, coroutineScope = scope, density = Density(density))
    private val recomposer = FrameRecomposer(Dispatchers.Unconfined)
    private val scene = CanvasLayersComposeScene(recomposer, density = Density(density), size = IntSize(100, 100))
    val measured = mutableMapOf<String, Constraints>()
    val placed = mutableSetOf<String>()
    val bounds = mutableMapOf<String, Rect>()
    private var frameTime = 0L

    init {
        mapState.setViewportSize(IntSize(100, 100))
    }

    fun coordinates(x: Double, y: Double): Coordinates = context(mapState) {
        ScreenOffset(x, y).toTilePoint().toCoordinates()
    }

    fun render(content: KMaPContent.() -> Unit) {
        scene.setContent(recomposer.compositionContext) { KMaP(mapState, content = content) }
        advanceFrame()
    }

    fun advanceFrame() {
        placed.clear()
        bounds.clear()
        recomposer.performFrame(frameTime++)
        scene.measureAndLayout()
    }

    @Composable
    fun item(id: String, width: Int = 20, height: Int = 10) {
        Layout(modifier = Modifier.onGloballyPositioned { coordinates ->
            val corners = listOf(
                Offset.Zero,
                Offset(coordinates.size.width.toFloat(), 0F),
                Offset(0F, coordinates.size.height.toFloat()),
                Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()),
            ).map(coordinates::localToRoot)
            bounds[id] = Rect(
                corners.minOf { it.x }, corners.minOf { it.y },
                corners.maxOf { it.x }, corners.maxOf { it.y },
            )
        }) { _, constraints ->
            measured[id] = constraints
            layout(constraints.constrainWidth(width), constraints.constrainHeight(height)) { placed += id }
        }
    }

    override fun close() {
        scene.close()
        recomposer.close()
        scope.cancel()
    }
}
