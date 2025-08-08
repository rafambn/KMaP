@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate

import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

class SizeTest {

    private val testFiles = listOf(
        "model3D",
        "text",
        "Rainier.bmp",
        "Maltese.bmp",
        "Sunrise.bmp",
        "simpleText",
    )

    private fun readResourceFile(fileName: String): ByteArray {
        return javaClass.classLoader.getResourceAsStream(fileName)?.readBytes()
            ?: throw IllegalArgumentException("Resource file not found: $fileName")
    }

    private fun ByteArray.toUByteArray(): UByteArray {
        return UByteArray(this.size) { this[it].toUByte() }
    }

    @Test
    fun testFlateSize() {
        println("Flate Size Test")
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)
            val compressedData = KFlate.Raw.deflate(originalData.toUByteArray())

            val deflater = Deflater(6, true)
            val outputStream = ByteArrayOutputStream()
            val deflaterStream = DeflaterOutputStream(outputStream, deflater)

            deflaterStream.write(originalData)
            deflaterStream.finish()
            deflaterStream.close()

            val expectedCompressedSize = outputStream.toByteArray().size

            println("File: $fileName, KFlate: ${compressedData.size}, JVM: $expectedCompressedSize")
        }
    }

    @Test
    fun testGzipSize() {
        println("Gzip Size Test")
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)
            val compressedData = KFlate.Gzip.compress(originalData.toUByteArray())

            val outputStream = ByteArrayOutputStream()
            val gzipOutputStream = GZIPOutputStream(outputStream)

            gzipOutputStream.write(originalData)
            gzipOutputStream.finish()
            gzipOutputStream.close()

            val expectedCompressedSize = outputStream.toByteArray().size

            println("File: $fileName, KFlate: ${compressedData.size}, JVM: $expectedCompressedSize")
        }
    }

    @Test
    fun testZlibSize() {
        println("Zlib Size Test")
        for (fileName in testFiles) {
            val originalData = readResourceFile(fileName)
            val compressedData = KFlate.Zlib.compress(originalData.toUByteArray())

            val deflater = Deflater(Deflater.DEFAULT_COMPRESSION)
            val outputStream = ByteArrayOutputStream()
            val deflaterStream = DeflaterOutputStream(outputStream, deflater)

            deflaterStream.write(originalData)
            deflaterStream.finish()
            deflaterStream.close()

            val expectedCompressedSize = outputStream.toByteArray().size

            println("File: $fileName, KFlate: ${compressedData.size}, JVM: $expectedCompressedSize")
        }
    }
}
