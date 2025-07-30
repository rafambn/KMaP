package com.rafambn.gziputils

/**
 * Decompresses gzip-compressed data.
 *
 * @param compressedBytes The gzip-compressed byte array
 * @return The decompressed byte array, or the original array if not gzip-compressed
 */
expect fun decompressGzip(compressedBytes: ByteArray): ByteArray

/**
 * Compresses data using gzip compression.
 *
 * @param data The byte array to compress
 * @return The gzip-compressed byte array
 */
expect fun compressGzip(data: ByteArray): ByteArray
