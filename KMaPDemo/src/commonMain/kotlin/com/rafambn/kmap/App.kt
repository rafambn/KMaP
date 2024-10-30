package com.rafambn.kmap

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rafambn.kmap.config.customSources.OSMMapProperties
import com.rafambn.kmap.config.customSources.OSMTileSource
import com.rafambn.kmap.core.ClusterParameters
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.core.MarkerParameters
import com.rafambn.kmap.core.rememberMotionController
import com.rafambn.kmap.core.state.rememberMapState
import com.rafambn.kmap.screens.LayerMapRoot
import com.rafambn.kmap.screens.MarkerMapRoot
import com.rafambn.kmap.screens.SimpleMapProperties
import com.rafambn.kmap.screens.SimpleMapRoot
import com.rafambn.kmap.screens.SimpleMapTileSource
import com.rafambn.kmap.screens.StartRoot
import com.rafambn.kmap.theme.AppTheme
import com.rafambn.kmap.utils.offsets.ProjectedCoordinates
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.teste
import kmap.kmapdemo.generated.resources.teste2
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random

@Composable
fun App() = AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box {
            val list = mutableStateListOf<String>()
            var bool by mutableStateOf(false)
            LazyColumn {
                items(list){
                    if (bool)
                        Text(it)
                    else
                        Text("noip")
                }
            }
            Button(onClick = {
                list.add("oitr")
            }, modifier = Modifier.align(Alignment.BottomCenter)) {
                Text("click")
            }
            Button(onClick = {
                bool = !bool
            }, modifier = Modifier.align(Alignment.BottomEnd)) {
                Text("modify")
            }
            Button(onClick = {
                list[0] = "fdsfs"
            }, modifier = Modifier.align(Alignment.BottomStart)) {
                Text("modify 2")
            }
        }
    }
}

//@Composable
// fun App() = AppTheme {
//    Surface(modifier = Modifier.fillMaxSize()) {
//        val navigationController = rememberNavController()
//        NavHost(
//            navController = navigationController,
//            startDestination = Routes.Start,
//            enterTransition = {
//                slideInHorizontally(
//                    initialOffsetX = { fullWidth -> fullWidth },
//                    animationSpec = tween(durationMillis = 300)
//                ) + fadeIn(animationSpec = tween(300))
//            },
//            exitTransition = {
//                slideOutHorizontally(
//                    targetOffsetX = { fullWidth -> -fullWidth },
//                    animationSpec = tween(durationMillis = 300)
//                ) + fadeOut(animationSpec = tween(300))
//            }
//        ) {
//            composable<Routes.Start> {
//                StartRoot(
//                    navigateSimpleMap = { navigationController.navigate(Routes.SimpleMap) },
//                    navigateLayers = { navigationController.navigate(Routes.LayerMap) },
//                    navigateMarkers = {navigationController.navigate(Routes.MarkersMap) },
//                    navigatePath = {},
//                    navigateAnimation = {},
//                    navigateOSM = {},
//                    navigateClustering = {},
//                    navigateWidgets = {}
//                )
//            }
//            composable<Routes.SimpleMap> {
//                SimpleMapRoot(
//                    navigateBack = { navigationController.popBackStack() }
//                )
//            }
//            composable<Routes.LayerMap> {
//                LayerMapRoot(
//                    navigateBack = { navigationController.popBackStack() }
//                )
//            }
//            composable<Routes.MarkersMap> {
//                MarkerMapRoot(
//                    navigateBack = { navigationController.popBackStack() }
//                )
//            }
//        }
//    }
//}

//@Composable
// fun App() = AppTheme {
//    Surface(modifier = Modifier.fillMaxSize()) {
//        Box {
//            val motionController = rememberMotionController()
//            val mapState = rememberMapState(mapProperties = OSMMapProperties())
//            KMaP(
//                modifier = Modifier.align(Alignment.Center).size(300.dp, 600.dp),
//                motionController = motionController,
//                mapState = mapState,
//                canvasGestureListener = DefaultCanvasGestureListener()
//            ) {
//                canvas(tileSource = OSMTileSource("com.rafambn.kmap")::getTile)
//                markers(
//                    listOf(
//                        MarkerParameters(
//                            ProjectedCoordinates(-45.949303, -21.424608),
//                            drawPosition = DrawPosition.BOTTOM_RIGHT,
//                            rotateWithMap = true,
//                            tag = "zika"
//                        ),
//                        MarkerParameters(
//                            ProjectedCoordinates(-46.949303, -21.424608),
//                            drawPosition = DrawPosition.BOTTOM_RIGHT,
//                            rotateWithMap = true,
//                            tag = "zika"
//                        ),
//                        MarkerParameters(
//                            ProjectedCoordinates(180.0, -85.0),
//                            drawPosition = DrawPosition.BOTTOM_RIGHT,
//                            rotateWithMap = true,
//                            tag = "zika"
//                        ),
//                    )
//                ) {
//                    Image(
//                        painterResource(Res.drawable.teste),
//                        "fd",
//                        Modifier
//                            .background(Color.Black)
//                            .size(32.dp)
//                            .clickable {
//                                println("fsdfd")
//                            }
//                    )
//                }
//                markers(
//                    listOf(
//                        MarkerParameters(
//                            ProjectedCoordinates(180.0, 85.0),
//                            drawPosition = DrawPosition.TOP_RIGHT,
//                            rotateWithMap = false,
//                        )
//                    )
//                ) {
//                    val cor = remember { mutableStateOf(Color.Black) }
//                    val rnd = remember { Random(545) }
//                    Box(
//                        Modifier
//                            .background(cor.value)
//                            .size(32.dp)
//                            .clickable {
//                                cor.value = Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
//                            }
//                    ) {
//                    }
//                }
//                cluster(ClusterParameters("zika", 50.dp, rotateWithMap = true)) {
//                    Image(
//                        painterResource(Res.drawable.teste2),
//                        "fd",
//                        Modifier
//                            .background(Color.Black)
//                            .size(32.dp)
//                            .clickable {
//                                println("fsdfd")
//                            }
//                    )
//                }
//            }
//        }
//    }
//}