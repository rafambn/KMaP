package io.github.rafambn.kmap.states

import io.github.rafambn.kmap.enums.MapBorderType
import io.github.rafambn.kmap.enums.OutsideTilesType

class MapProperties(
   val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
   val  outsideTiles: OutsideMapTiles = OutsideMapTiles(OutsideTilesType.NONE, OutsideTilesType.NONE),
   val maxMapZoom: Int = 19,
   val minMapZoom: Int = 0
)

data class BoundMapBorder(val horizontal: MapBorderType, val vertical: MapBorderType)
data class OutsideMapTiles(val horizontal: OutsideTilesType, val vertical: OutsideTilesType)