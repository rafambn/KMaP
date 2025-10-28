package com.rafambn.kmap.customSources

import androidx.compose.ui.graphics.ImageBitmap
import com.rafambn.kmap.mapSource.tiled.RasterTile
import com.rafambn.kmap.mapSource.tiled.TileSpecs
import com.rafambn.kmap.mapSource.tiled.raster.RasterTileResult
import com.rafambn.kmap.mapSource.tiled.raster.RasterTileSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

class OSMTileSource(private val userAgent: String) : RasterTileSource {
    private val client = HttpClient()

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getTile(zoom: Int, row: Int, column: Int): RasterTileResult {
        val imageBitmap: ImageBitmap
        try {
            val byteArray = client.get("https://tile.openstreetmap.org/$zoom/$row/$column.png") {
                header("User-Agent", userAgent)
            }.readRawBytes()
            imageBitmap = byteArray.decodeToImageBitmap()
            return RasterTileResult.Success(RasterTile(zoom, row, column, imageBitmap))
        } catch (ex: Exception) {
            println(ex) //TODO understand error after libs upgrade
            return RasterTileResult.Failure(TileSpecs(zoom, row, column))
        }
    }
}
