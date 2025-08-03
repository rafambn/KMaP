@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals

class KFlateTest {

    // Test resources
    private val testFiles = listOf(
        "Maltese.bmp",
        "moby10b.txt",
        "Rainier.bmp",
        "Sunrise.bmp"
    )

    // Test file sizes for verification
    private val expectedFileSizes = mapOf(
        "Maltese.bmp" to 16427390,
        "moby10b.txt" to 1256164,
        "Rainier.bmp" to 6220854,
        "Sunrise.bmp" to 52344054
    )

    // Helper function to read resource files
    private fun readResourceFile(fileName: String): ByteArray {
        return javaClass.classLoader.getResourceAsStream(fileName)?.readBytes()
            ?: throw IllegalArgumentException("Resource file not found: $fileName")
    }

    // Helper function to convert ByteArray to UByteArray
    private fun ByteArray.toUByteArray(): UByteArray {
        return UByteArray(this.size) { this[it].toUByte() }
    }

    // Helper function to convert UByteArray to ByteArray
    private fun UByteArray.toByteArray(): ByteArray {
        return ByteArray(this.size) { this[it].toByte() }
    }

    // RESOURCE TESTS

    @Test
    fun testResourceFilesExist() {
        for (fileName in testFiles) {
            val fileData = readResourceFile(fileName)
            val expectedSize = expectedFileSizes[fileName]

            // Verify file was loaded and has the expected size
            assert(fileData.isNotEmpty()) { "File $fileName could not be loaded or is empty" }
            assert(fileData.size == expectedSize) {
                "File $fileName has size ${fileData.size} but expected $expectedSize"
            }
        }
    }

    // FLATE TESTS

    @Test
    fun testFlateCompressDecompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            // Compress with KFlate
            val compressedData = KFlate.Flate.deflate(originalData.toUByteArray(), DeflateOptions())

            // Decompress with Java's Inflater
            val inflater = Inflater()
            val inputStream = ByteArrayInputStream(compressedData.toByteArray())
            val inflaterStream = InflaterInputStream(inputStream, inflater)
            val outputStream = ByteArrayOutputStream()

            inflaterStream.copyTo(outputStream)
            val decompressedData = outputStream.toByteArray()

            // Verify the data matches
            assertContentEquals(originalData, decompressedData, "Failed on file: $fileName")

            // Clean up
            inflater.end()
        }
    }

    @Test
    fun testFlateDecompressCompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            // Compress with Java's Deflater
            val deflater = Deflater()
            val outputStream = ByteArrayOutputStream()
            val deflaterStream = DeflaterOutputStream(outputStream, deflater)

            deflaterStream.write(originalData)
            deflaterStream.finish()
            deflaterStream.close()

            val compressedData = outputStream.toByteArray()

            // Decompress with KFlate
            val decompressedData = KFlate.Flate.inflate(compressedData.toUByteArray(), InflateOptions())

            // Verify the data matches
            assertContentEquals(originalData, decompressedData.toByteArray(), "Failed on file: $fileName")

            // Clean up
            deflater.end()
        }
    }

    // GZIP TESTS

    @Test
    fun testGzipCompressDecompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            // Compress with KFlate's gzip
            val compressedData = KFlate.Gzip.compress(originalData.toUByteArray(), GzipOptions())

            // Decompress with Java's GZIPInputStream
            val inputStream = ByteArrayInputStream(compressedData.toByteArray())
            val gzipInputStream = GZIPInputStream(inputStream)
            val outputStream = ByteArrayOutputStream()

            gzipInputStream.copyTo(outputStream)
            val decompressedData = outputStream.toByteArray()

            // Verify the data matches
            assertContentEquals(originalData, decompressedData, "Failed on file: $fileName")
        }
    }

    @Test
    fun testGzipDecompressCompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            // Compress with Java's GZIPOutputStream
            val outputStream = ByteArrayOutputStream()
            val gzipOutputStream = GZIPOutputStream(outputStream)

            gzipOutputStream.write(originalData)
            gzipOutputStream.finish()
            gzipOutputStream.close()

            val compressedData = outputStream.toByteArray()

            // Decompress with KFlate's gzip
            val decompressedData = KFlate.Gzip.decompress(compressedData.toUByteArray(), DeflateOptions())

            // Verify the data matches
            assertContentEquals(originalData, decompressedData.toByteArray(), "Failed on file: $fileName")
        }
    }

    // ZLIB TESTS

    @Test
    fun testZlibCompressDecompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            // Compress with KFlate's zlib
            val compressedData = KFlate.Zlib.compress(originalData.toUByteArray(), DeflateOptions())

            // Decompress with Java's Inflater (with ZLIB header)
            val inflater = Inflater(true) // true = with ZLIB header
            val inputStream = ByteArrayInputStream(compressedData.toByteArray())
            val inflaterStream = InflaterInputStream(inputStream, inflater)
            val outputStream = ByteArrayOutputStream()

            inflaterStream.copyTo(outputStream)
            val decompressedData = outputStream.toByteArray()

            // Verify the data matches
            assertContentEquals(originalData, decompressedData, "Failed on file: $fileName")

            // Clean up
            inflater.end()
        }
    }

    @Test
    fun testZlibDecompressCompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            // Compress with Java's Deflater (with ZLIB header)
            val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true) // true = with ZLIB header
            val outputStream = ByteArrayOutputStream()
            val deflaterStream = DeflaterOutputStream(outputStream, deflater)

            deflaterStream.write(originalData)
            deflaterStream.finish()
            deflaterStream.close()

            val compressedData = outputStream.toByteArray()

            // Decompress with KFlate's zlib
            val decompressedData = KFlate.Zlib.decompress(compressedData.toUByteArray(), DeflateOptions())

            // Verify the data matches
            assertContentEquals(originalData, decompressedData.toByteArray(), "Failed on file: $fileName")

            // Clean up
            deflater.end()
        }
    }
}
