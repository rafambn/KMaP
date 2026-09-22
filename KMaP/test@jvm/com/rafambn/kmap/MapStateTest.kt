package com.rafambn.kmap

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.camera.CameraState
import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import com.rafambn.kmap.geometry.plane.ScreenOffset
import com.rafambn.kmap.geometry.plane.TilePoint
import com.rafambn.kmap.geometry.plane.toScreenOffset
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.mapProperties.coordinates.CoordinatesRange
import com.rafambn.kmap.mapProperties.coordinates.Latitude
import com.rafambn.kmap.mapProperties.coordinates.Longitude
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MapStateTest {
    @Test
    fun densityChangePreservesCoordinatesAndTilePoint() {
        val mapState = mapState(density = Density(1F))
        val coordinates = mapState.cameraState.coordinates

        mapState.updateDensity(Density(2F, 1.5F))

        assertEquals(2F, mapState.density)
        assertEquals(1.5F, mapState.fontScale)
        assertEquals(coordinates, mapState.cameraState.coordinates)
        assertEquals(TilePoint(256.0, 256.0), mapState.internalCameraState.tilePoint)
    }

    @Test
    fun densityIsAppliedWhenTilePointIsConvertedToScreen() {
        val mapState = mapState(density = Density(1F))
        mapState.setViewportSize(IntSize(800, 600))
        val point = TilePoint(266.0, 256.0)

        val initialOffset = context(mapState) { point.toScreenOffset() }
        mapState.updateDensity(Density(2F))
        val scaledOffset = context(mapState) { point.toScreenOffset() }

        assertEquals(ScreenOffset(410.0, 300.0), initialOffset)
        assertEquals(ScreenOffset(420.0, 300.0), scaledOffset)
        assertEquals(TilePoint(256.0, 256.0), mapState.internalCameraState.tilePoint)
    }

    @Test
    fun initialCoordinatesDefineTilePoint() {
        val mapState = MapState(
            mapProperties = mapProperties(),
            initialCameraState = CameraState(
                coordinates = Coordinates(-90.0, -45.0),
            ),
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
            density = Density(2F),
        )

        assertEquals(TilePoint(128.0, 384.0), mapState.internalCameraState.tilePoint)
        assertEquals(Coordinates(-90.0, -45.0), mapState.cameraState.coordinates)
    }

    @Test
    fun saverRestoresTilePointWithoutDensity() {
        val mapProperties = mapProperties()
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        val original = mapState(
            mapProperties = mapProperties,
            coroutineScope = coroutineScope,
            density = Density(1F),
        )
        original.setPosition(TilePoint(128.0, 384.0))
        val saver = MapState.saver(mapProperties, coroutineScope)
        val saved = assertNotNull(with(saver) {
            SaverScope { true }.save(original)
        })

        val restored = assertNotNull(saver.restore(saved))

        assertEquals(original.cameraState.coordinates, restored.cameraState.coordinates)
        assertEquals(TilePoint(128.0, 384.0), restored.internalCameraState.tilePoint)
    }

    private fun mapState(
        mapProperties: MapProperties = mapProperties(),
        coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
        density: Density,
    ) = MapState(
        mapProperties = mapProperties,
        coroutineScope = coroutineScope,
        density = density,
    )

    private fun mapProperties() = object : MapProperties {
        override val boundMap = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND)
        override val outsideTiles = OutsideTilesType.NONE
        override val zoomLevels = object : ZoomLevelRange {
            override val min = 0
            override val max = 31
        }
        override val coordinatesRange = object : CoordinatesRange {
            override val latitude = Latitude(north = 90.0, south = -90.0)
            override val longitude = Longitude(west = -180.0, east = 180.0)
        }
        override val tileSize = TileDimension(512.dp, 512.dp)

        override fun toProjectedCoordinates(coordinates: Coordinates) =
            ProjectedCoordinates(coordinates.x, coordinates.y)

        override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) =
            Coordinates(projectedCoordinates.x, projectedCoordinates.y)
    }
}
