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

class StreamingValidityTest {

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

    // RAW STREAMING TESTS

    @Test
    fun testRawStreamingCompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName).toUByteArray()

            var compressedChunks = UByteArray(0)
            var streamingFinal = false

            val compressor = KFlate.RawStream.Compressor(
                DeflateOptions(),
                { chunk, isFinal ->
                    compressedChunks = compressedChunks.copyOf(compressedChunks.size + chunk.size)
                    chunk.copyInto(compressedChunks, compressedChunks.size - chunk.size)
                    streamingFinal = isFinal
                }
            )

            val chunkSize = originalData.size / 3
            val chunk1 = originalData.copyOfRange(0, chunkSize)
            val chunk2 = originalData.copyOfRange(chunkSize, 2 * chunkSize)
            val chunk3 = originalData.copyOfRange(2 * chunkSize, originalData.size)

            compressor.push(chunk1, false)
            compressor.push(chunk2, false)
            compressor.push(chunk3, true)

            assert(streamingFinal) { "Final flag was not set for file: $fileName" }

            val inflater = Inflater(true)
            val inputStream = ByteArrayInputStream(compressedChunks.toByteArray())
            val inflaterStream = InflaterInputStream(inputStream, inflater)
            val outputStream = ByteArrayOutputStream()

            inflaterStream.copyTo(outputStream)
            val decompressedData = outputStream.toByteArray()

            assertContentEquals(originalData.toByteArray(), decompressedData, "Failed on file: $fileName")

            inflater.end()
        }
    }

    @Test
    fun testRawStreamingDecompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            val deflater = Deflater(6, true)
            val outputStream = ByteArrayOutputStream()
            val deflaterStream = DeflaterOutputStream(outputStream, deflater)

            deflaterStream.write(originalData)
            deflaterStream.finish()
            deflaterStream.close()

            val compressedData = outputStream.toByteArray().toUByteArray()

            var decompressedChunks = UByteArray(0)
            var streamingFinal = false

            val decompressor = KFlate.RawStream.Decompressor(InflateOptions()) { chunk, isFinal ->
                decompressedChunks = decompressedChunks.copyOf(decompressedChunks.size + chunk.size)
                chunk.copyInto(decompressedChunks, decompressedChunks.size - chunk.size)
                streamingFinal = isFinal
            }

            val chunkSize = compressedData.size / 3
            val chunk1 = compressedData.copyOfRange(0, chunkSize)
            val chunk2 = compressedData.copyOfRange(chunkSize, 2 * chunkSize)
            val chunk3 = compressedData.copyOfRange(2 * chunkSize, compressedData.size)

            decompressor.push(chunk1, false)
            decompressor.push(chunk2, false)
            decompressor.push(chunk3, true)

            assert(streamingFinal) { "Final flag was not set for file: $fileName" }

            assertContentEquals(originalData, decompressedChunks.toByteArray(), "Failed on file: $fileName")

            deflater.end()
        }
    }

    // GZIP STREAMING TESTS

    @Test
    fun testGzipStreamingCompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName).toUByteArray()

            var compressedChunks = UByteArray(0)
            var streamingFinal = false

            val compressor = KFlate.GzipStream.Compressor(GzipOptions()) { chunk, isFinal ->
                compressedChunks = compressedChunks.copyOf(compressedChunks.size + chunk.size)
                chunk.copyInto(compressedChunks, compressedChunks.size - chunk.size)
                streamingFinal = isFinal
            }

            val chunkSize = originalData.size / 3
            val chunk1 = originalData.copyOfRange(0, chunkSize)
            val chunk2 = originalData.copyOfRange(chunkSize, 2 * chunkSize)
            val chunk3 = originalData.copyOfRange(2 * chunkSize, originalData.size)

            compressor.push(chunk1, false)
            compressor.push(chunk2, false)
            compressor.push(chunk3, true)

            assert(streamingFinal) { "Final flag was not set for file: $fileName" }

            val inputStream = ByteArrayInputStream(compressedChunks.toByteArray())
            val gzipInputStream = GZIPInputStream(inputStream)
            val outputStream = ByteArrayOutputStream()

            gzipInputStream.copyTo(outputStream)
            val decompressedData = outputStream.toByteArray()

            assertContentEquals(originalData.toByteArray(), decompressedData, "Failed on file: $fileName")
        }
    }

    @Test
    fun testGzipStreamingDecompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            val outputStream = ByteArrayOutputStream()
            val gzipOutputStream = GZIPOutputStream(outputStream)

            gzipOutputStream.write(originalData)
            gzipOutputStream.finish()
            gzipOutputStream.close()

            val compressedData = outputStream.toByteArray().toUByteArray()

            var decompressedChunks = UByteArray(0)
            var streamingFinal = false

            val decompressor = KFlate.GzipStream.Decompressor(InflateOptions()) { chunk, isFinal ->
                decompressedChunks = decompressedChunks.copyOf(decompressedChunks.size + chunk.size)
                chunk.copyInto(decompressedChunks, decompressedChunks.size - chunk.size)
                streamingFinal = isFinal
            }

            val chunkSize = compressedData.size / 3
            val chunk1 = compressedData.copyOfRange(0, chunkSize)
            val chunk2 = compressedData.copyOfRange(chunkSize, 2 * chunkSize)
            val chunk3 = compressedData.copyOfRange(2 * chunkSize, compressedData.size)

            decompressor.push(chunk1, false)
            decompressor.push(chunk2, false)
            decompressor.push(chunk3, true)

            assert(streamingFinal) { "Final flag was not set for file: $fileName" }

            assertContentEquals(originalData, decompressedChunks.toByteArray(), "Failed on file: $fileName")
        }
    }

    // ZLIB STREAMING TESTS

    @Test
    fun testZlibStreamingCompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName).toUByteArray()

            var compressedChunks = UByteArray(0)
            var streamingFinal = false

            val compressor = KFlate.ZlibStream.Compressor(DeflateOptions()) { chunk, isFinal ->
                compressedChunks = compressedChunks.copyOf(compressedChunks.size + chunk.size)
                chunk.copyInto(compressedChunks, compressedChunks.size - chunk.size)
                streamingFinal = isFinal
            }

            val chunkSize = originalData.size / 3
            val chunk1 = originalData.copyOfRange(0, chunkSize)
            val chunk2 = originalData.copyOfRange(chunkSize, 2 * chunkSize)
            val chunk3 = originalData.copyOfRange(2 * chunkSize, originalData.size)

            compressor.push(chunk1, false)
            compressor.push(chunk2, false)
            compressor.push(chunk3, true)

            assert(streamingFinal) { "Final flag was not set for file: $fileName" }

            val inflater = Inflater()
            val inputStream = ByteArrayInputStream(compressedChunks.toByteArray())
            val inflaterStream = InflaterInputStream(inputStream, inflater)
            val outputStream = ByteArrayOutputStream()

            inflaterStream.copyTo(outputStream)
            val decompressedData = outputStream.toByteArray()

            assertContentEquals(originalData.toByteArray(), decompressedData, "Failed on file: $fileName")

            inflater.end()
        }
    }

    @Test
    fun testZlibStreamingDecompress() {
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)

            val deflater = Deflater(Deflater.DEFAULT_COMPRESSION)
            val outputStream = ByteArrayOutputStream()
            val deflaterStream = DeflaterOutputStream(outputStream, deflater)

            deflaterStream.write(originalData)
            deflaterStream.finish()
            deflaterStream.close()

            val compressedData = outputStream.toByteArray().toUByteArray()

            var decompressedChunks = UByteArray(0)
            var streamingFinal = false

            val decompressor = KFlate.ZlibStream.Decompressor(InflateOptions()) { chunk, isFinal ->
                decompressedChunks = decompressedChunks.copyOf(decompressedChunks.size + chunk.size)
                chunk.copyInto(decompressedChunks, decompressedChunks.size - chunk.size)
                streamingFinal = isFinal
            }

            val chunkSize = compressedData.size / 3
            val chunk1 = compressedData.copyOfRange(0, chunkSize)
            val chunk2 = compressedData.copyOfRange(chunkSize, 2 * chunkSize)
            val chunk3 = compressedData.copyOfRange(2 * chunkSize, compressedData.size)

            decompressor.push(chunk1, false)
            decompressor.push(chunk2, false)
            decompressor.push(chunk3, true)

            assert(streamingFinal) { "Final flag was not set for file: $fileName" }

            assertContentEquals(originalData, decompressedChunks.toByteArray(), "Failed on file: $fileName")

            deflater.end()
        }
    }
}
