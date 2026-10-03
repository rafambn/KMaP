package com.rafambn.kmap.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.plane.DifferentialScreenOffset
import com.rafambn.kmap.geometry.plane.ScreenOffset
import de.infix.testBalloon.framework.core.testSuite
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private fun pointer(
    id: Long,
    previous: Offset,
    position: Offset = previous,
    previousPressed: Boolean = true,
    pressed: Boolean = true,
) = PointerInputChange(
    id = PointerId(id),
    uptimeMillis = 16L,
    position = position,
    pressed = pressed,
    previousUptimeMillis = 0L,
    previousPosition = previous,
    previousPressed = previousPressed,
    isInitiallyConsumed = false,
)

private fun event(vararg pointers: PointerInputChange) = PointerEvent(pointers.toList())

private fun tapSwipeEvents(positions: List<Offset>): List<PointerEvent> {
    val start = positions.first()
    val beforeSlop = start - Offset(40f, 0f)
    return listOf(
        event(pointer(1, beforeSlop, previousPressed = false)),
        event(pointer(1, beforeSlop, pressed = false)),
        event(pointer(1, beforeSlop, previousPressed = false)),
        event(pointer(1, beforeSlop, start)),
    ) + positions.zipWithNext { previous, current -> event(pointer(1, previous, current)) }
}

val MapGestureTest by testSuite {
    test("pinch emits a scale ratio independent of pixel distance and density") {
        for (density in listOf(1f, 2f, 3f)) {
            val left = Offset(500f - 40f * density, 500f)
            val right = Offset(500f + 40f * density, 500f)
            val events = listOf(
                event(pointer(1, left, previousPressed = false)),
                event(pointer(1, left), pointer(2, right, previousPressed = false)),
                event(
                    pointer(1, left, Offset(500f - 80f * density, 500f)),
                    pointer(2, right, Offset(500f + 80f * density, 500f)),
                ),
            )
            val factors = mutableListOf<Float>()
            GestureTestScope(events, density).detect(MapGestureCallbacks(
                onTransform = { centroid, panDelta, zoomFactor, rotationDelta ->
                    assertEquals(ScreenOffset(500.0, 500.0), centroid)
                    assertEquals(DifferentialScreenOffset.Zero, panDelta)
                    assertEquals(Degrees.Zero, rotationDelta)
                    factors += zoomFactor
                },
            ))
            assertEquals(listOf(2f), factors)
            assertFalse(events.any { it.changes.any { change -> change.isConsumed } })
        }
    }

    test("transform reports the previous centroid before simultaneous pan zoom and rotation") {
        val left = Offset(450f, 500f)
        val right = Offset(550f, 500f)
        var calls = 0
        GestureTestScope(listOf(
            event(pointer(1, left, previousPressed = false)),
            event(pointer(1, left), pointer(2, right, previousPressed = false)),
            event(
                pointer(1, left, Offset(510f, 420f)),
                pointer(2, right, Offset(510f, 620f)),
            ),
        )).detect(MapGestureCallbacks(
            onTransform = { centroid, panDelta, zoomFactor, rotationDelta ->
                calls++
                assertEquals(ScreenOffset(500.0, 500.0), centroid)
                assertEquals(DifferentialScreenOffset(10.0, 20.0), panDelta)
                assertEquals(2f, zoomFactor)
                assertEquals(90.0, rotationDelta.value, 0.0001)
            },
        ))
        assertEquals(1, calls)
    }

    test("pinch can start with a stationary centroid when two finger tap is registered") {
        val factors = mutableListOf<Float>()
        var taps = 0
        GestureTestScope(listOf(
            event(pointer(1, Offset(400f, 500f), previousPressed = false)),
            event(pointer(1, Offset(400f, 500f)), pointer(2, Offset(600f, 500f), previousPressed = false)),
            event(
                pointer(1, Offset(400f, 500f), Offset(350f, 500f)),
                pointer(2, Offset(600f, 500f), Offset(650f, 500f)),
            ),
            event(
                pointer(1, Offset(350f, 500f), Offset(300f, 500f)),
                pointer(2, Offset(650f, 500f), Offset(700f, 500f)),
            ),
        )).detect(MapGestureCallbacks(
            onTransform = { _, _, factor, _ -> factors += factor },
            onTwoFingerTap = { taps++ },
        ))
        assertEquals(listOf(4f / 3f), factors)
        assertEquals(0, taps)
    }

    test("single pointer panning and zero pinch radius emit neutral zoom") {
        val center = Offset(500f, 500f)
        val factors = mutableListOf<Float>()
        GestureTestScope(listOf(
            event(pointer(1, center, previousPressed = false)),
            event(pointer(1, center, center + Offset(40f, 0f))),
            event(pointer(1, center + Offset(40f, 0f), center + Offset(50f, 0f))),
            event(pointer(1, center), pointer(2, center, previousPressed = false)),
            event(pointer(1, center, center - Offset(50f, 0f)), pointer(2, center, center + Offset(50f, 0f))),
            event(pointer(1, center - Offset(50f, 0f), center), pointer(2, center + Offset(50f, 0f), center)),
        )).detect(MapGestureCallbacks(
            onTransform = { _, _, factor, _ -> factors += factor },
        ))
        assertEquals(listOf(1f, 1f, 1f), factors)
    }

    test("rotation can start with a stationary centroid when two finger tap is registered") {
        val rotations = mutableListOf<Degrees>()
        GestureTestScope(listOf(
            event(pointer(1, Offset(400f, 500f), previousPressed = false)),
            event(pointer(1, Offset(400f, 500f)), pointer(2, Offset(600f, 500f), previousPressed = false)),
            event(
                pointer(1, Offset(400f, 500f), Offset(500f, 400f)),
                pointer(2, Offset(600f, 500f), Offset(500f, 600f)),
            ),
            event(
                pointer(1, Offset(500f, 400f), Offset(600f, 500f)),
                pointer(2, Offset(500f, 600f), Offset(400f, 500f)),
            ),
        )).detect(MapGestureCallbacks(
            onTransform = { _, _, factor, rotation ->
                assertEquals(1f, factor)
                rotations += rotation
            },
            onTwoFingerTap = { error("Rotation must not produce a tap") },
        ))
        assertEquals(listOf(Degrees(90.0)), rotations)
    }

    test("two finger tap still reports the last released pointer") {
        val positions = mutableListOf<ScreenOffset>()
        val left = Offset(400f, 500f)
        val right = Offset(600f, 500f)
        GestureTestScope(listOf(
            event(pointer(1, left, previousPressed = false)),
            event(pointer(1, left), pointer(2, right, previousPressed = false)),
            event(pointer(1, left, pressed = false), pointer(2, right)),
            event(pointer(2, right, pressed = false)),
        )).detect(MapGestureCallbacks(
            onTransform = { _, _, _, _ -> error("Tap must not produce a transform") },
            onTwoFingerTap = { positions += it },
        ))
        assertEquals(listOf(ScreenOffset(600.0, 500.0)), positions)
    }

    test("tap swipe uses reciprocal scale factors and stays finite at the center") {
        val center = Offset(500f, 500f)
        val factors = mutableListOf<Float>()
        GestureTestScope(tapSwipeEvents(listOf(
            center + Offset(100f, 0f),
            center + Offset(200f, 0f),
            center + Offset(100f, 0f),
            center,
            center + Offset(100f, 0f),
        ))).detect(MapGestureCallbacks(onTapSwipe = { factor, _ -> factors += factor }))
        assertEquals(listOf(2f, 0.5f, 1f, 1f), factors)
    }

    test("tap swipe rotation crosses the angle boundary without a full turn") {
        fun point(degrees: Double): Offset {
            val radians = Math.toRadians(degrees)
            return Offset(500f + (100 * sin(radians)).toFloat(), 500f + (100 * cos(radians)).toFloat())
        }
        val rotations = mutableListOf<Degrees>()
        GestureTestScope(tapSwipeEvents(listOf(point(-179.0), point(179.0))))
            .detect(MapGestureCallbacks(onTapSwipe = { _, rotation -> rotations += rotation }))
        assertEquals(1, rotations.size)
        assertEquals(2.0, rotations.single().value, 0.0001)
    }
}
