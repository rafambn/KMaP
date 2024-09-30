package com.rafambn.kmapdemo.core

import androidx.compose.ui.geometry.Offset

interface CanvasSizeChangeListener {
    fun onCanvasSizeChanged(size: Offset)
}