package com.rafambn.kmap.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.gestures.hoverReporting

@Composable
fun LayeredComposables() {
    Box {
        // Bottom Composable
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color.Blue)
                .hoverReporting { offset ->
                    println("Mouse position: x=${offset.x}, y=${offset.y}")
                }
        )

        // Top Composable
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color.Red)
                .hoverReporting { offset ->
                    println("teste")
                }
        )
    }
}
