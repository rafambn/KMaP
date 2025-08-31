package com.rafambn.kmap.customSources

import com.rafambn.kmap.tiles.TileRenderResult
import com.rafambn.kmap.tiles.TileSource
import com.rafambn.kmap.tiles.TileSpecs
import com.rafambn.kmap.utils.vectorTile.RawMVTile
import com.rafambn.kmap.utils.vectorTile.parse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.InternalResourceApi

class VectorTileSource : TileSource {
    private val client = HttpClient()

    @OptIn(ExperimentalResourceApi::class, InternalResourceApi::class, ExperimentalUnsignedTypes::class, ExperimentalSerializationApi::class)
    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileRenderResult {
        try {
//            val compressedBytes = client.get("https://vtiles.openhistoricalmap.org/maps/osm/$zoom/$row/$column") {
//                contentType(ContentType.Application.ProtoBuf)
//            }.readRawBytes()
//            val rawMVTile = ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), compressedBytes)
//            val mvTile = rawMVTile.parse()

            val compressedBytes = client.get("https://api.maptiler.com/tiles/v3/$zoom/$row/$column.pbf?key=") {
                contentType(ContentType.Application.ProtoBuf)
            }.readRawBytes()
            val rawMVTile = ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), compressedBytes)
            val mvTile = rawMVTile.parse()

//            val compressedBytes = client.get("https://tiles.versatiles.org/tiles/osm/$zoom/$row/$column") {
//                contentType(ContentType.Application.ProtoBuf)
//            }.readRawBytes()
//            val rawMVTile = ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), compressedBytes)
//            val mvTile = rawMVTile.parse()

//            val styleJson = readResourceBytes("style.json").decodeToString()
//            val style = deserialize(styleJson)
//            val decompressedBytes = KFlate.Gzip.decompress(compressedBytes.toUByteArray())
//            println("Parsed tile with ${parsedMVTile.layers.size} layers and style ${style.layers.size}")
//            parsedMVTile.layers.forEach { layer ->
//                println("Layer: ${layer.name} with ${layer.features.size} features")
//            }

        } catch (ex: Exception) {
            println(ex)
        }
        return TileRenderResult.Failure(TileSpecs(zoom, row, column))
    }
}
