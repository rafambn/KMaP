package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import com.rafambn.kmap.components.parameters.PathParameters

data class Path(
    val parameters: PathParameters,
    val content: @Composable () -> Unit
) : Component
