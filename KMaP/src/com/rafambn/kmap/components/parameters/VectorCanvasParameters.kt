package com.rafambn.kmap.components.parameters

import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.VectorTile
import com.rafambn.kmap.style.compiled.CompiledStyle

/**
 * Encoded tiles use owned workers and [CompiledStyle.preparation]. Decoded tiles use local compatibility
 * preparation. Call [com.rafambn.kmap.MapState.invalidateTiles] when source content changes.
 */
open class VectorCanvasParameters(
    override val id: Int,
    override val alpha: Float = 1F,
    override val zIndex: Float = 0F,
    override val maxCacheTiles: Int = 20,
    val tileSource: suspend (zoom: Int, row: Int, column: Int) -> TileResult<VectorTile>,
    val style: CompiledStyle,
) : CanvasParameters
