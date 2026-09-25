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

val VectorCanvasEngineTest by testSuite {
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
            Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(StyleLayer(id = "land-fill", type = "fill", sourceLayer = "land"))
            )
        )

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
            Style(
                version = 8,
                sources = emptyMap(),
                layers = listOf(StyleLayer(id = "land-outline", type = "line", sourceLayer = "land"))
            )
        )

        val geometry = optimizeMVTile(tile, style).optimizedTile!!
            .layerFeatures.getValue("land-outline").single().geometry
        assertTrue(geometry is OptimizedGeometry.Polygon)
    }
}
