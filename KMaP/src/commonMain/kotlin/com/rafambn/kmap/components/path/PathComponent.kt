package com.rafambn.kmap.components.path

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.Component

data class PathComponent(
    val parameters: PathParameter,
    val gestureDetector: (suspend PointerInputScope.(path: Path) -> Unit)? = null,
    val pathContent: @Composable () -> Unit
) : Component
