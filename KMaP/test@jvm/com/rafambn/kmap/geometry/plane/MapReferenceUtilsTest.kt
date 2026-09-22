package com.rafambn.kmap.geometry.plane

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.MapState
import com.rafambn.kmap.geometry.angle.Degrees
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
import kotlin.test.assertFailsWith

class MapReferenceUtilsTest {
    @Test
    fun screenAndTileConversionsAreInverseWithZoomAndRotation() {
        val mapState = mapState(
            cameraPoint = TilePoint(170.0, 210.0),
            viewportSize = ScreenOffset(800.0, 600.0),
            zoom = 4.25F,
            angle = Degrees(37.0),
        )
        val tilePoint = TilePoint(182.5, 194.25)

        val converted = context(mapState) {
            tilePoint.toScreenOffset().toTilePoint()
        }

        assertEquals(tilePoint.x, converted.x, 0.0000000001)
        assertEquals(tilePoint.y, converted.y, 0.0000000001)
    }

    @Test
    fun markerUsesTheNearestCopyWhenOutsideTilesRepeat() {
        val mapState = mapState(
            cameraPoint = TilePoint(511.0, 511.0),
            viewportSize = ScreenOffset(100.0, 100.0),
            outsideTiles = OutsideTilesType.LOOP,
        )

        val offset = context(mapState) {
            TilePoint(1.0, 1.0).toNearestScreenOffset()
        }

        assertEquals(ScreenOffset(52.0, 52.0), offset)
    }

    @Test
    fun markerStaysOnOriginalTileWhenOutsideTilesAreDisabled() {
        val mapState = mapState(
            cameraPoint = TilePoint(511.0, 511.0),
            viewportSize = ScreenOffset(1024.0, 1024.0),
            boundMap = BoundMapBorder(MapBorderType.LOOP, MapBorderType.LOOP),
        )

        val offset = context(mapState) {
            TilePoint(1.0, 1.0).toNearestScreenOffset()
        }

        assertEquals(ScreenOffset(2.0, 2.0), offset)
    }

    @Test
    fun widePathStillPassesThroughCameraPosition() {
        val mapState = mapState(
            cameraPoint = TilePoint(400.0, 400.0),
            viewportSize = ScreenOffset(800.0, 800.0),
            boundMap = BoundMapBorder(MapBorderType.LOOP, MapBorderType.LOOP),
            outsideTiles = OutsideTilesType.LOOP,
        )

        val pathOrigin = context(mapState) {
            TilePoint(100.0, 100.0).toScreenOffset()
        }
        val pathEnd = pathOrigin + ScreenOffset(300.0, 300.0)

        assertEquals(ScreenOffset(400.0, 400.0), pathEnd)
    }

    @Test
    fun screenConversionPreservesMapCopyWithLoopZoomRotationAndDensity() {
        val mapState = mapState(
            cameraPoint = TilePoint(1000.0, 1000.0),
            viewportSize = ScreenOffset(800.0, 600.0),
            zoom = 2.5F,
            angle = Degrees(37.0),
            boundMap = BoundMapBorder(MapBorderType.LOOP, MapBorderType.LOOP),
            outsideTiles = OutsideTilesType.LOOP,
            density = Density(2F),
        )
        val tilePoint = TilePoint(1.0, 1.0)

        val converted = context(mapState) {
            tilePoint.toScreenOffset().toTilePoint()
        }

        assertEquals(tilePoint.x, converted.x, 0.0000000001)
        assertEquals(tilePoint.y, converted.y, 0.0000000001)
    }

    @Test
    fun canvasReferenceSupportsZoomAboveIntShiftRange() {
        val mapState = mapState(
            cameraPoint = TilePoint(1.0, 2.0),
            zoom = 31F,
        )

        val reference = context(mapState) {
            mapState.cameraState.tilePoint.toCanvasDrawReference()
        }

        assertEquals(CanvasDrawReference(-2_147_483_648.0, -4_294_967_296.0), reference)
    }

    @Test
    fun transformReferenceRejectsZeroSourceSpan() {
        assertFailsWith<IllegalArgumentException> {
            transformReference(
                pointX = 1.0,
                pointY = 1.0,
                sourceRangeX = Pair(1.0, 1.0),
                sourceRangeY = Pair(0.0, 2.0),
                targetRangeX = Pair(0.0, 10.0),
                targetRangeY = Pair(0.0, 10.0),
            )
        }
    }

    @Test
    fun coordinatesRejectZeroTileSizeWhenRead() {
        assertFailsWith<IllegalArgumentException> {
            mapState(tileSize = TileDimension(512.dp, 0.dp)).coordinates
        }
    }

    @Test
    fun screenConversionUsesTheLatestViewportSize() {
        val cameraPoint = TilePoint(256.0, 256.0)
        val mapState = mapState(
            cameraPoint = cameraPoint,
            viewportSize = ScreenOffset(100.0, 80.0),
        )

        val initialCenter = context(mapState) { cameraPoint.toScreenOffset() }
        mapState.setViewportSize(IntSize(300, 200))
        val resizedCenter = context(mapState) { cameraPoint.toScreenOffset() }

        assertEquals(ScreenOffset(50.0, 40.0), initialCenter)
        assertEquals(ScreenOffset(150.0, 100.0), resizedCenter)
    }

    private fun mapState(
        cameraPoint: TilePoint = TilePoint(256.0, 256.0),
        viewportSize: ScreenOffset = ScreenOffset.Zero,
        zoom: Float = 0F,
        angle: Degrees = Degrees.Zero,
        boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
        outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
        tileSize: TileDimension = TileDimension(512.dp, 512.dp),
        density: Density = Density(1F),
    ): MapState {
        val mapProperties = object : MapProperties {
            override val boundMap = boundMap
            override val outsideTiles = outsideTiles
            override val zoomLevels = object : ZoomLevelRange {
                override val min = 0
                override val max = 31
            }
            override val coordinatesRange = object : CoordinatesRange {
                override val latitude = Latitude(north = 90.0, south = -90.0)
                override val longitude = Longitude(west = -180.0, east = 180.0)
            }
            override val tileSize = tileSize

            override fun toProjectedCoordinates(coordinates: Coordinates) =
                ProjectedCoordinates(coordinates.x, coordinates.y)

            override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) =
                Coordinates(projectedCoordinates.x, projectedCoordinates.y)
        }

        val mapState = MapState(
            mapProperties = mapProperties,
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
            density = density,
        )
        mapState.updateCamera(tilePoint = cameraPoint, zoom = zoom, angle = angle)
        mapState.setViewportSize(IntSize(viewportSize.x.toInt(), viewportSize.y.toInt()))
        return mapState
    }
}
