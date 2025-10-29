@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate

import com.rafambn.kflate.checksum.Adler32Checksum
import com.rafambn.kflate.checksum.Crc32Checksum
import com.rafambn.kflate.error.FlateErrorCode
import com.rafambn.kflate.error.createFlateError
import com.rafambn.kflate.options.DeflateOptions
import com.rafambn.kflate.options.GzipOptions
import com.rafambn.kflate.options.InflateOptions

object KFlate {

    object Raw {
        fun inflate(data: UByteArray, options: InflateOptions = InflateOptions()): UByteArray =
            inflate(data, InflateState(lastCheck = 2), null, options.dictionary)

        fun deflate(data: UByteArray, options: DeflateOptions = DeflateOptions()): UByteArray =
            deflateWithOptions(data, options, 0, 0)
    }

    object Gzip {

        fun compress(data: UByteArray, options: GzipOptions = GzipOptions()): UByteArray {
            val crc = Crc32Checksum()
            val dataLength = data.size
            crc.update(data)
            val deflatedData = deflateWithOptions(data, options, getGzipHeaderSize(options), 8)
            val deflatedDataLength = deflatedData.size
            writeGzipHeader(deflatedData, options)
            writeBytes(deflatedData, deflatedDataLength - 8, crc.getChecksum().toLong())
            writeBytes(deflatedData, deflatedDataLength - 4, dataLength.toLong())
            return deflatedData
        }

        fun decompress(data: UByteArray, options: DeflateOptions = DeflateOptions()): UByteArray {
            val start = writeGzipStart(data)
            if (start + 8 > data.size) {
                createFlateError(FlateErrorCode.INVALID_HEADER)
            }
            return inflate(
                data.copyOfRange(start, data.size - 8),
                InflateState(lastCheck = 2),
                UByteArray(getGzipUncompressedSize(data).toInt()),
                options.dictionary
            )
        }
    }

    object Zlib {
        fun compress(data: UByteArray, options: DeflateOptions = DeflateOptions()): UByteArray {
            val adler = Adler32Checksum()
            adler.update(data)
            val deflatedData = deflateWithOptions(data, options, if (options.dictionary != null) 6 else 2, 4)
            writeZlibHeader(deflatedData, options)
            writeBytes(deflatedData, deflatedData.size - 4, adler.getChecksum().toLong())
            return deflatedData
        }

        fun decompress(data: UByteArray, options: InflateOptions = InflateOptions()): UByteArray {
            val start = writeZlibStart(data, options.dictionary != null)
            val inputData = data.copyOfRange(start, data.size - 4)
            return inflate(
                inputData,
                InflateState(lastCheck = 2),
                null,
                options.dictionary
            )
        }
    }
}
