package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import com.rafambn.kmap.components.parameters.CanvasParameters

data class Canvas(
    val parameters: CanvasParameters,
    val content: @Composable () -> Unit
) : Component
