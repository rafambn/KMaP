package io.github.rafambn.kmap.config.sources.openStreetMaps

import io.github.rafambn.kmap.config.characteristics.BOTTOM_TO_TOP
import io.github.rafambn.kmap.config.characteristics.LEFT_TO_RIGHT
import io.github.rafambn.kmap.config.characteristics.Latitude
import io.github.rafambn.kmap.config.characteristics.Longitude
import io.github.rafambn.kmap.config.characteristics.MapCoordinatesRange

object OSMCoordinatesRange : MapCoordinatesRange {
    override val latitude: Latitude = Latitude(north = 85.051129, south = -85.051129, orientation = BOTTOM_TO_TOP)
    override val longitute: Longitude = Longitude(east = 180.0, west = -180.0, orientation = LEFT_TO_RIGHT)
}