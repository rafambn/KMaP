package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import com.rafambn.kmap.components.parameters.ClusterParameters

data class Cluster(
    val parameters: ClusterParameters,
    val content: @Composable () -> Unit
) : Component
