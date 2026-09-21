package com.rafambn.kmap.source

import com.rafambn.kmap.mapProperties.ZoomLevelRange

data class OSMZoomLevelRange(
    override val max: Int = 19,
    override val min: Int = 0,
) : ZoomLevelRange
