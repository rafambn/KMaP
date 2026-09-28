package com.rafambn.kmap.style.compiled

import androidx.compose.ui.graphics.Color
import com.rafambn.kmap.style.SpriteImage

data class CompiledFillLayer(
    override val id: String,
    override val sourceLayer: String,
    override val minZoom: Double,
    override val maxZoom: Double,
    override val filter: CompiledFilter?,
    override val visibility: CompiledValue<Boolean>,
    val color: CompiledValue<Color>?,
    val opacity: CompiledValue<Double>?,
    val outlineColor: CompiledValue<Color>?,
    val antialias: CompiledValue<Boolean>?,
    val pattern: CompiledValue<SpriteImage>?
) : CompiledStyleLayer {
    override val type: CompiledLayerType get() = CompiledLayerType.FILL
}
