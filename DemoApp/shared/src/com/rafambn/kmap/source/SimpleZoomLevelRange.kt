package com.rafambn.kmap.source

import com.rafambn.kmap.map.ZoomLevelRange

data class SimpleZoomLevelRange(
    override val max: Int = 2,
    override val min: Int = 0,
) : ZoomLevelRange
