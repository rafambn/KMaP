package com.rafambn.kmap.style.compiled

import androidx.compose.ui.text.font.FontFamily
import com.rafambn.kmap.style.SpriteImage

data class CompiledStyle(
    val layers: List<CompiledStyleLayer>,
    val sprites: Map<String, SpriteImage> = emptyMap(),
    val glyphs: Map<String, FontFamily> = emptyMap()
)
