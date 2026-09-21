package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import com.rafambn.kmap.components.parameters.MarkerParameters

data class Marker(
    val parameters: MarkerParameters,
    val content: @Composable () -> Unit
) : Component
