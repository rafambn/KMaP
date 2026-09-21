package com.rafambn.kmap.source

import com.rafambn.kmap.source.RasterTile
import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.TileSource
import com.rafambn.kmap.source.TileSpecs
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

class OSMTileSource(private val userAgent: String) : TileSource<RasterTile> {
    private val client = HttpClient()

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
