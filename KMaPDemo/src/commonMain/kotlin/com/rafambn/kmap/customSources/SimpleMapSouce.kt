package com.rafambn.kmap.customSources

import com.rafambn.kmap.config.characteristics.TileSource
import com.rafambn.kmap.model.Tile
import com.rafambn.kmap.model.TileRenderResult
import kmap.kmapdemo.generated.resources.Res
import org.jetbrains.compose.resources.decodeToImageBitmap

class SimpleMapTileSource : TileSource {
    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileRenderResult {
        val resourcePath = "drawable/${zoom}_${column}_${row}.png"
        val bytes = Res.readBytes(resourcePath)
        val imageBitmap = bytes.decodeToImageBitmap()
        return TileRenderResult.Success(Tile(zoom, row, column, imageBitmap))
    }
}