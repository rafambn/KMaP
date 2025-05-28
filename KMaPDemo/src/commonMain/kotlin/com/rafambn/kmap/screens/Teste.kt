package com.rafambn.kmap.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.gestures.sharedPointerInput

@Composable
fun LayeredComposables() {
    Box {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color.Blue)
                .sharedPointerInput {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
                                if (event.type == PointerEventType.Move ||
                                    event.type == PointerEventType.Enter) {
                                    val position = change.position
                                    println("Mouse position: x=${position.x}, y=${position.y}")
                                }
                            }
                        }
                    }
                }
        )

        // Top Composable
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color.Red)
                .sharedPointerInput {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
                                if (event.type == PointerEventType.Move ||
                                    event.type == PointerEventType.Enter) {
                                    println("teste")
                                }
                            }
                        }
                    }
                }
        )
    }
}
