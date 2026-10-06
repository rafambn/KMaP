package com.rafambn.kmap.source

/**
 * Uncompressed MVT protobuf bytes. Fetching this tile does not decode its geometry.
 * The source transfers ownership of [bytes] to the map and must not mutate them after returning.
 * Worker preparation requires a style produced by StyleResolver, or an explicit preparation definition.
 */
class EncodedVectorTile(
    zoom: Int,
    row: Int,
    col: Int,
    val bytes: ByteArray,
) : VectorTile(zoom, row, col, null) {
    override fun withSpecs(newSpecs: TileSpecs) = EncodedVectorTile(newSpecs.zoom, newSpecs.row, newSpecs.col, bytes)
}
