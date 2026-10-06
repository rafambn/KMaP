package com.rafambn.kmap

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App(initialRoute = if (openVectorDemo()) Routes.VectorTiles else Routes.Start, vectorZoom = vectorZoom().toFloat().coerceIn(0F, 14F))
    }
}

private fun openVectorDemo(): Boolean = js("new URLSearchParams(window.location.search).get('screen') === 'vector'")
private fun vectorZoom(): Double = js("Number(new URLSearchParams(window.location.search).get('zoom')) || 0")
