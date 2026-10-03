package com.rafambn.kmap.geometry.plane

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.MapState
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.ProjectedBounds
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import de.infix.testBalloon.framework.core.testSuite
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope

private fun mapState(
    cameraPoint: TilePoint = TilePoint(256.0, 256.0),
    viewportSize: ScreenOffset = ScreenOffset.Zero,
    zoom: Float = 0F,
    angle: Degrees = Degrees.Zero,
    boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    tileSize: TileDimension = TileDimension(512.dp, 512.dp),
    density: Density = Density(1F),
    projectedBounds: ProjectedBounds = ProjectedBounds(
        topLeft = ProjectedCoordinates(-180.0, 90.0),
        bottomRight = ProjectedCoordinates(180.0, -90.0),
    ),
): MapState {
    val mapProperties = object : MapProperties {
        override val boundMap = boundMap
        override val outsideTiles = outsideTiles
        override val zoomLevels = ZoomLevelRange(min = 0, max = 30)
        override val projectedBounds = projectedBounds
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

val MapReferenceUtilsTest by testSuite {
    test("projectedBoundsMapCornersAndInteriorPointsInEitherAxisDirection") {
        for ((left, right) in listOf(1000.0 to 3000.0, 3000.0 to 1000.0)) {
            for ((top, bottom) in listOf(400.0 to 800.0, 800.0 to 400.0)) {
                val bounds = ProjectedBounds(
                    topLeft = ProjectedCoordinates(left, top),
                    bottomRight = ProjectedCoordinates(right, bottom),
                )
                val mapState = mapState(
                    tileSize = TileDimension(height = 256.dp, width = 512.dp),
                    density = Density(2F),
                    zoom = 3.5F,
                    angle = Degrees(37.0),
                    projectedBounds = bounds,
                )
                val coordinates = Coordinates(left + (right - left) / 4, top + (bottom - top) * 3 / 4)

                context(mapState) {
                    assertEquals(TilePoint.Zero, bounds.topLeft.toTilePoint())
                    assertEquals(TilePoint(512.0, 256.0), bounds.bottomRight.toTilePoint())
                    assertEquals(TilePoint(128.0, 192.0), coordinates.toTilePoint())
                    assertEquals(coordinates, coordinates.toTilePoint().toCoordinates())
                }
            }
        }
    }

    test("nonlinearProjectionUsesProjectedBoundsAndAllowsCoordinatesOutsideTheMap") {
        val properties = object : MapProperties by mapState().mapProperties {
            override val projectedBounds = ProjectedBounds(
                topLeft = ProjectedCoordinates(1000.0, 400.0),
                bottomRight = ProjectedCoordinates(3000.0, 100.0),
            )

            override fun toProjectedCoordinates(coordinates: Coordinates) =
                ProjectedCoordinates(coordinates.x * 1000.0, coordinates.y * coordinates.y)

            override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) =
                Coordinates(projectedCoordinates.x / 1000.0, sqrt(projectedCoordinates.y))
        }
        val state = MapState(
            mapProperties = properties,
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
        )

        context(state) {
            assertEquals(TilePoint.Zero, Coordinates(1.0, 20.0).toTilePoint())
            assertEquals(TilePoint(512.0, 512.0), Coordinates(3.0, 10.0).toTilePoint())
            val coordinates = Coordinates(2.0, 15.0)
            val tilePoint = coordinates.toTilePoint()
            assertEquals(256.0, tilePoint.x, 1e-10)
            assertEquals(512.0 * 7 / 12, tilePoint.y, 1e-10)
            val restored = tilePoint.toCoordinates()
            assertEquals(coordinates.x, restored.x, 1e-10)
            assertEquals(coordinates.y, restored.y, 1e-10)
            assertEquals(TilePoint(768.0, -384.0), Coordinates(4.0, 25.0).toTilePoint())
            assertEquals(Coordinates(4.0, 25.0), TilePoint(768.0, -384.0).toCoordinates())
        }
    }

    test("screenAndTileConversionsAreInverseWithZoomAndRotation") {
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

    test("markerUsesTheNearestCopyWhenOutsideTilesRepeat") {
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

    test("markerStaysOnOriginalTileWhenOutsideTilesAreDisabled") {
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

    test("widePathStillPassesThroughCameraPosition") {
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

    test("screenConversionPreservesMapCopyWithLoopZoomRotationAndDensity") {
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

    test("canvasReferenceSupportsPositionsBeyondIntRangeAtMaximumZoom") {
        val mapState = mapState(
            cameraPoint = TilePoint(2.0, 4.0),
            zoom = 30F,
        )

        val reference = context(mapState) {
            mapState.cameraState.tilePoint.toCanvasDrawReference()
        }

        assertEquals(CanvasDrawReference(-2_147_483_648.0, -4_294_967_296.0), reference)
    }

    test("transformReferenceRejectsZeroSourceSpan") {
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

    test("coordinatesRejectZeroTileSizeWhenRead") {
        assertFailsWith<IllegalArgumentException> {
            mapState(tileSize = TileDimension(512.dp, 0.dp)).coordinates
        }
    }

    test("screenConversionUsesTheLatestViewportSize") {
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
}
