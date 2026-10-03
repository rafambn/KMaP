package com.rafambn.kmap.camera

import androidx.compose.animation.core.tween
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.MapState
import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
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
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun mapProperties(
    mercator: Boolean = false,
    horizontal: BoundaryMode = BoundaryMode.CLAMP,
    vertical: BoundaryMode = BoundaryMode.CLAMP,
) = object : MapProperties {
    override val boundaryBehavior = MapBoundaryBehavior(horizontal, vertical)
    override val tileRepeatMode = TileRepeatMode.NONE
    override val zoomLevels = ZoomLevelRange(0, 19)
    override val coordinatesRange = object : CoordinatesRange {
        // Mercator uses projected units distinct from latitude degrees.
        override val latitude = if (mercator) Latitude(north = PI, south = -PI)
        else Latitude(north = 90.0, south = -90.0)
        override val longitude = Longitude(west = -180.0, east = 180.0)
    }
    override val tileSize = TileDimension(512.dp, 512.dp)

    override fun toProjectedCoordinates(coordinates: Coordinates) = ProjectedCoordinates(
        coordinates.x,
        if (mercator) ln(tan(PI / 4 + PI * coordinates.y / 360)) else coordinates.y,
    )

    override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) = Coordinates(
        projectedCoordinates.x,
        if (mercator) (atan(exp(projectedCoordinates.y)) - PI / 4) * 360 / PI
        else projectedCoordinates.y,
    )
}

private suspend fun moved(
    start: Coordinates,
    offset: Coordinates,
    animated: Boolean,
    properties: MapProperties = mapProperties(),
): Coordinates {
    val state = MapState(properties, coroutineScope = CoroutineScope(EmptyCoroutineContext))
    state.motionController.positionTo(start)
    if (animated) {
        var frames = 0
        val clock = object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
                if (properties.boundaryBehavior.horizontal == BoundaryMode.WRAP) {
                    // The animation must cross the seam instead of travelling through the map center.
                    assertTrue(state.coordinates.x >= 170.0 || state.coordinates.x <= -170.0)
                }
                return onFrame(++frames * 16_000_000L)
            }
        }
        withContext(clock) { state.motionController.positionBy(offset, tween(64)) }
    } else {
        state.motionController.positionBy(offset)
    }
    return state.coordinates
}

val MotionControllerTest by testSuite {
    for (animated in listOf(false, true)) {
        test("zero coordinate offset preserves camera, animated=$animated") {
            val actual = moved(Coordinates(-90.0, -45.0), Coordinates.Zero, animated)
            assertEquals(-90.0, actual.x, 0.000001)
            assertEquals(-45.0, actual.y, 0.000001)
        }

        test("offset is added to current coordinates, animated=$animated") {
            val actual = moved(Coordinates(-90.0, -45.0), Coordinates(10.0, 5.0), animated)
            assertEquals(-80.0, actual.x, 0.000001)
            assertEquals(-40.0, actual.y, 0.000001)
        }

        test("Mercator offset is added before projection, animated=$animated") {
            val actual = moved(Coordinates(0.0, 60.0), Coordinates(10.0, 5.0), animated, mapProperties(mercator = true))
            assertEquals(10.0, actual.x, 0.000001)
            assertEquals(65.0, actual.y, 0.000001)
        }

        for (direction in listOf(-1.0, 1.0)) {
            test("Mercator movement clamps before projecting past a pole, direction=$direction, animated=$animated") {
                val actual = moved(
                    Coordinates(0.0, direction * 80.0),
                    Coordinates(0.0, direction * 15.0),
                    animated,
                    mapProperties(mercator = true),
                )
                assertEquals(direction * 85.0511287798066, actual.y, 0.000001)
            }
        }

        test("horizontal wrapping is preserved while latitude clamps, animated=$animated") {
            val actual = moved(
                Coordinates(170.0, 80.0),
                Coordinates(20.0, 15.0),
                animated,
                mapProperties(mercator = true, horizontal = BoundaryMode.WRAP),
            )
            assertEquals(-170.0, actual.x, 0.000001)
            assertEquals(85.0511287798066, actual.y, 0.000001)
        }

        test("vertical wrapping is preserved while longitude clamps, animated=$animated") {
            val actual = moved(
                Coordinates(170.0, 80.0),
                Coordinates(20.0, 15.0),
                animated,
                mapProperties(vertical = BoundaryMode.WRAP),
            )
            assertEquals(180.0, actual.x, 0.000001)
            assertEquals(-85.0, actual.y, 0.000001)
        }
    }
}
