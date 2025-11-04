package com.rafambn.kmap.customSources

import androidx.compose.ui.graphics.ImageBitmap
import com.rafambn.kmap.mapSource.tiled.RasterTile
import com.rafambn.kmap.mapSource.tiled.TileSpecs
import com.rafambn.kmap.mapSource.tiled.raster.RasterTileResult
import com.rafambn.kmap.mapSource.tiled.raster.RasterTileSource
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

class OSMTileSource(private val userAgent: String) : RasterTileSource {
    private val client = HttpClient()

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getTile(zoom: Int, row: Int, column: Int): RasterTileResult {
        val imageBitmap: ImageBitmap
        try {
            val byteArray = client.get("https://tile.openstreetmap.org/$zoom/$column/$row.png") {
                header("User-Agent", userAgent)
            }.readRawBytes()
            imageBitmap = byteArray.decodeToImageBitmap()
            return RasterTileResult.Success(RasterTile(zoom, row, column, imageBitmap))
        } catch (ex: Exception) {
            println(ex)
            return RasterTileResult.Failure(TileSpecs(zoom, row, column))
        }
    }
}
