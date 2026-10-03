package com.rafambn.kmap.style

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Cuts the images described by a sprite index out of its matching PNG atlas. */
fun decodeSpriteAtlas(indexJson: String, atlas: ImageBitmap, prefix: String = ""): Map<String, SpriteImage> {
    val index = Json.parseToJsonElement(indexJson).jsonObject
    return index.mapValues { (_, value) ->
        val entry = value as? JsonObject ?: error("Invalid sprite entry")
        val x = entry.getValue("x").jsonPrimitive.intOrNull ?: error("Invalid sprite x")
        val y = entry.getValue("y").jsonPrimitive.intOrNull ?: error("Invalid sprite y")
        val width = entry.getValue("width").jsonPrimitive.intOrNull ?: error("Invalid sprite width")
        val height = entry.getValue("height").jsonPrimitive.intOrNull ?: error("Invalid sprite height")
        val pixelRatio = entry.getValue("pixelRatio").jsonPrimitive.doubleOrNull ?: error("Invalid sprite pixelRatio")
        require(x >= 0 && y >= 0 && width > 0 && height > 0 &&
            x.toLong() + width <= atlas.width && y.toLong() + height <= atlas.height) {
            "Sprite image is outside its atlas"
        }
        val image = ImageBitmap(width, height)
        Canvas(image).drawImageRect(
            image = atlas,
            srcOffset = IntOffset(x, y),
            srcSize = IntSize(width, height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, height),
            paint = Paint()
        )
        SpriteImage(image, pixelRatio, entry["sdf"]?.jsonPrimitive?.booleanOrNull == true)
    }.mapKeys { (name, _) -> "$prefix$name" }
}
