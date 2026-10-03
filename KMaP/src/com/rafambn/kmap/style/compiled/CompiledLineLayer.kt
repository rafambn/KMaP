package com.rafambn.kmap.style.compiled

import androidx.compose.ui.graphics.Color

data class CompiledLineLayer(
    override val id: String,
    override val sourceLayer: String,
    override val minZoom: Double,
    override val maxZoom: Double,
    override val filter: CompiledFilter?,
    override val visibility: CompiledValue<Boolean>,
    val color: CompiledValue<Color>?,
    val width: CompiledValue<Double>?,
    val opacity: CompiledValue<Double>?,
    val dashArray: CompiledValue<List<Double>>?,
    val patternPresent: CompiledValue<Boolean>?,
    val cap: CompiledValue<String>?,
    val join: CompiledValue<String>?
) : CompiledStyleLayer {
    override val type: CompiledLayerType get() = CompiledLayerType.LINE
}
