package com.rafambn.kmap.style.compiled

import androidx.compose.ui.graphics.Color
import com.rafambn.kmap.style.SpriteImage

data class CompiledSymbolLayer(
    override val id: String,
    override val sourceLayer: String,
    override val minZoom: Double,
    override val maxZoom: Double,
    override val filter: CompiledFilter?,
    override val visibility: CompiledValue<Boolean>,
    val textField: CompiledValue<String>?,
    val textTransform: CompiledValue<String>?,
    val textSize: CompiledValue<Double>?,
    val textMaxWidth: CompiledValue<Double>?,
    val textLineHeight: CompiledValue<Double>?,
    val textJustify: CompiledValue<String>?,
    val textAnchor: CompiledValue<String>?,
    val textOffset: CompiledValue<List<Double>>?,
    val textRadialOffset: CompiledValue<Double>?,
    val textRotate: CompiledValue<Double>?,
    val textFont: CompiledValue<List<String>>?,
    val iconImage: CompiledValue<SpriteImage>?,
    val iconSize: CompiledValue<Double>?,
    val iconRotate: CompiledValue<Double>?,
    val iconOffset: CompiledValue<List<Double>>?,
    val iconAnchor: CompiledValue<String>?,
    val textColor: CompiledValue<Color>?,
    val textOpacity: CompiledValue<Double>?,
    val textHaloColor: CompiledValue<Color>?,
    val textHaloWidth: CompiledValue<Double>?,
    val textHaloBlur: CompiledValue<Double>?,
    val textTranslate: CompiledValue<List<Double>>?,
    val iconColor: CompiledValue<Color>?,
    val iconOpacity: CompiledValue<Double>?,
    val iconHaloColor: CompiledValue<Color>?,
    val iconHaloWidth: CompiledValue<Double>?,
    val iconHaloBlur: CompiledValue<Double>?
) : CompiledStyleLayer {
    override val type: CompiledLayerType get() = CompiledLayerType.SYMBOL
}
