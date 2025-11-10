package com.rafambn.kmap.customSources

import com.rafambn.kmap.mapSource.tiled.TileResult
import com.rafambn.kmap.mapSource.tiled.TileSource
import com.rafambn.kmap.mapSource.tiled.tiles.TileSpecs
import com.rafambn.kmap.mapSource.tiled.tiles.VectorTile
import com.rafambn.kmap.utils.vectorTile.RawMVTile
import com.rafambn.kmap.utils.vectorTile.parse
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes

class VectorTileSource : TileSource<VectorTile> {
    private val client = HttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        useArrayPolymorphism = false
    }

    @OptIn(ExperimentalResourceApi::class, InternalResourceApi::class, ExperimentalUnsignedTypes::class, ExperimentalSerializationApi::class)
    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileResult<VectorTile> {
        try {
//            val compressedBytes = client.get("https://vtiles.openhistoricalmap.org/maps/osm/$zoom/$row/$column") {
//                contentType(ContentType.Application.ProtoBuf)
//            }.readRawBytes()
//            val rawMVTile = ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), compressedBytes)
//            val mvTile = rawMVTile.parse()

//            val compressedBytes = client.get("https://tiles.versatiles.org/tiles/osm/$zoom/$row/$column") {
//                contentType(ContentType.Application.ProtoBuf)
//            }.readRawBytes()
//            val rawMVTile = ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), compressedBytes)
//            val mvTile = rawMVTile.parse()

            val compressedBytes = client.get("https://api.maptiler.com/tiles/v4/$zoom/$column/$row.pbf?key=GCqxEKWuBP1S6iQ1aSBG") {
                contentType(ContentType.Application.ProtoBuf)
            }.readRawBytes()
            val rawMVTile = ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), compressedBytes)
//            rawMVTile = json.decodeFromString(RawMVTile.serializer(), readResourceBytes("000.json").decodeToString())
            val mvTile = rawMVTile.parse()
//            if (zoom == 0 && row == 0 && column == 0){
//                val jsonTile = Json.encodeToString(RawMVTile.serializer(),ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), compressedBytes))
//                println(jsonTile)
//            }
//            val jsonTile = Json.encodeToString(RawMVTile.serializer(),rawMVTile)
//            println(jsonTile)
            return TileResult.Success(VectorTile(zoom, row, column, mvTile))
        } catch (ex: Exception) {
            println(ex)
            println(ex.cause)
            println("Tile $zoom/$row/$column failed")
        }
        return TileResult.Failure(TileSpecs(zoom, row, column))
    }
}
