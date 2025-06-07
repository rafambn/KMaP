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
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rafambn.kmap.core.MotionController
import com.rafambn.kmap.gestures.MapGestureWrapper
import com.rafambn.kmap.gestures.detectMapGestures
import com.rafambn.kmap.screens.AnimationScreen
import com.rafambn.kmap.screens.ClusteringScreen
import com.rafambn.kmap.screens.LayersScreen
import com.rafambn.kmap.screens.MarkersScreen
import com.rafambn.kmap.screens.OSMRemoteScreen
import com.rafambn.kmap.screens.PathScreen
import com.rafambn.kmap.screens.SimpleMapScreen
import com.rafambn.kmap.screens.StartScreen
import com.rafambn.kmap.screens.ViewmodelScreen
import com.rafambn.kmap.theme.AppTheme
import com.rafambn.kmap.utils.asDifferentialScreenOffset

@Composable
fun App() = AppTheme {
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
                StartScreen(
                    navigateSimpleMap = { navigationController.navigate(Routes.Simple) },
                    navigateLayers = { navigationController.navigate(Routes.Layers) },
                    navigateMarkers = { navigationController.navigate(Routes.Markers) },
                    navigatePath = { navigationController.navigate(Routes.Path) },
                    navigateAnimation = { navigationController.navigate(Routes.Animation) },
                    navigateOSM = { navigationController.navigate(Routes.OSMRemote) },
                    navigateClustering = { navigationController.navigate(Routes.Clustering) },
                    navigateSavedStateHandle = { navigationController.navigate(Routes.SavedStateHandle) },
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
        }
    }
}

expect val scrollScale: Int

expect val gestureScale: Int

fun getGestureDetector(motionController: MotionController): MapGestureWrapper = MapGestureWrapper(
    onDoubleTap = { offset -> motionController.move { zoomByCentered(-1 / 3F, offset) } },
    onTapLongPress = { offset -> motionController.move { positionBy(offset.asDifferentialScreenOffset()) } },
    onTapSwipe = { zoom -> motionController.move { zoomBy(zoom / 100) } },
    onTwoFingersTap = { offset -> motionController.move { zoomByCentered(1 / 3F, offset) } },
    onGesture = { centroid, pan, zoom, rotation ->
        motionController.move {
            rotateByCentered(rotation.toDouble(), centroid)
            zoomByCentered(zoom / gestureScale, centroid)
            positionBy(pan)
        }
    },
    onScroll = { mouseOffset, scrollAmount -> motionController.move { zoomByCentered(scrollAmount / scrollScale, mouseOffset) } },
)
