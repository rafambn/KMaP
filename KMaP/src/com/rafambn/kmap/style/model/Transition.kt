package com.rafambn.kmap.style.model

import kotlinx.serialization.Serializable

@Serializable
data class Transition(
    val duration: Int? = null,
    val delay: Int? = null
)
