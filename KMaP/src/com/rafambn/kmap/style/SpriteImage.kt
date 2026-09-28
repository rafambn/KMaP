package com.rafambn.kmap.style

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.TileMode

/** A sprite image and the pixel density recorded for it in the sprite index. */
data class SpriteImage(
    val bitmap: ImageBitmap,
    val pixelRatio: Double = 1.0,
    val sdf: Boolean = false
) {
    internal val repeatingShader by lazy { ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated) }

    init {
        require(pixelRatio.isFinite() && pixelRatio > 0.0)
    }
}
