package io.github.rafambn.templateapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.rafambn.kmap.KMaP
import io.github.rafambn.kmap.config.sources.openStreetMaps.OSMMapSource
import io.github.rafambn.kmap.core.DrawPosition
import io.github.rafambn.kmap.core.Placer
import io.github.rafambn.kmap.core.state.rememberMapState
import io.github.rafambn.kmap.gestures.DefaultCanvasGestureListener
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.templateapp.theme.AppTheme
import kmap_library_with_app.composeapp.generated.resources.Res
import kmap_library_with_app.composeapp.generated.resources.teste
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun App() = AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box {
            val mapState = rememberMapState(
                coroutineScope = CoroutineScope(Dispatchers.Default),
                mapSource = OSMMapSource,
            )
            KMaP(
                modifier = Modifier.align(Alignment.Center).size(300.dp, 600.dp),
                onCanvasChangeSize = {
                    mapState.onCanvasSizeChanged(it)
                }
            ) {
                tileCanvas( //TODO make it receive a tileProvider
                    zIndex = 0F,
                    alpha = 1F,
                    gestureListener = DefaultCanvasGestureListener(mapState.motionController),
                    tileCanvasStateModel = mapState.tileCanvasStateFlow.collectAsState().value
                )
                placers(
                    listOf(
                        Placer(
                            mapState,
                            ProjectedCoordinates(-45.949303, -21.424608),
                            DrawPosition.TOP_LEFT,
                        ),
                    )
                ) {
                    Image(
                        painterResource(Res.drawable.teste),
                        "fd",
                        Modifier
                            .background(Color.Black)
                            .size(32.dp)
                            .clickable {
                                println("Clicked")
                            }
                    )
                }
            }
        }
    }
}