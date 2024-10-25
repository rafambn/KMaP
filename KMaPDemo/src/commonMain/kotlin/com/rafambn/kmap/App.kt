package com.rafambn.kmap

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rafambn.kmap.screens.LayerMapRoot
import com.rafambn.kmap.screens.SimpleMapRoot
import com.rafambn.kmap.screens.StartRoot
import com.rafambn.kmap.theme.AppTheme

@Composable
internal fun App() = AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
        val navigationController = rememberNavController()
        NavHost(
            navController = navigationController,
            startDestination = Routes.Start,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable<Routes.Start> {
                StartRoot(
                    navigateSimpleMap = { navigationController.navigate(Routes.SimpleMap) },
                    navigateLayers = { navigationController.navigate(Routes.LayerMap) },
                    navigateMarkers = {},
                    navigatePath = {},
                    navigateAnimation = {},
                    navigateOSM = {},
                    navigateClustering = {},
                    navigateWidgets = {}
                )
            }
            composable<Routes.SimpleMap> {
                SimpleMapRoot(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
            composable<Routes.LayerMap> {
                LayerMapRoot(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
        }
    }
}

//@Composable
//internal fun App() = AppTheme {
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