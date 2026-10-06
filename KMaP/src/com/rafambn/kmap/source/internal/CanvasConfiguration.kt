package com.rafambn.kmap.source.internal

import com.rafambn.kmap.components.parameters.CanvasParameters

internal data class CanvasConfiguration(val parameters: List<CanvasParameters>, val generations: Map<Int, Long>)
