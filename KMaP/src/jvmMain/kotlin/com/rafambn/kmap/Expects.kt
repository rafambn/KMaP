package com.rafambn.kmap

import com.rafambn.kmap.lazyMarker.MeasuredComponent
import com.rafambn.kmap.lazyMarker.MeasuredComponentProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

internal actual fun dsds(
    measuredComponentProvider: MeasuredComponentProvider,
    markersCount: Int,
    coroutineScope: CoroutineScope
): List<MeasuredComponent> {
    var test = emptyList<MeasuredComponent>()
    val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    runBlocking(Dispatchers.IO) {
        test = (0 until markersCount).map { index ->
            async {
                measuredComponentProvider.getAndMeasure(index)
            }
        }.awaitAll()
    }
    return test
}