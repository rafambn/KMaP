package com.rafambn.kmap.source

import com.rafambn.kmap.mapProperties.CoordinatesRange
import com.rafambn.kmap.mapProperties.Latitude
import com.rafambn.kmap.mapProperties.Longitude

data class OSMCoordinatesRange(
    override val latitude: Latitude = Latitude(north = 85.051129, south = -85.051129),
    override val longitude: Longitude = Longitude(east = 180.0, west = -180.0),
) : CoordinatesRange
