package io.github.rafambn.kmap.config

import io.github.rafambn.kmap.config.border.BoundMapBorder
import io.github.rafambn.kmap.config.border.MapBorderType
import io.github.rafambn.kmap.config.border.OutsideTilesType
import io.github.rafambn.kmap.config.sources.openStreetMaps.OSMMapSource

data class MapProperties(
    val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    val outsideTiles: OutsideTilesType = OutsideTilesType.NONE,
    val mapSource: MapSource = OSMMapSource  //TODO add source future -- online, db, cache or mapFile
)