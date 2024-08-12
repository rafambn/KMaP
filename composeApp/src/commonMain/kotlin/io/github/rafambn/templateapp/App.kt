package io.github.rafambn.templateapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.rafambn.kmap.DefaultCanvasGestureListener
import io.github.rafambn.kmap.KMaP
import io.github.rafambn.kmap.config.customSources.OSMMapProperties
import io.github.rafambn.kmap.config.customSources.OSMTileSource
import io.github.rafambn.kmap.core.CanvasParameters
import io.github.rafambn.kmap.core.ClusterParameters
import io.github.rafambn.kmap.core.DrawPosition
import io.github.rafambn.kmap.core.MarkerParameters
import io.github.rafambn.kmap.core.rememberMotionController
import io.github.rafambn.kmap.core.state.rememberMapState
import io.github.rafambn.kmap.utils.offsets.ProjectedCoordinates
import io.github.rafambn.templateapp.theme.AppTheme
import kmap_library_with_app.composeapp.generated.resources.Res
import kmap_library_with_app.composeapp.generated.resources.teste
import kmap_library_with_app.composeapp.generated.resources.teste2
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun App() = AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box {
            val motionController = rememberMotionController()
            val mapState = rememberMapState(mapProperties = OSMMapProperties())
            KMaP(
                modifier = Modifier.align(Alignment.Center).size(300.dp, 600.dp),
                motionController = motionController,
                mapState = mapState,
                canvasGestureListener = DefaultCanvasGestureListener()
            ) {
                canvas(
                    CanvasParameters(
                        zIndex = 0F,
                        alpha = 1F
                    ),
                    OSMTileSource::getTile
                )
                markers(
                    listOf(
                        MarkerParameters(
                            ProjectedCoordinates(-45.949303, -21.424608),
                            drawPosition = DrawPosition.BOTTOM_RIGHT,
                            rotateWithMap = true,
                            tag = "zika"
                        ),
                        MarkerParameters(
                            ProjectedCoordinates(-46.949303, -21.424608),
                            drawPosition = DrawPosition.BOTTOM_RIGHT,
                            rotateWithMap = true,
                            tag = "zika"
                        ),
                        MarkerParameters(
                            ProjectedCoordinates(180.0, -85.0),
                            drawPosition = DrawPosition.BOTTOM_RIGHT,
                            rotateWithMap = true,
                            tag = "zika"
                        ),
                        MarkerParameters(
                            ProjectedCoordinates(180.0, 85.0),
                            drawPosition = DrawPosition.TOP_RIGHT,
                            rotateWithMap = true,
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
                                println("fsdfd")
                            }
                    )
                }
                cluster(ClusterParameters("zika", 50.dp, rotateWithMap = true)) {
                    Image(
                        painterResource(Res.drawable.teste2),
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