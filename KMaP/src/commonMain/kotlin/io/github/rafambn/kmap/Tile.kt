package io.github.rafambn.kmap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

internal data class Tile(
    val zoom: Int,
    val row: Int,
    val col: Int
) {
    var imageBitmap: ImageBitmap? = null
    var alpha: Float by mutableStateOf(0f)
}