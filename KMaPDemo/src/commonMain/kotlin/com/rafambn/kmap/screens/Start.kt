package com.rafambn.kmap.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

@Composable
fun StartRoot(
    navigateSimpleMap: () -> Unit,
    navigateLayers: () -> Unit,
    navigateMarkers: () -> Unit,
    navigatePath: () -> Unit,
    navigateAnimation: () -> Unit,
    navigateOSM: () -> Unit,
    navigateClustering: () -> Unit,
    navigateWidgets: () -> Unit
) {
    val columnItems = listOf(
        Pair(navigateSimpleMap, "SimpleMap"),
        Pair(navigateLayers, "Layers"),
        Pair(navigateMarkers, "Markers"),
        Pair(navigatePath, "Paths"),
        Pair(navigateAnimation, "Animations"),
        Pair(navigateOSM, "Remote with Open Street Maps"),
        Pair(navigateClustering, "Clustering"),
        Pair(navigateWidgets, "Widgets"),
    )
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items(columnItems) {
            Button(onClick = it.first) {
                Text(it.second)
            }
        }
    }
}