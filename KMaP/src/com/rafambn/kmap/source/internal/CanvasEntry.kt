package com.rafambn.kmap.source.internal

import kotlinx.coroutines.Job

internal class CanvasEntry(
    val engine: CanvasEngine<*>,
    val job: Job,
)
