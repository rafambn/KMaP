package io.github.rafambn.templateapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.rafambn.kmap.DefaultCanvasGestureListener
import io.github.rafambn.kmap.KMaP
import io.github.rafambn.kmap.config.customSources.OSMMapProperties
import io.github.rafambn.kmap.config.customSources.OSMTileSource
import io.github.rafambn.kmap.core.DrawPosition
import io.github.rafambn.kmap.core.CanvasData
import io.github.rafambn.kmap.core.MarkerData
import io.github.rafambn.kmap.core.rememberMotionController
import io.github.rafambn.kmap.core.state.rememberMapState
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.templateapp.theme.AppTheme
import kmap_library_with_app.composeapp.generated.resources.Res
import kmap_library_with_app.composeapp.generated.resources.teste
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun App() = AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box {
            val motionController = rememberMotionController()
            val mapState = rememberMapState(mapProperties = OSMMapProperties())
            var show by mutableStateOf(true)
            CoroutineScope(Dispatchers.Default).launch {
                delay(5000)
                show = false
            }
            KMaP(
                modifier = Modifier.align(Alignment.Center).size(300.dp, 600.dp),
                motionController = motionController,
                mapState = mapState,
                canvasGestureListener = DefaultCanvasGestureListener()
            ) {
                canvas(
                    CanvasData(
                        zIndex = 0F,
                        alpha = 1F
                    ),
                    OSMTileSource::getTile
                )
                markers(
                    listOf(
                        MarkerData(
                            ProjectedCoordinates(-45.949303, -21.424608),
                            drawPosition = DrawPosition.BOTTOM_RIGHT,
                            rotation = -45.0,
                            rotateWithMap = true
                        ),
                    )
                ) {
                    if (show)
                        Image(
                            painterResource(Res.drawable.teste),
                            "fd",
                            Modifier
                                .background(Color.Black)
                                .size(32.dp)
                                .clickable {
                                    println("fsdfd")
                                }
                        )
                }
            }
        }
    }
}