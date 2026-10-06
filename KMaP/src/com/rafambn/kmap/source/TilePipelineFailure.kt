package com.rafambn.kmap.source

data class TilePipelineFailure(val specs: TileSpecs, val stage: String, val message: String, val attempts: Int)
