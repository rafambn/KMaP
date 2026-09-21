package com.rafambn.kmap.components.parameters

import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.VectorTile
import com.rafambn.kmap.style.OptimizedStyle

open class VectorCanvasParameters(
    override val id: Int,
    override val alpha: Float = 1F,
    override val zIndex: Float = 0F,
    override val maxCacheTiles: Int = 20,
    val tileSource: suspend (zoom: Int, row: Int, column: Int) -> TileResult<VectorTile>,
    val style: OptimizedStyle,
) : CanvasParameters
