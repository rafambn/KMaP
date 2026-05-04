package com.rafambn.kmap.customSources

import com.rafambn.kmap.mapSource.tiled.TileResult
import com.rafambn.kmap.mapSource.tiled.TileSource
import com.rafambn.kmap.mapSource.tiled.tiles.RasterTile
import com.rafambn.kmap.mapSource.tiled.tiles.TileSpecs
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

class OSMTileSource(private val userAgent: String) : TileSource<RasterTile> {
    private val client = HttpClient()

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileResult<RasterTile> {
        val imageBitmap: androidx.compose.ui.graphics.ImageBitmap
        try {
            val byteArray = client.get("https://tile.openstreetmap.org/$zoom/$column/$row.png") {
                header("User-Agent", userAgent)
            }.readRawBytes()
            imageBitmap = byteArray.decodeToImageBitmap()
            return TileResult.Success(RasterTile(zoom, row, column, imageBitmap))
        } catch (ex: Exception) {
            println(ex)
            return TileResult.Failure(TileSpecs(zoom, row, column))
        }
    }
}
