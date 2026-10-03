@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package com.rafambn.kmap.gesture.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.Dispatchers
import kotlin.test.assertEquals

val GestureCancellationTest by testSuite {
    for (detector in listOf("map", "path")) {
        for (secondTap in listOf(false, true)) {
            test("cancelling a $detector ${if (secondTap) "second" else "first"} press does not complete a tap") {
                val calls = mutableListOf<String>()
                val path = Path().apply { moveTo(0f, 0f); lineTo(100f, 0f) }
                val recomposer = FrameRecomposer(Dispatchers.Unconfined)
                val scene = CanvasLayersComposeScene(recomposer, density = Density(1f), size = IntSize(120, 100))

                try {
                    scene.setContent(recomposer.compositionContext) {
                        Box(Modifier.size(120.dp, 100.dp).sharedPointerInput {
                            if (detector == "map") {
                                detectMapGestures(
                                    onTap = { calls += "tap" },
                                    onDoubleTap = if (secondTap) ({ calls += "double tap" }) else null,
                                    onLongPress = { calls += "long press" },
                                )
                            } else {
                                detectPathGestures(
                                    onTap = { calls += "tap" },
                                    onDoubleTap = if (secondTap) ({ calls += "double tap" }) else null,
                                    onLongPress = { calls += "long press" },
                                    convertScreenOffsetToProjectedCoordinates = { ProjectedCoordinates(it.x, it.y) },
                                    path = path,
                                    checkForInsideClick = false,
                                )
                            }
                        })
                    }
                    recomposer.performFrame(0L)
                    scene.measureAndLayout()
                    scene.sendTouch(true, 0L)
                    if (secondTap) {
                        scene.sendTouch(false, 10L)
                        scene.sendTouch(true, 60L)
                    }

                    scene.cancelPointerInput()
                    assertEquals(emptyList(), calls)

                    scene.sendTouch(true, 100L)
                    scene.sendTouch(false, 110L)
                    if (secondTap) {
                        scene.sendTouch(true, 160L)
                        scene.sendTouch(false, 170L)
                    }
                    assertEquals(listOf(if (secondTap) "double tap" else "tap"), calls)
                } finally {
                    scene.close()
                    recomposer.close()
                }
            }
        }
    }
}

private fun ComposeScene.sendTouch(pressed: Boolean, timeMillis: Long) {
    sendPointerEvent(
        if (pressed) PointerEventType.Press else PointerEventType.Release,
        listOf(ComposeScenePointer(PointerId(0), Offset(50f, 10f), pressed, PointerType.Touch)),
        timeMillis = timeMillis,
    )
}
