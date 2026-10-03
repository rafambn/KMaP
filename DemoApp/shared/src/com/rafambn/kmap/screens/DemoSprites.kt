package com.rafambn.kmap.screens

import com.rafambn.kmap.style.SpriteImage
import com.rafambn.kmap.style.decodeSpriteAtlas
import kmap.kmapdemo.generated.resources.Res
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.decodeToImageBitmap

private val spriteFiles = mapOf(
    "https://tiles.openfreemap.org/sprites/ofm_f384/ofm" to "sprite-open-free-dark",
    "https://protomaps.github.io/basemaps-assets/sprites/v4/light" to "sprite-proto-maps-light",
    "https://api.maptiler.com/sprites/019d2471-bb09-76a3-8579-65d9425a91f6/sprite" to "sprite-map-tiler-osm",
    "https://api.maptiler.com/sprites/general/sprite" to "sprite-map-tiler-streets-general",
    "https://api.maptiler.com/sprites/misc/sprite" to "sprite-map-tiler-streets-misc",
    "https://api.maptiler.com/sprites/transportation/sprite" to "sprite-map-tiler-streets-transportation"
)

internal suspend fun loadDemoSprites(styleJson: String): Map<String, SpriteImage> {
    val sprite = Json.parseToJsonElement(styleJson).jsonObject["sprite"] ?: return emptyMap()
    val sources = when (sprite) {
        is JsonPrimitive -> listOf("" to sprite.content)
        is JsonArray -> sprite.map { source ->
            val fields = source.jsonObject
            val id = fields.getValue("id").jsonPrimitive.content
            val url = fields.getValue("url").jsonPrimitive.content
            (if (id == "default") "" else "$id:") to url
        }
        else -> error("Invalid sprite configuration")
    }
    return sources.flatMap { (prefix, url) ->
        val name = spriteFiles[url] ?: error("No packaged sprite for $url")
        val index = Res.readBytes("files/$name.json").decodeToString()
        val atlas = Res.readBytes("drawable/$name.png").decodeToImageBitmap()
        decodeSpriteAtlas(index, atlas, prefix).entries
    }.associate { it.key to it.value }
}
