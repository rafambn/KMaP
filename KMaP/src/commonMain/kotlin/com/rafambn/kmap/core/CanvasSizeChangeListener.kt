package com.rafambn.kmap.core

import androidx.compose.ui.geometry.Offset

interface CanvasSizeChangeListener {
    fun onCanvasSizeChanged(size: Offset)
}