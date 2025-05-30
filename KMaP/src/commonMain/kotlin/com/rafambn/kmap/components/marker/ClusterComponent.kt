package com.rafambn.kmap.components.marker

import androidx.compose.runtime.Composable
import com.rafambn.kmap.components.Component

data class ClusterComponent(
    val clusterParameters: ClusterParameters,
    val clusterContent: @Composable () -> Unit
): Component
