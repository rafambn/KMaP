package com.rafambn.kmap.mapSource.tiled.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.components.RasterCanvasParameters
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.mapProperties.CoordinatesRange
import com.rafambn.kmap.mapProperties.Latitude
import com.rafambn.kmap.mapProperties.Longitude
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.ProjectedCoordinates
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds

class CanvasKernelLifecycleTest {
    @Test
    fun newlyAddedCanvasStartsLatestRequestAndRemovalCancelsIt() = runTest(timeout = 10.seconds) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val requestStarted = CompletableDeferred<Unit>()
        val requestCancelled = CompletableDeferred<Unit>()
        try {
            val mapState = MapState(testMapProperties, coroutineScope = scope)
            mapState.setCanvasSize(Offset(512f, 512f))

            mapState.canvasKernel.refreshCanvas(
                listOf(
                    RasterCanvasParameters(id = 1) { _, _, _ ->
                        requestStarted.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            requestCancelled.complete(Unit)
                        }
                    }
                )
            )

            requestStarted.await()
            mapState.canvasKernel.refreshCanvas(emptyList())
            requestCancelled.await()
        } finally {
            scope.cancel()
        }
    }
}

private val testMapProperties = object : MapProperties {
    override val boundMap = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND)
    override val outsideTiles = OutsideTilesType.NONE
    override val zoomLevels = object : ZoomLevelRange {
        override val min = 0
        override val max = 4
    }
    override val coordinatesRange = object : CoordinatesRange {
        override val latitude = Latitude(north = 90.0, south = -90.0)
        override val longitude = Longitude(west = -180.0, east = 180.0)
    }
    override val tileSize = TileDimension(256.dp, 256.dp)

    override fun toProjectedCoordinates(coordinates: Coordinates) =
        ProjectedCoordinates(coordinates.x, coordinates.y)

    override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) =
        Coordinates(projectedCoordinates.x, projectedCoordinates.y)
}
