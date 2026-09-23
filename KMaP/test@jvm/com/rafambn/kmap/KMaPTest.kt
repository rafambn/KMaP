@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package com.rafambn.kmap

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.ProjectedCoordinates
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.mapProperties.coordinates.CoordinatesRange
import com.rafambn.kmap.mapProperties.coordinates.Latitude
import com.rafambn.kmap.mapProperties.coordinates.Longitude
import de.infix.testBalloon.framework.core.testSuite
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

val KMaPTest by testSuite {
    test("KMaP updates density and viewport across recompositions") {
        val properties = object : MapProperties {
            override val boundMap = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND)
            override val outsideTiles = OutsideTilesType.NONE
            override val zoomLevels = ZoomLevelRange(0, 3)
            override val coordinatesRange = object : CoordinatesRange {
                override val latitude = Latitude(north = 90.0, south = -90.0)
                override val longitude = Longitude(west = -180.0, east = 180.0)
            }
            override val tileSize = TileDimension(512.dp, 512.dp)

            override fun toProjectedCoordinates(coordinates: Coordinates) =
                ProjectedCoordinates(coordinates.x, coordinates.y)

            override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) =
                Coordinates(projectedCoordinates.x, projectedCoordinates.y)
        }
        val state = MapState(properties, coroutineScope = CoroutineScope(EmptyCoroutineContext))
        val replacementState = MapState(properties, coroutineScope = CoroutineScope(EmptyCoroutineContext))
        var activeState by mutableStateOf(state)
        var modifier by mutableStateOf<Modifier>(Modifier)
        var tick by mutableIntStateOf(0)
        var observedTick = -1
        val content: KMaPContent.() -> Unit = {}
        val frameRecomposer = FrameRecomposer(Dispatchers.Unconfined)
        val scene = CanvasLayersComposeScene(
            frameRecomposer = frameRecomposer,
            density = Density(2F),
            size = IntSize(64, 64),
        )

        try {
            scene.setContent(frameRecomposer.compositionContext) {
                observedTick = tick
                KMaP(activeState, content = content)
            }
            frameRecomposer.performFrame(0L)
            scene.measureAndLayout()

            assertEquals(2F, state.currentDensity.density)
            assertEquals(64, state.viewportSize.width)
            assertEquals(64, state.viewportSize.height)

            tick++
            frameRecomposer.performFrame(1L)
            scene.measureAndLayout()
            assertEquals(1, observedTick)

            activeState = replacementState
            frameRecomposer.performFrame(2L)
            scene.measureAndLayout()
            assertEquals(2F, replacementState.currentDensity.density)
            assertEquals(64, replacementState.viewportSize.width)

            scene.setContent(frameRecomposer.compositionContext) {
                KMaP(replacementState, modifier, content)
            }
            frameRecomposer.performFrame(3L)
            scene.measureAndLayout()

            modifier = Modifier.fillMaxSize()
            frameRecomposer.performFrame(4L)
            scene.measureAndLayout()
            assertEquals(64, replacementState.viewportSize.width)

            // Exercise the compiler's known-same, known-changed, and instance-checked argument paths.
            val method = Class.forName("com.rafambn.kmap.KMaPKt").getDeclaredMethod(
                "KMaP",
                MapState::class.java,
                Modifier::class.java,
                Function1::class.java,
                Composer::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            scene.setContent(frameRecomposer.compositionContext) {
                val composer = currentComposer
                for (mask in listOf(2, 4, 8)) {
                    key(mask) {
                        method.invoke(null, replacementState, Modifier, content, composer, mask, 0)
                    }
                }
            }
            frameRecomposer.performFrame(5L)
            scene.measureAndLayout()
        } finally {
            scene.close()
            frameRecomposer.close()
        }
    }
}
