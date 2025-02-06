package com.rafambn.kmap

import androidx.compose.ui.layout.Placeable
import com.rafambn.kmap.lazyMarker.MeasuredComponent
import com.rafambn.kmap.lazyMarker.MeasuredComponentProvider
import kotlinx.coroutines.CoroutineScope


internal expect fun dsds(
    measuredComponentProvider: MeasuredComponentProvider,
    markersCount: Int,
    coroutineScope: CoroutineScope
): List<MeasuredComponent>