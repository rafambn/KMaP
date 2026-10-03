@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package com.rafambn.kmap.gesture.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.MapState
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.components.parameters.PathParameters
import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import com.rafambn.kmap.gesture.PathGestureWrapper
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.BoundaryMode
import com.rafambn.kmap.mapProperties.border.MapBoundaryBehavior
import com.rafambn.kmap.mapProperties.border.TileRepeatMode
import com.rafambn.kmap.mapProperties.coordinates.CoordinatesRange
import com.rafambn.kmap.mapProperties.coordinates.Latitude
import com.rafambn.kmap.mapProperties.coordinates.Longitude
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.test.assertEquals

val PathGestureContentTest by testSuite {
    test("the path DSL delivers hover on the geometry and still accepts a following tap") {
        val properties = object : MapProperties {
            override val boundaryBehavior = MapBoundaryBehavior(BoundaryMode.CLAMP, BoundaryMode.CLAMP)
            override val tileRepeatMode = TileRepeatMode.NONE
            override val zoomLevels = ZoomLevelRange(0, 3)
            override val coordinatesRange = object : CoordinatesRange {
                override val latitude = Latitude(north = 0.0, south = 300.0)
                override val longitude = Longitude(west = 0.0, east = 300.0)
            }
            override val tileSize = TileDimension(300.dp, 300.dp)
            override fun toProjectedCoordinates(coordinates: Coordinates) = ProjectedCoordinates(coordinates.x, coordinates.y)
            override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) = Coordinates(projectedCoordinates.x, projectedCoordinates.y)
        }
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val mapState = MapState(properties, coroutineScope = scope, density = Density(1f))
        val hovers = mutableListOf<ProjectedCoordinates>()
        val taps = mutableListOf<ProjectedCoordinates>()
        val content = KMaPContent({
            path(
                PathParameters(Path().apply { moveTo(50f, 50f); lineTo(150f, 150f) }, Color.Black),
                PathGestureWrapper(onTap = { taps += it }, onHover = { hovers += it }),
            )
        }, mapState)
        val recomposer = FrameRecomposer(Dispatchers.Unconfined)
        val scene = CanvasLayersComposeScene(recomposer, density = Density(1f), size = IntSize(300, 300))

        try {
            scene.setContent(recomposer.compositionContext) { Box { content.paths.single().content() } }
            recomposer.performFrame(0L)
            scene.measureAndLayout()
            // The path's bounds start at 50,50, with 10 pixels of padding.
            scene.sendPointerEvent(PointerEventType.Move, Offset(35f, 35f), timeMillis = 0)
            assertEquals(listOf(ProjectedCoordinates(75.0, 75.0)), hovers)
            scene.sendPointerEvent(PointerEventType.Move, Offset(35f, 100f), timeMillis = 10)
            assertEquals(1, hovers.size)
            scene.sendPointerEvent(PointerEventType.Press, Offset(35f, 35f), timeMillis = 20)
            scene.sendPointerEvent(PointerEventType.Release, Offset(35f, 35f), timeMillis = 30)
            assertEquals(listOf(ProjectedCoordinates(75.0, 75.0)), taps)
        } finally {
            scene.close()
            recomposer.close()
            scope.cancel()
        }
    }
}
