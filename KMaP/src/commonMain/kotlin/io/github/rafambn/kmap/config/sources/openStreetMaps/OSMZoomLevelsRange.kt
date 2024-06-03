package io.github.rafambn.kmap.config.sources.openStreetMaps

import io.github.rafambn.kmap.config.characteristics.MapZoomLevelsRange

object OSMZoomLevelsRange : MapZoomLevelsRange{
    override val max: Int = 19
    override val min: Int = 0
}