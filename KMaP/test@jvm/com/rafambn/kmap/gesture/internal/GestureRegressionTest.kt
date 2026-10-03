@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package com.rafambn.kmap.gesture.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import com.rafambn.kmap.gesture.MapGestureWrapper
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.*

val GestureRegressionTest by testSuite {
    test("tap swipe rotation crosses the angle boundary by the shortest signed turn") {
        for (direction in listOf(-1f, 1f)) {
            val rotations = mutableListOf<Double>()
            withPointerReviewScene({
                Box(Modifier.size(300.dp).mapGestures(MapGestureWrapper(
                    onTapSwipe = { _, rotation -> rotations += rotation.value },
                )))
            }) { scene ->
                val x = 150f - direction
                scene.touch(PointerEventType.Press, 0, finger(0, x, 50f))
                scene.touch(PointerEventType.Release, 10, finger(0, x, 50f, false))
                scene.touch(PointerEventType.Press, 60, finger(1, x, 50f))
                scene.touch(PointerEventType.Move, 70, finger(1, x, 20f))
                scene.touch(PointerEventType.Move, 80, finger(1, 150f + direction, 20f))
                scene.touch(PointerEventType.Release, 90, finger(1, 150f + direction, 20f, false))
                assertEquals(1, rotations.size)
                assertTrue(rotations.single() * direction in 0.0..2.0)
            }
        }
    }

    test("path hover followed by click preserves the initiating pointer") {
        val failures = mutableListOf<Throwable>()
        var taps = 0
        val path = Path().apply { moveTo(0f, 0f); lineTo(200f, 0f) }
        withPointerReviewScene({
            Box(Modifier.size(300.dp).sharedPointerInput {
                detectPathGestures(onTap = { taps++ }, onHover = {},
                    convertScreenOffsetToProjectedCoordinates = { ProjectedCoordinates(it.x, it.y) },
                    path = path, threshold = 10f, checkForInsideClick = false)
            })
        }, failures) { scene ->
            scene.sendPointerEvent(PointerEventType.Move, Offset(50f, 10f), timeMillis = 0)
            scene.sendPointerEvent(PointerEventType.Press, Offset(50f, 10f), timeMillis = 10)
            scene.sendPointerEvent(PointerEventType.Release, Offset(50f, 10f), timeMillis = 20)
            assertTrue(failures.isEmpty())
            assertEquals(1, taps)
        }
    }

    test("scroll callback uses the updated wrapper after recomposition") {
        val calls = mutableListOf<String>()
        var wrapper by mutableStateOf(MapGestureWrapper(onScroll = { _, _ -> calls += "old" }))
        val recomposer = FrameRecomposer(Dispatchers.Unconfined)
        val scene = CanvasLayersComposeScene(recomposer, density = Density(1f), size = IntSize(300, 300))
        try {
            scene.setContent(recomposer.compositionContext) {
                Box(Modifier.size(300.dp).mapGestures(wrapper))
            }
            recomposer.performFrame(0L)
            scene.measureAndLayout()
            scene.sendPointerEvent(PointerEventType.Scroll, Offset(50f, 50f),
                scrollDelta = Offset(0f, 1f), timeMillis = 0)
            wrapper = MapGestureWrapper(onScroll = { _, _ -> calls += "new" })
            recomposer.performFrame(1L)
            scene.measureAndLayout()
            scene.sendPointerEvent(PointerEventType.Scroll, Offset(50f, 50f),
                scrollDelta = Offset(0f, 1f), timeMillis = 10)
            assertEquals(listOf("old", "new"), calls)
        } finally {
            scene.close()
            recomposer.close()
        }
    }

    test("a released map press ends even when only long press is configured") {
        val longPress = CompletableDeferred<Unit>()
        withPointerReviewScene({
            val original = LocalViewConfiguration.current
            val configuration = object : ViewConfiguration by original {
                override val longPressTimeoutMillis = 30L
            }
            CompositionLocalProvider(LocalViewConfiguration provides configuration) {
                Box(Modifier.size(300.dp).mapGestures(MapGestureWrapper(onLongPress = { longPress.complete(Unit) })))
            }
        }) { scene ->
            scene.touch(PointerEventType.Press, 0, finger(0, 50f, 50f))
            scene.touch(PointerEventType.Release, 10, finger(0, 50f, 50f, false))
            // The scene uses a real dispatcher, while TestBalloon advances its own test clock.
            withContext(Dispatchers.Default) { kotlinx.coroutines.delay(100) }
            assertFalse(longPress.isCompleted)
            scene.touch(PointerEventType.Press, 200, finger(1, 50f, 50f))
            withContext(Dispatchers.Default) { withTimeout(2_000) { longPress.await() } }
            scene.touch(PointerEventType.Release, 300, finger(1, 50f, 50f, false))
        }
    }

    test("path outline hits points between the old samples on a long line") {
        val path = Path().apply { moveTo(0f, 0f); lineTo(20000f, 0f) }
        val tester = PathTester(path, 1f, false)
        assertTrue(tester.checkHit(Offset(21f, 1f)))
        assertTrue(tester.checkHit(Offset(11f, 1f)))
    }

    test("path outline testing includes the second contour") {
        val path = Path().apply {
            moveTo(0f, 0f); lineTo(100f, 0f)
            moveTo(0f, 100f); lineTo(100f, 100f)
        }
        val tester = PathTester(path, 10f, false)
        assertTrue(tester.checkHit(Offset(60f, 10f)))
        assertTrue(tester.checkHit(Offset(60f, 110f)))
    }
}

private fun finger(id: Long, x: Float, y: Float, pressed: Boolean = true) =
    ComposeScenePointer(PointerId(id), Offset(x, y), pressed, PointerType.Touch)

private fun ComposeScene.touch(type: PointerEventType, time: Long, vararg pointers: ComposeScenePointer) {
    sendPointerEvent(type, pointers.toList(), timeMillis = time)
}

private inline fun withPointerReviewScene(
    noinline content: @Composable () -> Unit,
    failures: MutableList<Throwable> = mutableListOf(),
    block: (ComposeScene) -> Unit,
) {
    val recomposer = FrameRecomposer(Dispatchers.Unconfined + CoroutineExceptionHandler { _, error -> failures += error })
    val scene = CanvasLayersComposeScene(recomposer, density = Density(1f), size = IntSize(300, 300))
    try {
        scene.setContent(recomposer.compositionContext, content)
        recomposer.performFrame(0L)
        scene.measureAndLayout()
        block(scene)
        assertTrue(failures.isEmpty(), "Unexpected pointer coroutine failure: $failures")
    } finally {
        scene.close()
        recomposer.close()
    }
}
