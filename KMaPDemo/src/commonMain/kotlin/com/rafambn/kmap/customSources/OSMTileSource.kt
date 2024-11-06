package com.rafambn.kmap.customSources

import androidx.compose.ui.graphics.ImageBitmap
import com.rafambn.kmap.config.characteristics.TileSource
import com.rafambn.kmap.model.ResultTile
import com.rafambn.kmap.model.Tile
import com.rafambn.kmap.model.TileResult
import com.rafambn.kmap.utils.loopInZoom
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

class OSMTileSource(private val userAgent: String) : TileSource {
    private val client = HttpClient()
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getTile(zoom: Int, row: Int, column: Int): ResultTile {
        val imageBitmap: ImageBitmap
        try {
            val byteArray = client.get("https://tile.openstreetmap.org/${zoom}/${row.loopInZoom(zoom)}/${column.loopInZoom(zoom)}.png") {
                header("User-Agent", userAgent)
            }.readBytes() //TODO(4) improve loopInZoom
            imageBitmap = byteArray.decodeToImageBitmap()
            return ResultTile(Tile(zoom, row, column, imageBitmap), TileResult.SUCCESS)
        } catch (ex: Exception) {
            return ResultTile(null, TileResult.FAILURE)
        }
    }
}