package com.rafambn.gziputils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

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

actual fun compressGzip(data: ByteArray): ByteArray {
    if (data.isEmpty()) return data

    ByteArrayOutputStream().use { baos ->
        GZIPOutputStream(baos).use { gos ->
            gos.write(data)
        }
        return baos.toByteArray()
    }
}
