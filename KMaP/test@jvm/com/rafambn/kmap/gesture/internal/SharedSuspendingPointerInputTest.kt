@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package com.rafambn.kmap.gesture.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.Dispatchers
import kotlin.test.assertEquals

val SharedSuspendingPointerInputTest by testSuite {
    test("recompositions replace the handler without retaining previous delegates") {
        var version by mutableStateOf(0)
        val started = mutableListOf<Pair<Int, Modifier.Node>>()
        val cancelled = mutableListOf<Int>()
        val recomposer = FrameRecomposer(Dispatchers.Unconfined)
        val scene = CanvasLayersComposeScene(recomposer, density = Density(1f), size = IntSize(100, 100))

        try {
            scene.setContent(recomposer.compositionContext) {
                val capturedVersion = version
                Box(Modifier.size(100.dp).sharedPointerInput {
                    started += capturedVersion to (this as Modifier.Node)
                    try {
                        awaitPointerEventScope { while (true) awaitPointerEvent() }
                    } finally {
                        cancelled += capturedVersion
                    }
                })
            }

            for (index in 0..3) {
                version = index
                recomposer.performFrame(index.toLong())
                scene.measureAndLayout()
                for (pressed in listOf(true, false)) {
                    scene.sendPointerEvent(
                        if (pressed) PointerEventType.Press else PointerEventType.Release,
                        listOf(ComposeScenePointer(PointerId(0), Offset(50f, 50f), pressed, PointerType.Touch)),
                        timeMillis = index * 20L + if (pressed) 0L else 10L,
                    )
                }
            }

            assertEquals(listOf(0, 1, 2, 3), started.map { it.first })
            assertEquals(listOf(0, 1, 2), cancelled)
            assertEquals(1, started.map { it.second }.distinct().count { it.isAttached })
        } finally {
            scene.close()
            recomposer.close()
        }
        assertEquals(listOf(0, 1, 2, 3), cancelled)
    }
}
