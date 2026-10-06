package com.rafambn.kmap.source

/** Per-canvas diagnostics. Worker time includes transport; conversion covers Compose object creation. */
data class TilePipelineStatus(
    val fetching: Int = 0,
    val pendingPreparation: Int = 0,
    val preparing: Int = 0,
    val workerTiles: Int = 0,
    val compatibilityTiles: Int = 0,
    val rawCacheBytes: Int = 0,
    val preparedCacheBytes: Int = 0,
    val fetchMillis: Double = 0.0,
    val preparationMillis: Double = 0.0,
    val conversionMillis: Double = 0.0,
    val failures: List<TilePipelineFailure> = emptyList(),
)
