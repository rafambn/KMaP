package com.rafambn.kmap.style.compiled

import androidx.compose.ui.graphics.Color
import com.rafambn.kmap.style.SpriteImage

data class CompiledBackgroundLayer(
    override val id: String,
    override val minZoom: Double,
    override val maxZoom: Double,
    override val filter: CompiledFilter?,
    override val visibility: CompiledValue<Boolean>,
    val color: CompiledValue<Color>?,
    val opacity: CompiledValue<Double>?,
    val pattern: CompiledValue<SpriteImage>?
) : CompiledStyleLayer {
    override val type: CompiledLayerType get() = CompiledLayerType.BACKGROUND
    override val sourceLayer: String? get() = null
}
