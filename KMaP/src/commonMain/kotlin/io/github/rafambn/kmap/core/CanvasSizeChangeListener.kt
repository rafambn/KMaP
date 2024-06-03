package io.github.rafambn.kmap.core

import androidx.compose.ui.geometry.Offset

interface CanvasSizeChangeListener {
    fun onCanvasSizeChanged(size: Offset)
}