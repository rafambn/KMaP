package com.rafambn.kmap.style.compiled

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily

data class CompiledStyle(
    val layers: List<CompiledStyleLayer>,
    val sprites: Map<String, ImageBitmap> = emptyMap(),
    val glyphs: Map<String, FontFamily> = emptyMap()
)
