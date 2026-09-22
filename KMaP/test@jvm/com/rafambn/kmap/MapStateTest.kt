package com.rafambn.kmap

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.animation.core.tween
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.camera.CameraState
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.plane.DifferentialScreenOffset
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import com.rafambn.kmap.source.RasterTile
import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.TileSpecs
import com.rafambn.kmap.source.internal.CanvasEngine
import com.rafambn.kmap.source.internal.TileRenderer
import com.rafambn.kmap.components.parameters.RasterCanvasParameters
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MapStateTest {
    @Test
    fun animatedPanZoomAndRotationDoNotRepeatUnchangedTileRequests() {
        val scope = CoroutineScope(Job().apply { cancel() })
        val renderer = TileRenderer<RasterTile, RasterTile>(scope, { _, _, _ -> error("Consumer is paused") }, { it })
        val engine = object : CanvasEngine<RasterTile>(coroutineScope = scope, tileRenderer = renderer) {}
        val mapState = mapState(coroutineScope = scope)
        mapState.updateCamera(zoom = 2F)
        mapState.canvasKernel.canvas[1] = engine
        mapState.setViewportSize(IntSize(64, 64))
        val initialTiles = renderer.tilesToProcessChannel.tryReceive().getOrThrow()
        assertEquals(4, initialTiles.size)
        var frames = 0
        val clock = object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
                assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)
                return onFrame(++frames * 16_000_000L)
            }
        }

        runBlocking(clock) {
            mapState.motionController.animate {
                positionTo(TilePoint(258.0, 258.0), tween(160))
                zoomTo(2.3F, tween(160))
                rotateTo(Degrees(45.0), tween(160))
            }
        }

        assertTrue(frames > 20)
        assertEquals(initialTiles, engine.currentVisibleTiles)
        assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)
    }

    @Test
    fun animatedPanKeepsOnlyLatestPendingTileRequest() {
        val scope = CoroutineScope(Job().apply { cancel() })
        val renderer = TileRenderer<RasterTile, RasterTile>(scope, { _, _, _ -> error("Consumer is paused") }, { it })
        val engine = object : CanvasEngine<RasterTile>(coroutineScope = scope, tileRenderer = renderer) {}
        val mapState = mapState(coroutineScope = scope)
        mapState.updateCamera(zoom = 3F)
        mapState.canvasKernel.canvas[1] = engine
        mapState.setViewportSize(IntSize(64, 64))
        val initialTiles = engine.currentVisibleTiles
        var frames = 0
        val clock = object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
                onFrame(++frames * 16_000_000L)
        }

        runBlocking(clock) {
            mapState.motionController.animate {
                positionTo(TilePoint(400.0, 400.0), tween(320))
                zoomTo(4F, tween(160))
                rotateTo(Degrees(45.0), tween(160))
            }
        }

        assertTrue(initialTiles != engine.currentVisibleTiles)
        assertEquals(engine.currentVisibleTiles, renderer.tilesToProcessChannel.tryReceive().getOrThrow())
        assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)
    }

    @Test
    fun emptyRequestReplacesObsoleteWorkAndZoomChangesArePublished() {
        val scope = CoroutineScope(Job().apply { cancel() })
        val renderer = TileRenderer<RasterTile, RasterTile>(scope, { _, _, _ -> error("Consumer is paused") }, { it })
        val engine = object : CanvasEngine<RasterTile>(coroutineScope = scope, tileRenderer = renderer) {}
        engine.renderTiles(listOf(TileSpecs(2, 1, 1)), 2)
        engine.renderTiles(emptyList(), 2)
        assertEquals(emptyList(), renderer.tilesToProcessChannel.tryReceive().getOrThrow())

        engine.renderTiles(emptyList(), 3)
        assertEquals(3, engine.activeTiles.value.currentZoom)
        assertEquals(emptyList(), renderer.tilesToProcessChannel.tryReceive().getOrThrow())
        engine.renderTiles(emptyList(), 3)
        assertTrue(renderer.tilesToProcessChannel.tryReceive().isFailure)
    }

    @Test
    fun newCanvasReceivesCurrentTilesEvenWhenViewportHasNotChanged() {
        val scope = CoroutineScope(Job().apply { cancel() })
        val mapState = mapState(coroutineScope = scope)
        mapState.setViewportSize(IntSize(64, 64))
        val first = RasterCanvasParameters(id = 1, tileSource = { z, r, c -> TileResult.Failure(TileSpecs(z, r, c)) })
        val second = RasterCanvasParameters(id = 2, tileSource = first.tileSource)
        mapState.canvasKernel.refreshCanvas(listOf(first))
        val initialTiles = mapState.canvasKernel.canvas.getValue(1).currentVisibleTiles
        assertTrue(initialTiles.isNotEmpty())

        mapState.canvasKernel.refreshCanvas(listOf(first, second))

        assertEquals(initialTiles, mapState.canvasKernel.canvas.getValue(2).currentVisibleTiles)
    }

    @Test
    fun initialZoomUsesPreferenceMinimum() {
        val mapState = mapState(
            zoomLevelPreference = zoomRange(3, 8),
        )

        assertEquals(3F, mapState.cameraState.zoom)
    }

    @Test
    fun constructorRejectsInvertedZoomPreference() {
        assertFailsWith<IllegalArgumentException> {
            mapState(zoomLevelPreference = zoomRange(8, 3))
        }
    }

    @Test
    fun constructorRejectsZoomPreferenceOutsideMapRange() {
        assertFailsWith<IllegalArgumentException> {
            mapState(zoomLevelPreference = zoomRange(-1, 8))
        }
        assertFailsWith<IllegalArgumentException> {
            mapState(zoomLevelPreference = zoomRange(3, 32))
        }
    }

    @Test
    fun constructorRejectsInitialZoomOutsidePreference() {
        assertFailsWith<IllegalArgumentException> {
            mapState(
                zoomLevelPreference = zoomRange(3, 8),
                initialCameraState = cameraState(zoom = 2F),
            )
        }
    }

    @Test
    fun changingZoomPreferenceCoercesCurrentZoom() {
        val zoomAboveMaximum = mapState(initialCameraState = cameraState(zoom = 8F))
        val zoomBelowMinimum = mapState(initialCameraState = cameraState(zoom = 2F))

        zoomAboveMaximum.zoomLevelPreference = zoomRange(2, 5)
        zoomBelowMinimum.zoomLevelPreference = zoomRange(3, 9)

        assertEquals(5F, zoomAboveMaximum.cameraState.zoom)
        assertEquals(3F, zoomBelowMinimum.cameraState.zoom)
    }

    @Test
    fun zoomRejectsNaN() {
        val mapState = mapState()
        assertFailsWith<IllegalArgumentException> {
            mapState.updateCamera(zoom = Float.NaN)
        }
    }

    @Test
    fun centeredZoomPublishesOneCameraState() {
        val mapState = mapState()
        mapState.setViewportSize(IntSize(800, 600))
        val centerPoint = TilePoint(270.0, 270.0)
        val initialOffset = context(mapState) { centerPoint.toScreenOffset() }
        var stateWrites = 0

        Snapshot.observe(
            readObserver = null,
            writeObserver = { stateWrites++ },
        ) {
            mapState.motionController.zoomToCentered(2F, centerPoint)
        }

        val finalOffset = context(mapState) { centerPoint.toScreenOffset() }
        assertEquals(1, stateWrites)
        assertEquals(initialOffset.x, finalOffset.x, absoluteTolerance = 0.000001)
        assertEquals(initialOffset.y, finalOffset.y, absoluteTolerance = 0.000001)
    }

    @Test
    fun centeredRotationPublishesOneCameraState() {
        val mapState = mapState()
        mapState.setViewportSize(IntSize(800, 600))
        val centerPoint = TilePoint(270.0, 270.0)
        val initialOffset = context(mapState) { centerPoint.toScreenOffset() }
        var stateWrites = 0

        Snapshot.observe(
            readObserver = null,
            writeObserver = { stateWrites++ },
        ) {
            mapState.motionController.rotateToCentered(Degrees(90.0), centerPoint)
        }

        val finalOffset = context(mapState) { centerPoint.toScreenOffset() }
        assertEquals(1, stateWrites)
        assertEquals(initialOffset.x, finalOffset.x, absoluteTolerance = 0.000001)
        assertEquals(initialOffset.y, finalOffset.y, absoluteTolerance = 0.000001)
    }

    @Test
    fun equalCameraUpdateDoesNotPublishState() {
        val mapState = mapState()
        var stateWrites = 0

        Snapshot.observe(
            readObserver = null,
            writeObserver = { stateWrites++ },
        ) {
            mapState.updateCamera()
        }

        assertEquals(0, stateWrites)
    }

    @Test
    fun combinedCameraUpdatePlacesFocalPointWithDensityInOneWrite() {
        val mapState = mapState(density = Density(2F))
        mapState.setViewportSize(IntSize(800, 600))
        val focalPoint = TilePoint(270.0, 280.0)
        var stateWrites = 0

        Snapshot.observe(readObserver = null, writeObserver = { stateWrites++ }) {
            mapState.updateCamera(
                zoom = 2F,
                angle = Degrees(37.0),
                tilePoint = focalPoint,
                centerOffset = DifferentialScreenOffset(80.0, -40.0),
            )
        }

        val offset = context(mapState) { focalPoint.toScreenOffset() }
        assertEquals(1, stateWrites)
        assertEquals(2F, mapState.cameraState.zoom)
        assertEquals(Degrees(37.0), mapState.cameraState.angleDegrees)
        assertEquals(480.0, offset.x, absoluteTolerance = 0.000001)
        assertEquals(260.0, offset.y, absoluteTolerance = 0.000001)
    }

    @Test
    fun defaultCenterOffsetPlacesTilePointAtViewportCenter() {
        val mapState = mapState(density = Density(2F))
        mapState.setViewportSize(IntSize(800, 600))
        val point = TilePoint(128.0, 384.0)

        mapState.updateCamera(zoom = 3F, angle = Degrees(45.0), tilePoint = point)

        assertEquals(point, mapState.cameraState.tilePoint)
        assertEquals(ScreenOffset(400.0, 300.0), context(mapState) { point.toScreenOffset() })
    }

    @Test
    fun focalPlacementUsesClampedZoomAndRespectsMapBorders() {
        val mapState = mapState(zoomLevelPreference = zoomRange(0, 2))
        mapState.setViewportSize(IntSize(800, 600))
        val point = TilePoint(270.0, 280.0)

        mapState.updateCamera(
            zoom = 10F,
            tilePoint = point,
            centerOffset = DifferentialScreenOffset(80.0, -40.0),
        )

        assertEquals(2F, mapState.cameraState.zoom)
        assertEquals(ScreenOffset(480.0, 260.0), context(mapState) { point.toScreenOffset() })

        mapState.updateCamera(tilePoint = TilePoint(-10.0, 600.0))

        assertEquals(TilePoint(0.0, 512.0), mapState.cameraState.tilePoint)
    }

    @Test
    fun coordinatesAreProjectedOnlyOnAccess() {
        var projections = 0
        val properties = mapProperties()
        val mapState = mapState(mapProperties = object : MapProperties by properties {
            override fun toCoordinates(projectedCoordinates: ProjectedCoordinates): Coordinates {
                projections++
                return properties.toCoordinates(projectedCoordinates)
            }
        })
        mapState.setViewportSize(IntSize(800, 600))

        mapState.updateCamera(zoom = 2F, tilePoint = TilePoint(128.0, 384.0))
        assertEquals(TilePoint(128.0, 384.0), mapState.cameraState.tilePoint)
        assertEquals(0, projections)

        assertEquals(Coordinates(-90.0, -45.0), mapState.coordinates)
        assertEquals(1, projections)
    }

    @Test
    fun densityChangePreservesCoordinatesAndTilePoint() {
        val mapState = mapState(density = Density(1F))
        val coordinates = mapState.coordinates

        mapState.updateDensity(Density(2F, 1.5F))

        assertEquals(2F, mapState.density)
        assertEquals(1.5F, mapState.fontScale)
        assertEquals(coordinates, mapState.coordinates)
        assertEquals(TilePoint(256.0, 256.0), mapState.cameraState.tilePoint)
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
        assertEquals(TilePoint(256.0, 256.0), mapState.cameraState.tilePoint)
    }

    @Test
    fun initialTilePointDefinesCoordinates() {
        val mapState = MapState(
            mapProperties = mapProperties(),
            initialCameraState = CameraState(
                tilePoint = TilePoint(128.0, 384.0),
            ),
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
            density = Density(2F),
        )

        assertEquals(TilePoint(128.0, 384.0), mapState.cameraState.tilePoint)
        assertEquals(Coordinates(-90.0, -45.0), mapState.coordinates)
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
        original.updateCamera(tilePoint = TilePoint(128.0, 384.0))
        val saver = MapState.saver(
            mapProperties = mapProperties,
            zoomLevelPreference = mapProperties.zoomLevels,
            density = Density(1F),
            coroutineScope = coroutineScope,
        )
        val saved = assertNotNull(with(saver) {
            SaverScope { true }.save(original)
        })

        val restored = assertNotNull(saver.restore(saved))

        assertEquals(original.coordinates, restored.coordinates)
        assertEquals(TilePoint(128.0, 384.0), restored.cameraState.tilePoint)
    }

    @Test
    fun saverRestoresWithCurrentConfigurationAndCoercesOldZoom() {
        val mapProperties = mapProperties()
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        val density = Density(2F, 1.5F)
        val currentZoomLevelPreference = zoomRange(3, 8)
        val original = mapState(mapProperties = mapProperties)
        original.updateCamera(zoom = 20F)
        val saver = MapState.saver(
            mapProperties = mapProperties,
            zoomLevelPreference = currentZoomLevelPreference,
            density = density,
            coroutineScope = coroutineScope,
        )
        val saved = assertNotNull(with(saver) {
            SaverScope { true }.save(original)
        })

        assertFalse("zoomLevelPreference" in (saved as List<*>))

        val restored = assertNotNull(saver.restore(saved))

        assertSame(mapProperties, restored.mapProperties)
        assertSame(coroutineScope, restored.canvasKernel.coroutineScope)
        assertSame(currentZoomLevelPreference, restored.zoomLevelPreference)
        assertEquals(2F, restored.density)
        assertEquals(1.5F, restored.fontScale)
        assertEquals(8F, restored.cameraState.zoom)
    }

    @Test
    fun saverMigratesDensityScaledTilePoint() {
        val mapProperties = mapProperties()
        val saver = MapState.saver(
            mapProperties = mapProperties,
            zoomLevelPreference = mapProperties.zoomLevels,
            density = Density(1F),
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
        )
        val legacyState = listOf(
            "zoomLevelPreference", Pair(0, 31),
            "density", 2F,
            "fontScale", 1F,
            "zoom", 0F,
            "angleDegrees", 0.0,
            "coordinates", Pair(0.0, 0.0),
            "tilePoint", Pair(512.0, 512.0),
        )

        val restored = assertNotNull(saver.restore(legacyState))

        assertEquals(TilePoint(256.0, 256.0), restored.cameraState.tilePoint)
    }

    private fun mapState(
        mapProperties: MapProperties = mapProperties(),
        coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
        density: Density = Density(1F),
        zoomLevelPreference: ZoomLevelRange? = null,
        initialCameraState: CameraState? = null,
    ) = MapState(
        mapProperties = mapProperties,
        coroutineScope = coroutineScope,
        density = density,
        zoomLevelPreference = zoomLevelPreference,
        initialCameraState = initialCameraState,
    )

    private fun cameraState(
        zoom: Float = 0F,
        angle: Degrees = Degrees.Zero,
        tilePoint: TilePoint = TilePoint(256.0, 256.0),
    ) = CameraState(
        zoom = zoom,
        angleDegrees = angle,
        tilePoint = tilePoint,
    )

    private fun zoomRange(min: Int, max: Int) = object : ZoomLevelRange {
        override val min = min
        override val max = max
    }

    private fun mapProperties() = object : MapProperties {
        override val boundMap = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND)
        override val outsideTiles = OutsideTilesType.NONE
        override val zoomLevels = zoomRange(0, 31)
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
