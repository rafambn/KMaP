package com.rafambn.kmap.components.marker

import androidx.compose.runtime.Composable
import com.rafambn.kmap.components.Component

data class MarkerComponent(
    val markerParameters: MarkerParameters,
    val markerContent: @Composable () -> Unit
): Component
