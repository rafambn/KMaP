package com.rafambn.kmap.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rafambn.kmap.DefaultCanvasGestureListener
import com.rafambn.kmap.KMaP
import com.rafambn.kmap.config.MapProperties
import com.rafambn.kmap.config.border.BoundMapBorder
import com.rafambn.kmap.config.border.MapBorderType
import com.rafambn.kmap.config.border.OutsideTilesType
import com.rafambn.kmap.config.characteristics.LEFT_TO_RIGHT
import com.rafambn.kmap.config.characteristics.Latitude
import com.rafambn.kmap.config.characteristics.Longitude
import com.rafambn.kmap.config.characteristics.MapCoordinatesRange
import com.rafambn.kmap.config.characteristics.MapZoomLevelsRange
import com.rafambn.kmap.config.characteristics.TOP_TO_BOTTOM
import com.rafambn.kmap.config.characteristics.TileSource
import com.rafambn.kmap.core.rememberMotionController
import com.rafambn.kmap.core.state.rememberMapState
import com.rafambn.kmap.model.ResultTile
import com.rafambn.kmap.model.Tile
import com.rafambn.kmap.model.TileResult
import com.rafambn.kmap.utils.offsets.CanvasPosition
import com.rafambn.kmap.utils.offsets.ProjectedCoordinates
import kmap.kmapdemo.generated.resources.Res
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
fun SimpleMapRoot() {
    val motionController = rememberMotionController()
    val mapState = rememberMapState(mapProperties = SimpleMapProperties())
    KMaP(
        modifier = Modifier.fillMaxSize(),
        motionController = motionController,
        mapState = mapState,
        canvasGestureListener = DefaultCanvasGestureListener()
    ) {
        canvas(tileSource = SimpleMapTileSource()::getTile)
    }
}

data class SimpleMapProperties(
    override val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    override val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    override val zoomLevels: MapZoomLevelsRange = SimpleMapZoomLevelsRange(),
    override val mapCoordinatesRange: MapCoordinatesRange = SimpleMapCoordinatesRange(),
    override val tileSize: Int = 900
) : MapProperties {
    override fun toCanvasPosition(projectedCoordinates: ProjectedCoordinates): CanvasPosition = CanvasPosition(
        projectedCoordinates.horizontal,
        projectedCoordinates.vertical
    )

    override fun toProjectedCoordinates(canvasPosition: CanvasPosition): ProjectedCoordinates = ProjectedCoordinates(
        canvasPosition.horizontal,
        canvasPosition.vertical
    )
}

data class SimpleMapZoomLevelsRange(override val max: Int = 2, override val min: Int = 0) : MapZoomLevelsRange

data class SimpleMapCoordinatesRange(
    override val latitude: Latitude = Latitude(north = 90.0, south = -90.0, orientation = TOP_TO_BOTTOM),
    override val longitude: Longitude = Longitude(east = 180.0, west = -180.0, orientation = LEFT_TO_RIGHT)
) : MapCoordinatesRange

class SimpleMapTileSource : TileSource {
    override suspend fun getTile(zoom: Int, row: Int, column: Int): ResultTile {
        println("${zoom}_${column}_${row}")
        val resourcePath = "drawable/${zoom}_${column}_${row}.png"
        val bytes = Res.readBytes(resourcePath)
        val imageBitmap = bytes.decodeToImageBitmap()
        return ResultTile(Tile(zoom, row, column, imageBitmap), TileResult.SUCCESS)
    }
}