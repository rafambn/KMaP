package com.rafambn.kmap.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

data class FormattedText(val sections: List<Section>) {
    sealed interface Section {
        data class Text(
            val value: String,
            val fontScale: Double? = null,
            val color: Color? = null,
            val fonts: List<String>? = null
        ) : Section

        data class Image(val bitmap: ImageBitmap) : Section
    }
}
