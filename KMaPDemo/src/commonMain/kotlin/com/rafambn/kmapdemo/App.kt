package com.rafambn.kmapdemo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rafambn.kmapdemo.config.customSources.OSMMapProperties
import com.rafambn.kmapdemo.config.customSources.OSMTileSource
import com.rafambn.kmapdemo.core.ClusterParameters
import com.rafambn.kmapdemo.core.DrawPosition
import com.rafambn.kmapdemo.core.MarkerParameters
import com.rafambn.kmapdemo.core.MotionController
import com.rafambn.kmapdemo.core.MotionController.CenterLocation
import com.rafambn.kmapdemo.core.rememberMotionController
import com.rafambn.kmapdemo.core.state.rememberMapState
import com.rafambn.kmapdemo.theme.AppTheme
import com.rafambn.kmapdemo.utils.offsets.CanvasPosition
import com.rafambn.kmapdemo.utils.offsets.ProjectedCoordinates
import com.rafambn.kmapdemo.utils.offsets.ScreenOffset
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.teste
import kmap.kmapdemo.generated.resources.teste2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random

@Composable
internal fun App() = AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box {
            val motionController = rememberMotionController()
            CoroutineScope(Dispatchers.Default).launch {
                motionController.animate {
                    positionTo(CenterLocation.Offset(Offset(5F, 5F)))
                    positionBy(CenterLocation.Offset(Offset(10F, 10F)))
                }
            }
            val mapState = rememberMapState(mapProperties = OSMMapProperties())
            KMaP(
                modifier = Modifier.align(Alignment.Center).size(300.dp, 600.dp),
                motionController = motionController,
                mapState = mapState,
                canvasGestureListener = DefaultCanvasGestureListener()
            ) {
                canvas(tileSource = OSMTileSource::getTile)
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
                markers(
                    listOf(
                        MarkerParameters(
                            ProjectedCoordinates(180.0, 85.0),
                            drawPosition = DrawPosition.TOP_RIGHT,
                            rotateWithMap = false,
                        )
                    )
                ) {
                    val cor = remember { mutableStateOf(Color.Black) }
                    val rnd = remember { Random(545) }
                    Box(
                        Modifier
                            .background(cor.value)
                            .size(32.dp)
                            .clickable {
                                cor.value = Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
                            }
                    ) {
                    }
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