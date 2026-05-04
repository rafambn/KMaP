package com.rafambn.kmap.customSources

import com.rafambn.kmap.mapSource.tiled.TileResult
import com.rafambn.kmap.mapSource.tiled.TileSource
import com.rafambn.kmap.mapSource.tiled.tiles.RasterTile
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
