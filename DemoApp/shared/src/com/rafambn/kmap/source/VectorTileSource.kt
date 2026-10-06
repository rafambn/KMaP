package com.rafambn.kmap.source

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException

/** Fetch only. Protobuf, geometry and filters are prepared by the map worker pool. */
class VectorTileSource : TileSource<EncodedVectorTile> {
    private val client = HttpClient()
    private val apiKey = "TRvgTCfAgciROrLkbKNj"

    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileResult<EncodedVectorTile> {
        try {
            val response = client.get("https://api.maptiler.com/tiles/v4/$zoom/$column/$row.pbf") {
                parameter("key", apiKey)
                accept(ContentType.Application.ProtoBuf)
            }
            if (response.status.value !in 200..299) return TileResult.Failure(TileSpecs(zoom, row, column))
            return TileResult.Success(EncodedVectorTile(zoom, row, column, response.readRawBytes()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return TileResult.Failure(TileSpecs(zoom, row, column))
        }
    }

    fun close() { client.close() }
}
