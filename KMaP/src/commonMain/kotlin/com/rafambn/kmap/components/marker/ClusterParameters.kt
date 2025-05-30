package com.rafambn.kmap.components.marker

import com.rafambn.kmap.components.Parameters
import com.rafambn.kmap.utils.Degrees

open class ClusterParameters(
    val id: Int,
    val alpha: Float = 1F,
    val zIndex: Float = 2F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
) : Parameters
