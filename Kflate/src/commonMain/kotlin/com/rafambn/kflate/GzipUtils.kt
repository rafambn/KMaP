@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalTime::class)

package com.rafambn.kflate

import com.rafambn.kflate.error.FlateErrorCode
import com.rafambn.kflate.error.createFlateError
import com.rafambn.kflate.options.GzipOptions
import kotlin.math.floor
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal fun writeGzipHeader(output: UByteArray, options: GzipOptions) {
    output[0] = 31u
    output[1] = 139u
    output[2] = 8u
    output[8] = when {
        options.level < 2 -> 4u
        options.level == 9 -> 2u
        else -> 0u
    }
    output[9] = 3u

    val mtime = options.mtime
    val timeInMillis = when (mtime) {
        is Number -> mtime.toLong()
        is String -> mtime.toLongOrNull() ?: Clock.System.now().toEpochMilliseconds()
        else -> Clock.System.now().toEpochMilliseconds()
    }

    if (timeInMillis != 0L)
        writeBytes(output, 4, floor(timeInMillis / 1000.0).toLong())

    options.filename?.let {
        output[3] = 8u
        for (i in it.indices) {
            output[i + 10] = it[i].code.toUByte()
        }
        output[it.length + 10] = 0u
    }
}

internal fun writeGzipStart(data: UByteArray): Int {
    if (data[0].toInt() != 31 || data[1].toInt() != 139 || data[2].toInt() != 8) {
        createFlateError(FlateErrorCode.INVALID_HEADER)
    }
    val flags = data[3].toInt()
    var headerSize = 10
    if ((flags and 4) != 0) {
        headerSize += (data[10].toInt() or (data[11].toInt() shl 8)) + 2
    }
    var remainingFlags = (flags ushr 3 and 1) + (flags ushr 4 and 1)
    while (remainingFlags > 0) {
        if (data[headerSize++].toInt() == 0) {
            remainingFlags--
        }
    }
    return headerSize + (flags and 2)
}

internal fun getGzipUncompressedSize(data: UByteArray): Long {
    val length = data.size
    return readFourBytes(data, length - 4)
}

internal fun getGzipHeaderSize(options: GzipOptions): Int {
    return 10 + if (options.filename != null) options.filename.length + 1 else 0
}
