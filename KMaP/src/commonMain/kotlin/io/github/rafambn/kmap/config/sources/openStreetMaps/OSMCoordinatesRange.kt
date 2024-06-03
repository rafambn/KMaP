package io.github.rafambn.kmap.config.sources.openStreetMaps

import io.github.rafambn.kmap.config.characteristics.Latitude
import io.github.rafambn.kmap.config.characteristics.Longitude
import io.github.rafambn.kmap.config.characteristics.MapCoordinatesRange

object OSMCoordinatesRange : MapCoordinatesRange {
    override val latitude: Latitude
        get() = Latitude(north = 85.051129, south = -85.051129, orientation = 1)
    override val longitute: Longitude
        get() = Longitude(east = 180.0, west = -180.0, orientation = -1)

}