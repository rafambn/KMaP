package io.github.rafambn.kmap.config.customSources

import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.config.characteristics.TileSource
import io.github.rafambn.kmap.model.ResultTile
import io.github.rafambn.kmap.model.Tile
import io.github.rafambn.kmap.model.TileResult
import io.github.rafambn.kmap.utils.loopInZoom
import io.github.rafambn.kmap.utils.toImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes

object OSMTileSource : TileSource {
    private val client = HttpClient()
    override suspend fun getTile(zoom: Int, row: Int, column: Int): ResultTile {
        val imageBitmap: ImageBitmap
        try {
            val byteArray = client.get("https://tile.openstreetmap.org/${zoom}/${row.loopInZoom(zoom)}/${column.loopInZoom(zoom)}.png") {
                header("User-Agent", "my.app.test5")
            }.readBytes() //TODO(4) improve loopInZoom
            imageBitmap = byteArray.toImageBitmap()
            return ResultTile(Tile(zoom, row, column, imageBitmap), TileResult.SUCCESS)
        } catch (ex: Exception) {
            return ResultTile(null, TileResult.SUCCESS)
        }
    }
}