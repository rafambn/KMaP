@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate

import com.rafambn.kflate.options.InflateOptions
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.*
import kotlin.test.Test
import kotlin.test.assertContentEquals

class BlockingValidityTest {

    private val testFiles = listOf(
        "model3D",
        "text",
        "Rainier.bmp",
        "Maltese.bmp",
        "Sunrise.bmp",
        "simpleText",
    )

    private val expectedFileSizes = mapOf(
        "Maltese.bmp" to 16427390,
        "text" to 1256167,
        "Rainier.bmp" to 6220854,
        "Sunrise.bmp" to 52344054,
        "model3D" to 2478,
        "simpleText" to 101,
    )

    private fun readResourceFile(fileName: String): ByteArray {
        return javaClass.classLoader.getResourceAsStream(fileName)?.readBytes()
            ?: throw IllegalArgumentException("Resource file not found: $fileName")
    }

    private fun ByteArray.toUByteArray(): UByteArray {
        return UByteArray(this.size) { this[it].toUByte() }
    }

    private fun UByteArray.toByteArray(): ByteArray {
        return ByteArray(this.size) { this[it].toByte() }
    }

    // RESOURCE TESTS

    @Test
    fun testResourceFilesExist() {
        for (fileName in testFiles) {
            val fileData = readResourceFile(fileName)
            val expectedSize = expectedFileSizes[fileName]

            assert(fileData.isNotEmpty()) { "File $fileName could not be loaded or is empty" }
            assert(fileData.size == expectedSize) {
                "File $fileName has size ${fileData.size} but expected $expectedSize"
            }
        }
    }

    // RAW TESTS

    @Test
    fun testFlateCompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            val compressedData = KFlate.Raw.deflate(originalData.toUByteArray())

            val inflater = Inflater(true)
            val inputStream = ByteArrayInputStream(compressedData.toByteArray())
            val inflaterStream = InflaterInputStream(inputStream, inflater)
            val outputStream = ByteArrayOutputStream()

            inflaterStream.copyTo(outputStream)
            val decompressedData = outputStream.toByteArray()

            assertContentEquals(originalData, decompressedData, "Failed on file: $fileName")

            inflater.end()
        }
    }

    @Test
    fun testFlateDecompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            val deflater = Deflater(6, true)
            val outputStream = ByteArrayOutputStream()
            val deflaterStream = DeflaterOutputStream(outputStream, deflater)

            deflaterStream.write(originalData)
            deflaterStream.finish()
            deflaterStream.close()

            val compressedData = outputStream.toByteArray()

            val decompressedData = KFlate.Raw.inflate(compressedData.toUByteArray(), InflateOptions())

            assertContentEquals(originalData, decompressedData.toByteArray(), "Failed on file: $fileName")

            deflater.end()
        }
    }

    // GZIP TESTS

    @Test
    fun testGzipCompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            val compressedData = KFlate.Gzip.compress(originalData.toUByteArray())

            val inputStream = ByteArrayInputStream(compressedData.toByteArray())
            val gzipInputStream = GZIPInputStream(inputStream)
            val outputStream = ByteArrayOutputStream()

            gzipInputStream.copyTo(outputStream)
            val decompressedData = outputStream.toByteArray()

            assertContentEquals(originalData, decompressedData, "Failed on file: $fileName")
        }
    }

    @Test
    fun testGzipDecompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            val outputStream = ByteArrayOutputStream()
            val gzipOutputStream = GZIPOutputStream(outputStream)

            gzipOutputStream.write(originalData)
            gzipOutputStream.finish()
            gzipOutputStream.close()

            val compressedData = outputStream.toByteArray()

            val decompressedData = KFlate.Gzip.decompress(compressedData.toUByteArray())

            assertContentEquals(originalData, decompressedData.toByteArray(), "Failed on file: $fileName")
        }
    }

    // ZLIB TESTS

    @Test
    fun testZlibCompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            val compressedData = KFlate.Zlib.compress(originalData.toUByteArray())

            val inflater = Inflater()
            val inputStream = ByteArrayInputStream(compressedData.toByteArray())
            val inflaterStream = InflaterInputStream(inputStream, inflater)
            val outputStream = ByteArrayOutputStream()

            inflaterStream.copyTo(outputStream)
            val decompressedData = outputStream.toByteArray()

            assertContentEquals(originalData, decompressedData, "Failed on file: $fileName")

            inflater.end()
        }
    }

    @Test
    fun testZlibDecompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            val deflater = Deflater(Deflater.DEFAULT_COMPRESSION)
            val outputStream = ByteArrayOutputStream()
            val deflaterStream = DeflaterOutputStream(outputStream, deflater)

            deflaterStream.write(originalData)
            deflaterStream.finish()
            deflaterStream.close()

            val compressedData = outputStream.toByteArray()

            val decompressedData = KFlate.Zlib.decompress(compressedData.toUByteArray(), InflateOptions())

            assertContentEquals(originalData, decompressedData.toByteArray(), "Failed on file: $fileName")

            deflater.end()
        }
    }
}
