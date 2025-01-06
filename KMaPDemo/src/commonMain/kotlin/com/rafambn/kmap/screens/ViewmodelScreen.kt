package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.getGestureDetector
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ViewmodelScreen(
    navigateBack: () -> Unit
) {
    val viewmodel = viewModel<MyViewmodel>()
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        viewmodel.mapState.density = density
    }
    Box{
        KMaP(
            modifier = Modifier.fillMaxSize(),
            mapState = viewmodel.mapState,
        ) {
            canvas(
                tileSource = SimpleMapTileSource()::getTile,
                gestureDetection = getGestureDetector(viewmodel.mapState.motionController)
            )
        }
        Image(
            imageVector = vectorResource(Res.drawable.back_arrow),
            contentDescription = "",
            modifier = Modifier.clickable { navigateBack() }
                .size(70.dp)
        )
    }
}

class MyViewmodel(savedStateHandle: SavedStateHandle) : ViewModel() {
    @OptIn(SavedStateHandleSaveableApi::class)
    val mapState = savedStateHandle.saveable(
        key = "mapState",
        saver = MapState.saver(SimpleMapProperties()),
        init = {
            MapState(
                mapProperties = SimpleMapProperties(),
                zoomLevelPreference = null
            )
        }
    )
}