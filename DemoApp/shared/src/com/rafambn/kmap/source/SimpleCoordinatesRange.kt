package com.rafambn.kmap.source

import com.rafambn.kmap.mapProperties.CoordinatesRange
import com.rafambn.kmap.mapProperties.Latitude
import com.rafambn.kmap.mapProperties.Longitude

data class SimpleCoordinatesRange(
    override val latitude: Latitude = Latitude(north = 90.0, south = -90.0),
    override val longitude: Longitude = Longitude(west = -180.0, east = 180.0),
) : CoordinatesRange
