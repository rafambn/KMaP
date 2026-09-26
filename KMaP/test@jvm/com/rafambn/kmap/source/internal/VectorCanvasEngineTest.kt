package com.rafambn.kmap.source.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathHitTester
import com.rafambn.kmap.mvttile.MVTFeature
import com.rafambn.kmap.mvttile.MVTLayer
import com.rafambn.kmap.mvttile.MVTile
import com.rafambn.kmap.mvttile.OptimizedGeometry
import com.rafambn.kmap.mvttile.RawMVTGeomType
import com.rafambn.kmap.source.VectorTile
import com.rafambn.kmap.style.Style
import com.rafambn.kmap.style.StyleLayer
import com.rafambn.kmap.style.StyleResolver
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

val VectorCanvasEngineTest by testSuite {
    test("tile processing preserves features outside layer zoom bounds for later drawing") {
        val feature = MVTFeature(
            id = null,
            type = RawMVTGeomType.POLYGON,
            geometry = listOf(listOf(0 to 0, 10 to 0, 10 to 10, 0 to 10)),
            properties = emptyMap()
        )
        val mvtile = MVTile(listOf(MVTLayer("land", 4096, listOf(feature))))
        val style = StyleResolver().resolve(
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(StyleLayer(
                    id = "land-fill",
                    type = "fill",
                    sourceLayer = "land",
                    minzoom = 5.5,
                    maxzoom = 8.5
                ))
            ))
        ).style!!

        assertTrue(optimizeMVTile(VectorTile(5, 0, 0, mvtile), style).optimizedTile!!.layerFeatures.getValue("land-fill").isNotEmpty())
        assertTrue(optimizeMVTile(VectorTile(9, 0, 0, mvtile), style).optimizedTile!!.layerFeatures.getValue("land-fill").isNotEmpty())
    }

    test("filter evaluates zoom from the tile being processed") {
        val feature = MVTFeature(
            id = 7,
            type = RawMVTGeomType.POLYGON,
            geometry = listOf(listOf(0 to 0, 10 to 0, 10 to 10, 0 to 10)),
            properties = mapOf("class" to "park")
        )
        val mvtile = MVTile(listOf(MVTLayer("land", 4096, listOf(feature))))
        val style = StyleResolver().resolve(
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(
                    StyleLayer(
                        id = "parks",
                        type = "fill",
                        sourceLayer = "land",
                        filter = listOf(
                            JsonPrimitive("all"),
                            JsonArray(listOf(JsonPrimitive(">="), JsonArray(listOf(JsonPrimitive("zoom"))), JsonPrimitive(5))),
                            JsonArray(listOf(JsonPrimitive("=="), JsonArray(listOf(JsonPrimitive("get"), JsonPrimitive("class"))), JsonPrimitive("park"))),
                            JsonArray(listOf(JsonPrimitive("=="), JsonArray(listOf(JsonPrimitive("geometry-type"))), JsonPrimitive("Polygon")))
                        )
                    )
                )
            ))
        ).style!!

        assertTrue(optimizeMVTile(VectorTile(4, 0, 0, mvtile), style).optimizedTile!!.layerFeatures.getValue("parks").isEmpty())
        assertTrue(optimizeMVTile(VectorTile(5, 0, 0, mvtile), style).optimizedTile!!.layerFeatures.getValue("parks").size == 1)
    }

    test("polygon interior rings cut holes while separate exterior rings remain filled") {
        val exterior = listOf(0 to 0, 10 to 0, 10 to 10, 0 to 10)
        val interior = listOf(3 to 3, 3 to 7, 7 to 7, 7 to 3)
        val secondExterior = listOf(12 to 12, 20 to 12, 20 to 20, 12 to 20)
        val feature = MVTFeature(
            id = null,
            type = RawMVTGeomType.POLYGON,
            geometry = listOf(exterior, interior, secondExterior),
            properties = emptyMap()
        )
        val tile = VectorTile(
            zoom = 0,
            row = 0,
            col = 0,
            mvtile = MVTile(listOf(MVTLayer("land", 4096, listOf(feature))))
        )
        val style = StyleResolver().resolve(
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(StyleLayer(id = "land-fill", type = "fill", sourceLayer = "land"))
            ))
        ).style!!

        val geometry = optimizeMVTile(tile, style).optimizedTile!!
            .layerFeatures.getValue("land-fill").single().geometry as OptimizedGeometry.Polygon
        val hitTester = PathHitTester(geometry.path, 0f)

        assertTrue(hitTester.contains(Offset(1f, 1f)))
        assertFalse(hitTester.contains(Offset(5f, 5f)))
        assertTrue(hitTester.contains(Offset(15f, 15f)))
        assertFalse(hitTester.contains(Offset(11f, 11f)))
    }

    test("line style keeps polygon geometry for its outer and inner boundaries") {
        val feature = MVTFeature(
            id = null,
            type = RawMVTGeomType.POLYGON,
            geometry = listOf(
                listOf(0 to 0, 20 to 0, 20 to 20, 0 to 20),
                listOf(5 to 5, 5 to 15, 15 to 15, 15 to 5)
            ),
            properties = emptyMap()
        )
        val tile = VectorTile(0, 0, 0, MVTile(listOf(MVTLayer("land", 4096, listOf(feature)))))
        val style = StyleResolver().resolve(
            Json.encodeToString(Style.serializer(), Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(StyleLayer(id = "land-outline", type = "line", sourceLayer = "land"))
            ))
        ).style!!

        val geometry = optimizeMVTile(tile, style).optimizedTile!!
            .layerFeatures.getValue("land-outline").single().geometry
        assertTrue(geometry is OptimizedGeometry.Polygon)
    }
}
