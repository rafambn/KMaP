package com.rafambn.kmap

import kotlinx.serialization.Serializable

sealed interface Routes {
    @Serializable
    data object Start

    @Serializable
    data object SimpleMap

    @Serializable
    data object LayerMap
}