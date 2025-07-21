package com.rafambn.mvtparser

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

actual fun decompressGzip(compressedBytes: ByteArray): ByteArray {
    if (compressedBytes.isEmpty()) return compressedBytes
    if (compressedBytes[0].toUByte().toUInt() != 0x1Fu || compressedBytes[1].toUByte().toUInt() != 0x8Bu) {
        return compressedBytes
    }

    ByteArrayInputStream(compressedBytes).use { bis ->
        GZIPInputStream(bis).use { gis ->
            return gis.readBytes()
        }
    }
}
