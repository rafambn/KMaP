package com.rafambn.kmap.components.internal

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Constraints
import com.rafambn.kmap.camera.CameraState
import com.rafambn.kmap.components.DrawPosition
import com.rafambn.kmap.components.parameters.ClusterParameters
import com.rafambn.kmap.components.parameters.MarkerParameters
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.geometry.plane.Coordinates
import com.rafambn.kmap.geometry.plane.TilePoint
import com.rafambn.kmap.mapProperties.border.TileRepeatMode
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val ComponentMeasurePolicyTest by testSuite {
    test("markers measure without viewport constraints and keep their draw anchor") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                marker(MarkerParameters(Coordinates(50.0, 50.0), drawPosition = DrawPosition.CENTER_BOTTOM)) {
                    scene.item("marker", 140, 20)
                }
            }
            assertEquals(Constraints(), scene.measured["marker"])
            assertEquals(setOf("marker"), scene.placed)
            assertBounds(Rect(-20F, 30F, 120F, 50F), scene.bounds.getValue("marker"))
        }
    }

    for ((name, transform) in listOf(
        "left" to Triple(-5.0, 50.0, -90.0),
        "right" to Triple(105.0, 50.0, 90.0),
        "top" to Triple(50.0, -5.0, 90.0),
        "bottom" to Triple(50.0, 105.0, -90.0),
    )) {
        test("rotation keeps a marker visible at the $name edge") {
            ComponentMeasureScene().use { scene ->
                val (x, y, rotation) = transform
                scene.render {
                    marker(MarkerParameters(scene.coordinates(x, y), rotation = Degrees(rotation))) {
                        scene.item("marker")
                    }
                }
                assertEquals(setOf("marker"), scene.placed)
            }
        }
    }

    test("rotation culls a marker whose original bounds overlap the viewport") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                marker(MarkerParameters(Coordinates(-5.0, 50.0), rotation = Degrees(90.0))) { scene.item("marker") }
            }
            assertTrue(scene.placed.isEmpty())
        }
    }

    for (rotateWithMap in listOf(false, true)) {
        test("visibility and placement use the same rotation with rotateWithMap=$rotateWithMap") {
            val camera = CameraState(tilePoint = TilePoint(50.0, 50.0), angleDegrees = Degrees(60.0))
            ComponentMeasureScene(camera).use { scene ->
                scene.render {
                    marker(MarkerParameters(
                        scene.coordinates(105.0, 50.0),
                        rotation = Degrees(30.0),
                        rotateWithMap = rotateWithMap,
                    )) { scene.item("marker") }
                }
                assertEquals(if (rotateWithMap) setOf("marker") else emptySet(), scene.placed)
                if (rotateWithMap) assertBounds(Rect(95F, 50F, 105F, 70F), scene.bounds.getValue("marker"))
            }
        }
    }

    test("rotation uses an asymmetric draw position as its pivot") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                marker(MarkerParameters(Coordinates(50.0, 50.0), drawPosition = DrawPosition.BOTTOM_RIGHT, rotation = Degrees(90.0))) {
                    scene.item("marker", 30, 10)
                }
            }
            assertBounds(Rect(50F, 20F, 60F, 50F), scene.bounds.getValue("marker"))
        }
    }

    test("camera rotation remeasures visibility without replacing the content") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                marker(MarkerParameters(Coordinates(105.0, 50.0))) { scene.item("marker") }
            }
            assertTrue(scene.placed.isEmpty())

            scene.mapState.updateCamera(angle = Degrees(-90.0))
            scene.advanceFrame()
            assertEquals(setOf("marker"), scene.placed)
            assertBounds(Rect(50F, -5F, 70F, 5F), scene.bounds.getValue("marker"))

            scene.mapState.updateCamera(angle = Degrees.Zero)
            scene.advanceFrame()
            assertTrue(scene.placed.isEmpty())
        }
    }

    for (zoom in listOf(0F, 2F)) {
        test("zoomToFix changes visibility and placement at zoom $zoom") {
            ComponentMeasureScene(CameraState(zoom = zoom, tilePoint = TilePoint(50.0, 50.0))).use { scene ->
                scene.render {
                    marker(MarkerParameters(scene.coordinates(-15.0, 50.0), zoomToFix = 1F)) { scene.item("marker") }
                }
                assertEquals(if (zoom == 2F) setOf("marker") else emptySet(), scene.placed)
                if (zoom == 2F) assertBounds(Rect(-15F, 50F, 25F, 70F), scene.bounds.getValue("marker"))
            }
        }
    }

    for (zoom in listOf(0F, 1F, 2F, 3F)) {
        test("zoom visibility includes both endpoints at zoom $zoom") {
            ComponentMeasureScene(CameraState(zoom = zoom, tilePoint = TilePoint(50.0, 50.0))).use { scene ->
                scene.render {
                    marker(MarkerParameters(Coordinates(50.0, 50.0), zoomVisibilityRange = 1F..2F)) { scene.item("marker") }
                }
                assertEquals(if (zoom in 1F..2F) setOf("marker") else emptySet(), scene.placed)
            }
        }
    }

    test("rotation and zoom scaling share an asymmetric draw anchor") {
        ComponentMeasureScene(CameraState(zoom = 2F, tilePoint = TilePoint(50.0, 50.0))).use { scene ->
            scene.render {
                marker(MarkerParameters(
                    scene.coordinates(-12.0, 50.0),
                    drawPosition = DrawPosition.BOTTOM_RIGHT,
                    rotation = Degrees(90.0),
                    zoomToFix = 1F,
                )) { scene.item("marker", 30, 10) }
            }
            assertEquals(setOf("marker"), scene.placed)
            assertBounds(Rect(-12F, -10F, 8F, 50F), scene.bounds.getValue("marker"))
        }
    }

    test("density and camera rotation transform the marker coordinates") {
        ComponentMeasureScene(CameraState(angleDegrees = Degrees(90.0), tilePoint = TilePoint(50.0, 50.0)), density = 2F).use { scene ->
            scene.render {
                marker(MarkerParameters(Coordinates(60.0, 50.0))) { scene.item("marker") }
            }
            assertBounds(Rect(50F, 70F, 70F, 80F), scene.bounds.getValue("marker"))
        }
    }

    test("markers use the nearest repeated map copy at the seam") {
        ComponentMeasureScene(CameraState(tilePoint = TilePoint(98.0, 50.0)), repeatMode = TileRepeatMode.REPEAT).use { scene ->
            scene.render { marker(MarkerParameters(Coordinates(2.0, 50.0))) { scene.item("marker") } }
            assertBounds(Rect(54F, 50F, 74F, 60F), scene.bounds.getValue("marker"))
        }
    }

    test("multiple rotated roots use the union of their actual bounds") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                marker(MarkerParameters(Coordinates(-40.0, 50.0), drawPosition = DrawPosition.CENTER, rotation = Degrees(45.0))) {
                    scene.item("wide", 80, 10)
                    scene.item("tall", 10, 80)
                }
            }
            assertEquals(setOf("wide", "tall"), scene.measured.keys)
            assertTrue(scene.placed.isEmpty())
        }
    }

    test("empty marker content does not fail measurement") {
        ComponentMeasureScene().use { scene ->
            scene.render { marker(MarkerParameters(Coordinates(50.0, 50.0))) {} }
            assertTrue(scene.placed.isEmpty())
        }
    }

    test("zero size markers do not create clusters") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                marker(MarkerParameters(Coordinates(40.0, 40.0), clusterId = 1)) { scene.item("visible") }
                marker(MarkerParameters(Coordinates(45.0, 45.0), clusterId = 1)) { scene.item("empty", 0, 0) }
                cluster(ClusterParameters(1)) { scene.item("cluster") }
            }
            assertEquals(setOf("visible"), scene.placed)
        }
    }

    for (order in listOf(listOf(0, 1, 2), listOf(2, 0, 1), listOf(1, 2, 0))) {
        test("overlap chains form one cluster in declaration order $order") {
            ComponentMeasureScene().use { scene ->
                scene.render {
                    order.forEach { index ->
                        marker(MarkerParameters(Coordinates(20.0 + index * 15.0, 40.0), clusterId = 1)) { scene.item("marker$index") }
                    }
                    cluster(ClusterParameters(1)) { scene.item("cluster") }
                }
                assertEquals(setOf("cluster"), scene.placed)
                assertBounds(Rect(35F, 40F, 55F, 50F), scene.bounds.getValue("cluster"))
            }
        }
    }

    test("clusters use rotated marker bounds") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                for (x in listOf(30.0, 45.0)) {
                    marker(MarkerParameters(Coordinates(x, 50.0), drawPosition = DrawPosition.CENTER, rotation = Degrees(90.0), clusterId = 1)) {
                        scene.item("marker$x", 10, 30)
                    }
                }
                cluster(ClusterParameters(1)) { scene.item("cluster") }
            }
            assertEquals(setOf("cluster"), scene.placed)
            assertBounds(Rect(22.5F, 45F, 42.5F, 55F), scene.bounds.getValue("cluster"))
        }
    }

    test("fully overlapping markers are counted once in the cluster position") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                for (x in listOf(30.0, 35.0, 40.0)) {
                    marker(MarkerParameters(Coordinates(x, 50.0), clusterId = 1)) { scene.item("marker$x") }
                }
                cluster(ClusterParameters(1)) { scene.item("cluster") }
            }
            assertEquals(setOf("cluster"), scene.placed)
            assertBounds(Rect(35F, 50F, 55F, 60F), scene.bounds.getValue("cluster"))
        }
    }

    test("markers touching at an edge do not cluster") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                for (x in listOf(20.0, 40.0)) {
                    marker(MarkerParameters(Coordinates(x, 50.0), clusterId = 1)) { scene.item("marker$x") }
                }
                cluster(ClusterParameters(1)) { scene.item("cluster") }
            }
            assertEquals(setOf("marker20.0", "marker40.0"), scene.placed)
        }
    }

    for (rotateWithMap in listOf(false, true)) {
        test("clusters apply their own rotation with rotateWithMap=$rotateWithMap") {
            ComponentMeasureScene(CameraState(angleDegrees = Degrees(90.0), tilePoint = TilePoint(50.0, 50.0))).use { scene ->
                scene.render {
                    for (x in listOf(40.0, 50.0)) {
                        marker(MarkerParameters(scene.coordinates(x, 50.0), clusterId = 1)) { scene.item("marker$x") }
                    }
                    cluster(ClusterParameters(1, rotation = Degrees(90.0), rotateWithMap = rotateWithMap)) { scene.item("cluster") }
                }
                assertEquals(setOf("cluster"), scene.placed)
                val expected = if (rotateWithMap) Rect(45F, 50F, 65F, 60F) else Rect(50F, 45F, 60F, 65F)
                assertBounds(expected, scene.bounds.getValue("cluster"))
            }
        }
    }

    test("clusters use marker bounds scaled by zoomToFix") {
        ComponentMeasureScene(CameraState(zoom = 2F, tilePoint = TilePoint(50.0, 50.0))).use { scene ->
            scene.render {
                for (x in listOf(20.0, 50.0)) {
                    marker(MarkerParameters(scene.coordinates(x, 50.0), zoomToFix = 1F, clusterId = 1)) { scene.item("marker$x") }
                }
                cluster(ClusterParameters(1)) { scene.item("cluster") }
            }
            assertEquals(setOf("cluster"), scene.placed)
        }
    }

    test("zoom changes split and rebuild clusters without replacing the content") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                for (x in listOf(40.0, 55.0)) {
                    marker(MarkerParameters(Coordinates(x, 50.0), clusterId = 1)) { scene.item("marker$x") }
                }
                cluster(ClusterParameters(1)) { scene.item("cluster") }
            }
            assertEquals(setOf("cluster"), scene.placed)

            scene.mapState.updateCamera(zoom = 1F)
            scene.advanceFrame()
            assertEquals(setOf("marker40.0", "marker55.0"), scene.placed)

            scene.mapState.updateCamera(zoom = 0F)
            scene.advanceFrame()
            assertEquals(setOf("cluster"), scene.placed)
        }
    }

    test("different cluster ids and unclustered markers stay independent") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                for (id in listOf(null, 1, 2)) {
                    marker(MarkerParameters(Coordinates(50.0, 50.0), clusterId = id)) { scene.item("marker$id") }
                }
                for (id in listOf(1, 2)) cluster(ClusterParameters(id)) { scene.item("cluster$id") }
            }
            assertEquals(setOf("markernull", "marker1", "marker2"), scene.placed)
            assertEquals(scene.placed, scene.measured.keys)
        }
    }

    test("hidden markers cannot join a visible cluster") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                marker(MarkerParameters(Coordinates(95.0, 50.0), clusterId = 1)) { scene.item("visible") }
                marker(MarkerParameters(Coordinates(105.0, 50.0), clusterId = 1)) { scene.item("outside") }
                marker(MarkerParameters(Coordinates(95.0, 50.0), zoomVisibilityRange = 1F..2F, clusterId = 1)) { scene.item("zoomHidden") }
                cluster(ClusterParameters(1)) { scene.item("cluster") }
            }
            assertEquals(setOf("visible"), scene.placed)
            assertTrue("cluster" !in scene.measured)
        }
    }

    test("disconnected groups measure separate cluster instances") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                for (x in listOf(10.0, 15.0, 70.0, 75.0)) {
                    marker(MarkerParameters(Coordinates(x, 50.0), clusterId = 1)) { scene.item("marker$x") }
                }
                var clusterIndex = 0
                cluster(ClusterParameters(1)) { scene.item("cluster${clusterIndex++}") }
            }
            assertEquals(2, scene.placed.size)
            assertEquals(listOf(12.5F, 72.5F), scene.bounds.values.map { it.left }.sorted())
        }
    }

    test("empty cluster content does not fail measurement") {
        ComponentMeasureScene().use { scene ->
            scene.render {
                repeat(2) { index ->
                    marker(MarkerParameters(Coordinates(50.0, 50.0), clusterId = 1)) { scene.item("marker$index") }
                }
                cluster(ClusterParameters(1)) {}
            }
            assertTrue(scene.placed.isEmpty())
        }
    }
}

private fun assertBounds(expected: Rect, actual: Rect) {
    assertEquals(expected.left, actual.left, 0.001F, "left")
    assertEquals(expected.top, actual.top, 0.001F, "top")
    assertEquals(expected.right, actual.right, 0.001F, "right")
    assertEquals(expected.bottom, actual.bottom, 0.001F, "bottom")
}
