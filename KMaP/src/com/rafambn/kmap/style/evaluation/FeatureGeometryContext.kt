package com.rafambn.kmap.style.evaluation

data class FeatureGeometryContext(
    val paths: List<List<Pair<Int, Int>>>,
    val zoom: Int,
    val row: Int,
    val col: Int,
    val extent: Int
)
