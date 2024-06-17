package io.github.rafambn.kmap.config

import io.github.rafambn.kmap.config.border.BoundMapBorder
import io.github.rafambn.kmap.config.border.MapBorderType
import io.github.rafambn.kmap.config.border.OutsideTilesType

data class DefaultMapProperties(
    override val boundMap: BoundMapBorder = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND),
    override val outsideTiles: OutsideTilesType = OutsideTilesType.NONE
) : MapProperties

interface MapProperties {
    val boundMap: BoundMapBorder
    val outsideTiles: OutsideTilesType
}