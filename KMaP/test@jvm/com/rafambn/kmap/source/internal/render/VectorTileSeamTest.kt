package com.rafambn.kmap.source.internal.render

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.geometry.plane.CanvasDrawReference
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mvttile.MVTFeature
import com.rafambn.kmap.mvttile.MVTLayer
import com.rafambn.kmap.mvttile.MVTile
import com.rafambn.kmap.mvttile.RawMVTGeomType
import com.rafambn.kmap.source.VectorTile
import com.rafambn.kmap.source.internal.optimizeMVTile
import com.rafambn.kmap.style.StyleResolver
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals

val VectorTileSeamTest by testSuite {
    for (opacity in listOf(1f, 0.5f)) {
        test("adjacent zoom 2 tiles have no gaps or overlap at opacity $opacity") {
            val style = StyleResolver().resolve("""{
                "layers": [{"id": "water", "type": "fill", "source-layer": "water",
                    "paint": {"fill-color": "#0000ff", "fill-opacity": $opacity}}]
            }""").style!!
            // MVT geometry extends past the tile, so only tile clipping divides this solid fill.
            val water = MVTFeature(1UL, RawMVTGeomType.POLYGON, listOf(listOf(
                -128 to -128, 4224 to -128, 4224 to 4224, -128 to 4224
            )), emptyMap())
            val tiles = (1..2).flatMap { row ->
                (1..2).map { col ->
                    optimizeMVTile(VectorTile(2, row, col,
                        MVTile(listOf(MVTLayer("water", 4096, listOf(water))))), style)
                }
            }
            val fontResolver = createFontFamilyResolver()

            for (displayDensity in listOf(1f, 2f)) {
                for (screenScale in listOf(1f, 1.3f)) {
                    for (scaleAdjustment in listOf(1f, 2f)) {
                        for (rotation in listOf(0f, 30f)) {
                            val bitmap = ImageBitmap(128, 128)
                            val canvas = Canvas(bitmap)
                            canvas.translate(64.25f, 64.25f)
                            canvas.rotate(rotation)
                            canvas.scale(screenScale, screenScale)
                            val density = Density(displayDensity)
                            val offset = -1024.0 * displayDensity * scaleAdjustment
                            CanvasDrawScope().draw(density, LayoutDirection.Ltr, canvas, Size(128f, 128f)) {
                                tiles.forEach { tile ->
                                    drawVectorTileLayerWithClipping(
                                        tile, style.layers.single(), emptyMap(), TileDimension(512.dp, 512.dp),
                                        CanvasDrawReference(offset, offset), scaleAdjustment, canvas, fontResolver,
                                        density, 2.0, rotation, screenScale
                                    )
                                }
                            }
                            val pixels = bitmap.toPixelMap()
                            for (y in 0 until 128) for (x in 0 until 128) {
                                assertEquals(opacity, pixels[x, y].alpha, 0.01f,
                                    "Pixel ($x, $y), density=$displayDensity, scale=$screenScale, " +
                                        "fallback=$scaleAdjustment, rotation=$rotation")
                            }
                        }
                    }
                }
            }
        }
    }
}
