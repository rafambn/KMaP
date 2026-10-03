package com.rafambn.kmap

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rafambn.kmap.camera.MotionController
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.gesture.MapGestureCallbacks
import com.rafambn.kmap.screens.*
import com.rafambn.kmap.theme.AppTheme
import kotlin.math.log2

@Composable
fun App() = AppTheme {
    Surface(modifier = Modifier.systemBarsPadding().fillMaxSize()) {
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
                StartScreen(
                    navigateSimpleMap = { navigationController.navigate(Routes.Simple) },
                    navigateLayers = { navigationController.navigate(Routes.Layers) },
                    navigateMarkers = { navigationController.navigate(Routes.Markers) },
                    navigatePath = { navigationController.navigate(Routes.Path) },
                    navigateAnimation = { navigationController.navigate(Routes.Animation) },
                    navigateOSM = { navigationController.navigate(Routes.OSMRemote) },
                    navigateClustering = { navigationController.navigate(Routes.Clustering) },
                    navigateSavedStateHandle = { navigationController.navigate(Routes.SavedStateHandle) },
                    navigateVectorTile = { navigationController.navigate(Routes.VectorTiles) },
                )
            }
            composable<Routes.Simple> {
                SimpleMapScreen(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
            composable<Routes.Layers> {
                LayersScreen(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
            composable<Routes.Markers> {
                MarkersScreen(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
            composable<Routes.Path> {
                PathScreen(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
            composable<Routes.Animation> {
                AnimationScreen(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
            composable<Routes.OSMRemote> {
                OSMRemoteScreen(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
            composable<Routes.Clustering> {
                ClusteringScreen(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
            composable<Routes.SavedStateHandle> {
                ViewmodelScreen(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
            composable<Routes.VectorTiles> {
                VectorTileScreen(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
        }
        FpsMonitor()
    }
}

expect val scrollScale: Int

fun getGestureDetector(motionController: MotionController): MapGestureCallbacks = MapGestureCallbacks(
    onDoubleTap = { offset -> motionController.move { zoomByCentered(-1 / 3F, offset) } },
    onTapSwipe = { zoomFactor, rotationDelta ->
        motionController.move {
            zoomBy(log2(zoomFactor))
            rotateBy(rotationDelta)
        }
    },
    onTwoFingerTap = { offset -> motionController.move { zoomByCentered(1 / 3F, offset) } },
    onTransform = { centroid, panDelta, zoomFactor, rotationDelta ->
        motionController.move {
            rotateByCentered(rotationDelta, centroid)
            zoomByCentered(log2(zoomFactor), centroid)
            positionBy(panDelta)
        }
    },
    onScroll = { position, scrollDeltaY -> motionController.move { zoomByCentered(scrollDeltaY / scrollScale, position) } },
)
