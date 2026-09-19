package com.rafambn.kmap.source

import com.rafambn.kmap.tile.TileResult
import com.rafambn.kmap.tile.TileSource
import com.rafambn.kmap.tile.RasterTile
import kmap.kmapdemo.generated.resources.Res
import org.jetbrains.compose.resources.decodeToImageBitmap

class SimpleMapTileSource : TileSource<RasterTile> {
    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileResult<RasterTile> {
        val resourcePath = "drawable/${zoom}_${row}_${column}.png"
        val bytes = Res.readBytes(resourcePath)
        val imageBitmap = bytes.decodeToImageBitmap()
        return TileResult.Success(RasterTile(zoom, row, column, imageBitmap))
    }
}
