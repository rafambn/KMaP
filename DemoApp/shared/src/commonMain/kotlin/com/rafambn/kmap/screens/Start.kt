package com.rafambn.kmap.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

@Composable
fun StartScreen(
    navigateSimpleMap: () -> Unit,
    navigateLayers: () -> Unit,
    navigateMarkers: () -> Unit,
    navigatePath: () -> Unit,
    navigateAnimation: () -> Unit,
    navigateOSM: () -> Unit,
    navigateClustering: () -> Unit,
    navigateSavedStateHandle: () -> Unit,
    navigateVectorTile: () -> Unit,
) {
    val columnItems = listOf(
        Pair(navigateSimpleMap, "SimpleMap"),
        Pair(navigateLayers, "Layers"),
        Pair(navigateMarkers, "Markers"),
        Pair(navigatePath, "Paths"),
        Pair(navigateAnimation, "Animations"),
        Pair(navigateOSM, "Remote with Open Street Maps"),
        Pair(navigateClustering, "Clustering"),
        Pair(navigateSavedStateHandle, "SavedStateHandle with ViewModel"),
        Pair(navigateVectorTile, "Remote Vector Tiles"),
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
        item {
            Text("Tiles generated with Azgaar's Fantasy Map Generator")
        }
    }
}
