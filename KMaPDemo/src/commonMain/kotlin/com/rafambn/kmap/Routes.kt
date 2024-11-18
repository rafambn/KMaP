package com.rafambn.kmap

import kotlinx.serialization.Serializable

sealed interface Routes {
    @Serializable
    data object Start

    @Serializable
    data object Simple

    @Serializable
    data object Layers

    @Serializable
    data object Markers

    @Serializable
    data object Path

    @Serializable
    data object Animation
}