package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rafambn.kmap.components.RasterCanvasParameters
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
    val density = LocalDensity.current
    val viewmodel = viewModel<MyViewmodel>(factory = MyViewmodel.Factory(density = density))
    Box {
        KMaP(
            modifier = Modifier.fillMaxSize(),
            mapState = viewmodel.mapState,
        ) {
            rasterCanvas(
                parameters = RasterCanvasParameters(id = 1, tileSource = SimpleMapTileSource()::getTile),
                gestureWrapper = getGestureDetector(viewmodel.mapState.motionController)
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

class MyViewmodel(savedStateHandle: SavedStateHandle, density: Density) : ViewModel() {
    @OptIn(SavedStateHandleSaveableApi::class)
    val mapState = savedStateHandle.saveable(
        key = "mapState",
        saver = MapState.saver(SimpleMapProperties(), viewModelScope),
        init = {
            MapState(
                mapProperties = SimpleMapProperties(),
                zoomLevelPreference = null,
                coroutineScope = viewModelScope,
                density = density
            )
        }
    )

    companion object {
        fun Factory(density: Density): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MyViewmodel(savedStateHandle = createSavedStateHandle(), density = density)
            }
        }
    }
}
