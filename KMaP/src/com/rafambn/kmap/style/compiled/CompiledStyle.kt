package com.rafambn.kmap.style.compiled

import androidx.compose.ui.text.font.FontFamily
import com.rafambn.kmap.style.SpriteImage
import com.rafambn.kmap.source.preparation.PreparationStyle

data class CompiledStyle(
    val layers: List<CompiledStyleLayer>,
    val sprites: Map<String, SpriteImage> = emptyMap(),
    val glyphs: Map<String, FontFamily> = emptyMap(),
    /** Null for manually constructed evaluator closures; those use the decoded-source compatibility path. */
    val preparation: PreparationStyle? = null,
)
