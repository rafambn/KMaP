package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.rafambn.kmap.enums.MapBorderType
import io.github.rafambn.kmap.enums.OutsideTilesType

class MapProperties(
   val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
   val  outsideTiles: OutsideMapTiles = OutsideMapTiles(OutsideTilesType.NONE, OutsideTilesType.NONE),
   val maxMapZoom: Float = 10F,
   val minMapZoom: Float = 0F
) {
}

data class BoundMapBorder(val horizontal: MapBorderType, val vertical: MapBorderType)
data class OutsideMapTiles(val horizontal: OutsideTilesType, val vertical: OutsideTilesType)