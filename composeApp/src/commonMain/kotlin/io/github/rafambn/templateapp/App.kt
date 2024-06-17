package io.github.rafambn.templateapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rafambn.kmap.KMaP
import io.github.rafambn.kmap.config.sources.openStreetMaps.OSMMapSource
import io.github.rafambn.kmap.core.motion.rememberMotionController
import io.github.rafambn.kmap.core.state.rememberMapState
import io.github.rafambn.kmap.DefaultCanvasGestureListener
import io.github.rafambn.kmap.core.motion.MapSetFactory
import io.github.rafambn.templateapp.theme.AppTheme

@Composable
internal fun App() = AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box {
            val motionController = rememberMotionController()
            motionController.set(MapSetFactory.setZoom(5F))
            val mapState = rememberMapState()
            KMaP(
                modifier = Modifier.align(Alignment.Center).size(300.dp, 600.dp),
                motionController = motionController,
                mapSource = OSMMapSource,
                mapState = mapState,
                canvasGestureListener = DefaultCanvasGestureListener(),
                onCanvasChangeSize = {
                    mapState.onCanvasSizeChanged(it)
                }
            ) {
//                placers(
//                    listOf(
//                        Placer(
//                            mapState,
//                            ProjectedCoordinates(-45.949303, -21.424608),
//                            drawPosition = DrawPosition.BOTTOM_RIGHT,
//                            rotation = 45.0,
//                            rotateWithMap = true
//                        ),
//                    )
//                ) {
//                    Image(
//                        painterResource(Res.drawable.teste),
//                        "fd",
//                        Modifier
//                            .background(Color.Black)
//                            .size(32.dp)
//                    )
//                }
            }
        }
    }
}