package io.github.rafambn.kmap.tiles

import androidx.compose.ui.graphics.ImageBitmap

data class Tile(
    val zoom: Int,
    val row: Int,
    val col: Int,
    val imageBitmap: ImageBitmap
)