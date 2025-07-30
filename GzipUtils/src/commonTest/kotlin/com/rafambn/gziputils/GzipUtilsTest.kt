package com.rafambn.gziputils

import kotlin.test.*

class GzipUtilsTest {

    @Test
    fun `compress and decompress empty data`() {
        val emptyData = byteArrayOf()
        val compressed = compressGzip(emptyData)
        val decompressed = decompressGzip(compressed)
        assertContentEquals(emptyData, decompressed, "Decompressed empty data should be empty")
    }

    @Test
    fun `compress and decompress various data types`() {
        val testCases = mapOf(
            "simple text" to "Hello, World!".encodeToByteArray(),
            "large data" to ByteArray(10000) { (it % 256).toByte() },
            "random data" to ByteArray(1000) { kotlin.random.Random.nextInt(256).toByte() },
            "repetitive data" to "A".repeat(1000).encodeToByteArray(),
            "binary data" to ByteArray(256) { it.toByte() },
            "unicode text" to "Hello 世界! 🌍 Здравствуй мир! مرحبا بالعالم!".encodeToByteArray()
        )

        for ((caseName, originalData) in testCases) {
            val compressed = compressGzip(originalData)
            val decompressed = decompressGzip(compressed)
            assertContentEquals(originalData, decompressed, "Failed at case: $caseName")
        }
    }

    @Test
    fun `decompressing non-gzip data returns original data`() {
        val nonGzipData = "this is not gzip data".encodeToByteArray()
        val result = decompressGzip(nonGzipData)
        assertContentEquals(nonGzipData, result, "Decompressing non-gzip data should return the original array")
    }

    @Test
    fun `compression is efficient for compressible data`() {
        val repetitiveData = "AAAAAAAAAA".repeat(1000).encodeToByteArray()
        val compressed = compressGzip(repetitiveData)
        assertTrue(compressed.size < repetitiveData.size, "Compressed size should be smaller for repetitive data")
    }

    @Test
    fun `multiple compression-decompression cycles are consistent`() {
        val originalData = "Test data for multiple cycles".encodeToByteArray()
        var currentData = originalData

        repeat(5) {
            val compressed = compressGzip(currentData)
            currentData = decompressGzip(compressed)
        }
        assertContentEquals(originalData, currentData, "Data should remain consistent after multiple cycles")
    }

    @Test
    fun `compressed data has correct gzip magic bytes`() {
        val testData = "Test data".encodeToByteArray()
        val compressed = compressGzip(testData)

        assertTrue(compressed.size >= 2, "Compressed data should be at least 2 bytes long")
        assertEquals(0x1F, compressed[0].toInt() and 0xFF, "First magic byte should be 0x1F")
        assertEquals(0x8B, compressed[1].toInt() and 0xFF, "Second magic byte should be 0x8B")
    }
}