package io.github.rafambn.kmap

import androidx.compose.ui.geometry.Offset

interface CanvasSizeChangeListener {
    fun onCanvasSizeChanged(size: Offset)
}