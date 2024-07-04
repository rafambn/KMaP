package io.github.rafambn.kmap.config.customSources

import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.config.characteristics.BOTTOM_TO_TOP
import io.github.rafambn.kmap.config.characteristics.LEFT_TO_RIGHT
import io.github.rafambn.kmap.config.characteristics.Latitude
import io.github.rafambn.kmap.config.characteristics.Longitude
import io.github.rafambn.kmap.config.characteristics.MapCoordinatesRange
import io.github.rafambn.kmap.config.characteristics.MapSource
import io.github.rafambn.kmap.config.characteristics.MapZoomLevelsRange
import io.github.rafambn.kmap.model.ResultTile
import io.github.rafambn.kmap.model.Tile
import io.github.rafambn.kmap.model.TileResult
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
    override val zoomLevels = OSMZoomLevelsRange()
    override val mapCoordinatesRange = OSMCoordinatesRange()
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

data class OSMZoomLevelsRange(override val max: Int = 19, override val min: Int = 0) : MapZoomLevelsRange

data class OSMCoordinatesRange(
    override val latitude: Latitude = Latitude(north = 85.051129, south = -85.051129, orientation = BOTTOM_TO_TOP),
    override val longitute: Longitude = Longitude(east = 180.0, west = -180.0, orientation = LEFT_TO_RIGHT)
) : MapCoordinatesRange