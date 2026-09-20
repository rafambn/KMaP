package com.rafambn.kmap.components.parameters

import com.rafambn.kmap.geometry.angle.Degrees

open class ClusterParameters(
    val id: Int,
    val alpha: Float = 1F,
    val zIndex: Float = 2F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = Degrees.Zero
) : Parameters
