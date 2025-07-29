package com.rafambn.kmap.customSources

import com.rafambn.kmap.tiles.TileRenderResult
import com.rafambn.kmap.tiles.TileSource
import com.rafambn.kmap.tiles.TileSpecs
import com.rafambn.mvtparser.deserializeMVT
import com.rafambn.mvtparser.parseMVT
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.jetbrains.compose.resources.ExperimentalResourceApi

class VectorTileSource : TileSource {
    private val client = HttpClient()

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileRenderResult {
        try {
//            val compressedBytes = client.get("https://vtiles.openhistoricalmap.org/maps/osm/$zoom/$row/$column") {
//                contentType(ContentType.Application.ProtoBuf)
//            }.readRawBytes()

            val compressedBytes = client.get("https://api.maptiler.com/tiles/v3/$zoom/$row/$column.pbf?key=") {
                contentType(ContentType.Application.ProtoBuf)
            }.readRawBytes()

//            val compressedBytes = client.get("https://tiles.versatiles.org/tiles/osm/$zoom/$row/$column") {
//                contentType(ContentType.Application.ProtoBuf)
//            }.readRawBytes()

            val mvtTile = deserializeMVT(compressedBytes)

            val parsedMVTile = parseMVT(mvtTile)

            println("Parsed tile with ${parsedMVTile.layers.size} layers")
            parsedMVTile.layers.forEach { layer ->
                println("Layer: ${layer.name} with ${layer.features.size} features")
            }

        } catch (ex: Exception) {
            println(ex)
        }
        return TileRenderResult.Failure(TileSpecs(zoom, row, column))
    }
}
