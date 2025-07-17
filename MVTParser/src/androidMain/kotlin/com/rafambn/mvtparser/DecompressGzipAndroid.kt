package com.rafambn.mvtparser

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

/**
 * Android implementation of decompressGzip
 */
actual fun decompressGzip(compressedBytes: ByteArray): ByteArray {
    if (compressedBytes.isEmpty()) return compressedBytes
    // A simple heuristic for GZIP magic number (0x1F, 0x8B)
    if (compressedBytes[0].toUByte().toUInt() != 0x1Fu || compressedBytes[1].toUByte().toUInt() != 0x8Bu) {
        // Not GZIP, return as is (or throw an error if you strictly expect GZIP)
        return compressedBytes
    }

    ByteArrayInputStream(compressedBytes).use { bis ->
        GZIPInputStream(bis).use { gis ->
            return gis.readBytes()
        }
    }
}
