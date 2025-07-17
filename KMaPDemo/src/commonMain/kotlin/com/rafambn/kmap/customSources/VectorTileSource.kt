package com.rafambn.kmap.customSources

import androidx.compose.ui.graphics.ImageBitmap
import com.rafambn.kmap.tiles.Tile
import com.rafambn.kmap.tiles.TileRenderResult
import com.rafambn.kmap.tiles.TileSource
import com.rafambn.kmap.tiles.TileSpecs
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

class VectorTileSource(private val userAgent: String) : TileSource {
    private val client = HttpClient()

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileRenderResult {
        val imageBitmap: ImageBitmap
        try {
            val byteArray = client.get("https://vtiles.openhistoricalmap.org/maps/osm/$zoom/$row/$column").readRawBytes()
            imageBitmap = byteArray.decodeToImageBitmap()
            return TileRenderResult.Success(Tile(zoom, row, column, imageBitmap))
        } catch (ex: Exception) {
            return TileRenderResult.Failure(TileSpecs(zoom, row, column))
        }
    }
}
