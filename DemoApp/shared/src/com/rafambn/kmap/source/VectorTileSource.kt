package com.rafambn.kmap.source

import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.TileSource
import com.rafambn.kmap.source.TileSpecs
import com.rafambn.kmap.source.VectorTile
import com.rafambn.kmap.mvttile.RawMVTile
import com.rafambn.kmap.mvttile.parse
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf

class VectorTileSource : TileSource<VectorTile> {
    private val client = HttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        useArrayPolymorphism = false
    }

    @OptIn(ExperimentalSerializationApi::class)
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
                accept(ContentType.Application.ProtoBuf)
            }.readRawBytes()
            val rawMVTile = ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), compressedBytes)
//            val rawMVTile = json.decodeFromString(RawMVTile.serializer(), Res.readBytes("files/000.json").decodeToString())
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
