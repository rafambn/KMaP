package com.rafambn.kmap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun FpsMonitor(modifier: Modifier = Modifier) {
    var frameCount by remember { mutableStateOf(0) }
    var lastFrameTime by remember { mutableStateOf(Clock.System.now()) }
    var fps by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                frameCount++
                val currentTime = Clock.System.now()
                val elapsedTime = currentTime - lastFrameTime
                if (elapsedTime.inWholeSeconds >= 1) {
                    fps = frameCount
                    frameCount = 0
                    lastFrameTime = currentTime
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Text(
            text = "FPS: $fps",
            color = Color.Red,
            fontSize = 16.sp
        )
    }
}
