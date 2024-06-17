package io.github.rafambn.kmap.config.sources.openStreetMaps

import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.config.MapSource
import io.github.rafambn.kmap.model.Tile
import io.github.rafambn.kmap.utils.loopInZoom
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.kmap.utils.toImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

object OSMMapSource : MapSource {
    override val zoomLevels = OSMZoomLevelsRange
    override val mapCoordinatesRange = OSMCoordinatesRange
    override val tileSize = 256
    private val client = HttpClient()

    override fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition = CanvasPosition(
        projectedCoordinates.horizontal,
        ln(tan(PI / 4 + (PI * projectedCoordinates.vertical) / 360)) / (PI / 85.051129)
    )

    override fun toProjectedCoordinates(canvasPosition: CanvasPosition): ProjectedCoordinates = ProjectedCoordinates(
        canvasPosition.horizontal,
        (atan(E.pow(canvasPosition.vertical * (PI / 85.051129))) - PI / 4) * 360 / PI
    )

    override suspend fun getTile(zoom: Int, row: Int, column: Int): Tile {
        val imageBitmap: ImageBitmap
        try {
            val byteArray = client.get("https://tile.openstreetmap.org/${zoom}/${row.loopInZoom(zoom)}/${column.loopInZoom(zoom)}.png") {
                header("User-Agent", "my.app.test5")
            }.readBytes() //TODO(4) improve loopInZoom
            imageBitmap = byteArray.toImageBitmap()
            return Tile(zoom, row, column, imageBitmap)
        } catch (ex: Exception) {
            throw ex
        }
    }
}