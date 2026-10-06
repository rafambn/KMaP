package com.rafambn.kmap.source.internal

import androidx.compose.ui.unit.dp
import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import com.rafambn.kmap.geometry.plane.TilePoint
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.BoundaryMode
import com.rafambn.kmap.mapProperties.border.MapBoundaryBehavior
import com.rafambn.kmap.mapProperties.border.TileRepeatMode
import com.rafambn.kmap.mapProperties.coordinates.CoordinatesRange
import com.rafambn.kmap.mapProperties.coordinates.Latitude
import com.rafambn.kmap.mapProperties.coordinates.Longitude
import com.rafambn.kmap.source.TileSpecs
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val VisibleTileSelectionTest by testSuite {
    for (repeatMode in TileRepeatMode.entries) {
        for (zoom in listOf(0, 1, 5, 30)) {
            test("exact tile bounds select only that tile at zoom $zoom with $repeatMode") {
                val count = (1L shl zoom).toDouble()
                assertEquals(
                    listOf(TileSpecs(zoom, 0, 0)),
                    visibleTiles(TilePoint.Zero, TilePoint(256.0 / count, 128.0 / count), zoom, repeatMode),
                )
            }
        }

        test("partial tiles on both sides of an edge remain visible with $repeatMode") {
            assertEquals(
                listOf(TileSpecs(1, 0, 0), TileSpecs(1, 1, 0), TileSpecs(1, 0, 1), TileSpecs(1, 1, 1)),
                visibleTiles(TilePoint(127.5, 63.5), TilePoint(128.5, 64.5), repeatMode = repeatMode),
            )
        }

        for ((name, bounds) in listOf(
            "zero width" to (TilePoint(10.0, 10.0) to TilePoint(10.0, 20.0)),
            "zero height" to (TilePoint(10.0, 10.0) to TilePoint(20.0, 10.0)),
            "point" to (TilePoint(10.0, 10.0) to TilePoint(10.0, 10.0)),
            "reversed width" to (TilePoint(20.0, 10.0) to TilePoint(10.0, 20.0)),
            "reversed height" to (TilePoint(10.0, 20.0) to TilePoint(20.0, 10.0)),
        )) {
            test("$name selects no tiles with $repeatMode") {
                assertTrue(visibleTiles(bounds.first, bounds.second, repeatMode = repeatMode).isEmpty())
            }
        }
    }

    test("negative repeated tile bounds exclude their right and bottom neighbors") {
        assertEquals(
            listOf(TileSpecs(1, -1, -1)),
            visibleTiles(TilePoint(-128.0, -64.0), TilePoint.Zero, repeatMode = TileRepeatMode.REPEAT),
        )
    }

    test("repeated copies retain their unwrapped indices") {
        assertEquals(
            listOf(TileSpecs(1, 2, 2)),
            visibleTiles(TilePoint(256.0, 128.0), TilePoint(384.0, 192.0), repeatMode = TileRepeatMode.REPEAT),
        )
    }

    test("crossing the repeated map origin selects only intersecting tiles") {
        assertEquals(
            listOf(TileSpecs(1, -1, -1), TileSpecs(1, 0, -1), TileSpecs(1, -1, 0), TileSpecs(1, 0, 0)),
            visibleTiles(TilePoint(-128.0, -64.0), TilePoint(128.0, 64.0), repeatMode = TileRepeatMode.REPEAT),
        )
    }

    test("a viewport touching the outside of the map selects no tiles without repetition") {
        for ((topLeft, bottomRight) in listOf(
            TilePoint(-128.0, 0.0) to TilePoint(0.0, 64.0),
            TilePoint(0.0, -64.0) to TilePoint(128.0, 0.0),
            TilePoint(256.0, 0.0) to TilePoint(384.0, 64.0),
            TilePoint(0.0, 128.0) to TilePoint(128.0, 192.0),
        )) {
            assertTrue(visibleTiles(topLeft, bottomRight).isEmpty())
        }
    }

    test("partially outside viewports clip to the map without repetition") {
        assertEquals(
            listOf(TileSpecs(1, 0, 0)),
            visibleTiles(TilePoint(-128.0, -64.0), TilePoint(128.0, 64.0)),
        )
    }

    for (index in listOf(Int.MIN_VALUE, Int.MAX_VALUE)) {
        test("exact repeated tile bounds preserve Int boundary $index at zoom 30") {
            val width = 256.0 / (1L shl 30)
            val height = 128.0 / (1L shl 30)
            assertEquals(
                listOf(TileSpecs(30, index, index)),
                visibleTiles(
                    TilePoint(index * width, index * height),
                    TilePoint((index.toLong() + 1) * width, (index.toLong() + 1) * height),
                    zoom = 30,
                    repeatMode = TileRepeatMode.REPEAT,
                ),
            )
        }
    }
}

private fun visibleTiles(
    topLeft: TilePoint,
    bottomRight: TilePoint,
    zoom: Int = 1,
    repeatMode: TileRepeatMode = TileRepeatMode.NONE,
): List<TileSpecs> {
    return getVisibleTilesForLevel(topLeft, bottomRight, zoom, repeatMode, selectionMapProperties().tileSize)
}

private fun selectionMapProperties(repeatMode: TileRepeatMode = TileRepeatMode.NONE) = object : MapProperties {
    override val boundaryBehavior = MapBoundaryBehavior(BoundaryMode.CLAMP, BoundaryMode.CLAMP)
    override val tileRepeatMode = repeatMode
    override val zoomLevels = ZoomLevelRange(0, 30)
    override val tileSize = TileDimension(width = 256.dp, height = 128.dp)
    override val coordinatesRange = object : CoordinatesRange {
        override val latitude = Latitude(north = 0.0, south = 128.0)
        override val longitude = Longitude(west = 0.0, east = 256.0)
    }

    override fun toProjectedCoordinates(coordinates: Coordinates) = ProjectedCoordinates(coordinates.x, coordinates.y)
    override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) = Coordinates(projectedCoordinates.x, projectedCoordinates.y)
}
