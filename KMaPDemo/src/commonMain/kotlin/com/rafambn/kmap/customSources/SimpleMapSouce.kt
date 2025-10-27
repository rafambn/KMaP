package com.rafambn.kmap.customSources

import com.rafambn.kmap.tiles.TileSource
import com.rafambn.kmap.tiles.Tile
import com.rafambn.kmap.tiles.TileResult
import kmap.kmapdemo.generated.resources.Res
import org.jetbrains.compose.resources.decodeToImageBitmap

class SimpleMapTileSource : TileSource {
    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileResult {
        val resourcePath = "drawable/${zoom}_${column}_${row}.png"
        val bytes = Res.readBytes(resourcePath)
        val imageBitmap = bytes.decodeToImageBitmap()
        return TileResult.Success(Tile(zoom, row, column, imageBitmap))
    }
}
