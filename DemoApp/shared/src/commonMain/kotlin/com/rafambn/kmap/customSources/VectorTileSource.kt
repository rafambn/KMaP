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
import kotlinx.serialization.protobuf.ProtoBuf

class VectorTileSource : TileSource<VectorTile> {
    private val client = HttpClient()

    fun close() {
        client.close()
    }

    @OptIn(ExperimentalUnsignedTypes::class, ExperimentalSerializationApi::class)
    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileResult<VectorTile> {
        return try {
            val compressedBytes = client.get("$TILES_URL/$zoom/$column/$row.pbf?key=$API_KEY") {
                accept(ContentType.Application.ProtoBuf)
            }.readRawBytes()
            val rawMVTile = ProtoBuf.decodeFromByteArray(RawMVTile.serializer(), compressedBytes)
            val mvTile = rawMVTile.parse()
            TileResult.Success(VectorTile(zoom, row, column, mvTile))
        } catch (_: Exception) {
            TileResult.Failure(TileSpecs(zoom, row, column))
        }
    }

    private companion object {
        const val API_KEY = "GCqxEKWuBP1S6iQ1aSBG"
        const val TILES_URL = "https://api.maptiler.com/tiles/v4"
    }
}
