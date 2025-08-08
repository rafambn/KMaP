@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate

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
                createFlateError(6)//TODO , "invalid gzip data"
            }
            return inflate(
                data.copyOfRange(start, data.size - 8),
                InflateState(lastCheck = 2),
                null,
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

    object RawStream {

        fun Compressor(
            options: DeflateOptions = DeflateOptions(),
            onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
        ) = RawStreamCompressor(options, onData)

        fun Decompressor(
            options: InflateOptions = InflateOptions(),
            onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
        ) = RawStreamDecompressor(options, onData)
    }

    object GzipStream {
        fun Compressor(
            options: GzipOptions = GzipOptions(),
            onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
        ) = GzipStreamCompressor(options, onData)

        fun Decompressor(
            options: InflateOptions = InflateOptions(),
            onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
        ) = GzipStreamDecompressor(options, onData)
    }

    object ZlibStream {
        fun Compressor(
            options: DeflateOptions = DeflateOptions(),
            onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
        ) = ZlibStreamCompressor(options, onData)

        fun Decompressor(
            options: InflateOptions = InflateOptions(),
            onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
        ) = ZlibStreamDecompressor(options, onData)
    }

    open class RawStreamCompressor(
        open val options: DeflateOptions = DeflateOptions(),
        val onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
    ) {

        val deflateState = DeflateState(isLastChunk = 0, index = 32768, waitIndex = 32768, endIndex = 32768)
        var buffer = UByteArray(98304)

        init {
            options.dictionary?.let {
                it.copyInto(buffer, destinationOffset = 32768 - it.size)
                deflateState.index = 32768 - it.size
            }
        }

        protected open fun processDeflate(
            chunk: UByteArray,
            isFinal: Boolean,
        ) {
            onData(deflateWithOptions(chunk, options, 0, 0, deflateState), isFinal)
        }

        open fun push(chunk: UByteArray, isFinal: Boolean) {
            if (deflateState.isLastChunk != 0) createFlateError(FlateErrorCode.STREAM_FINISHED.code)

            val endLength = chunk.size + deflateState.endIndex
            if (endLength > buffer.size) {
                if (endLength > 2 * buffer.size - 32768) {
                    val newBuffer = UByteArray(endLength and -32768)
                    buffer.copyInto(newBuffer, endIndex = deflateState.endIndex)
                    buffer = newBuffer
                }

                val split = buffer.size - deflateState.endIndex
                chunk.copyInto(buffer, destinationOffset = deflateState.endIndex, endIndex = split)
                deflateState.endIndex = buffer.size
                processDeflate(buffer, false)

                buffer.copyInto(buffer, destinationOffset = 0, startIndex = buffer.size - 32768)
                chunk.copyInto(buffer, destinationOffset = 32768, startIndex = split)
                deflateState.endIndex = chunk.size - split + 32768
                deflateState.index = 32766
                deflateState.waitIndex = 32768
            } else {
                chunk.copyInto(buffer, destinationOffset = deflateState.endIndex)
                deflateState.endIndex += chunk.size
            }

            deflateState.isLastChunk = if (isFinal) 1 else 0
            if (deflateState.endIndex > deflateState.waitIndex + 8191 || isFinal) {
                processDeflate(buffer, isFinal)
                deflateState.waitIndex = deflateState.index
                deflateState.index -= 2
            }
        }

        protected open fun flush() {
            if (deflateState.isLastChunk != 0) createFlateError(FlateErrorCode.STREAM_FINISHED.code)
            push(buffer, false)
            deflateState.waitIndex = deflateState.index
            deflateState.index -= 2
        }
    }

    open class RawStreamDecompressor(
        options: InflateOptions = InflateOptions(),
        val onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
    ) {

        val inflateState = InflateState(lastCheck = 0, byte = options.dictionary?.size ?: 0)
        var outputBuffer = UByteArray(32768)
        var pendingBuffer = UByteArray(0)
        val isFinished = false

        init {
            options.dictionary?.copyInto(outputBuffer)
        }

        protected fun accumulateChunk(
            chunk: UByteArray,
        ) {
            if (isFinished) createFlateError(FlateErrorCode.STREAM_FINISHED.code)
            if (pendingBuffer.isEmpty()) {
                pendingBuffer.copyInto(chunk)
            } else {
                val newPendingInput = UByteArray(pendingBuffer.size + chunk.size)
                pendingBuffer.copyInto(newPendingInput)
                chunk.copyInto(newPendingInput, pendingBuffer.size, 0, chunk.size)
                pendingBuffer = newPendingInput
            }
        }

        protected fun processInput(
            isFinal: Boolean,
        ) {
            inflateState.lastCheck = if (isFinal) 1 else 0
            val result = inflate(pendingBuffer, inflateState, outputBuffer)
            onData(
                sliceArray(result, 0, inflateState.byte),
                isFinished
            )
            outputBuffer = sliceArray(result, inflateState.byte!! - 32768)
            inflateState.byte = outputBuffer.size
            pendingBuffer = sliceArray(pendingBuffer, (inflateState.position!! / 8))
            inflateState.position = inflateState.position!! and 7
        }


        open fun push(
            chunk: UByteArray,
            isFinal: Boolean,
        ) {
            accumulateChunk(chunk)
            processInput(isFinal)
        }
    }

    class GzipStreamCompressor(
        override val options: GzipOptions = GzipOptions(),
        onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
    ) : RawStreamCompressor(options, onData) {

        var totalUncompressedLength = 0
        var isFirstChunk = true
        val crc = Crc32Checksum()

        init {
            options.dictionary?.let {
                it.copyInto(buffer, destinationOffset = 32768 - it.size)
                deflateState.index = 32768 - it.size
            }
        }

        override fun push(
            chunk: UByteArray,
            isFinal: Boolean,
        ) {
            crc.update(chunk)
            totalUncompressedLength += chunk.size
            super.push(chunk, isFinal)
        }

        override fun processDeflate(chunk: UByteArray, isFinal: Boolean) {
            val raw = deflateWithOptions(chunk, options, getGzipHeaderSize(options), if (isFinal) 8 else 0, deflateState)
            if (isFirstChunk) {
                writeGzipHeader(raw, options)
                isFirstChunk = false
            }

            if (isFinal) {
                writeBytes(raw, raw.size - 8, crc.getChecksum().toLong())
                writeBytes(raw, raw.size - 4, totalUncompressedLength.toLong())
            }
            onData(raw, isFinal)
        }
    }

    class GzipStreamDecompressor(
        options: InflateOptions = InflateOptions(),
        onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
    ) : RawStreamDecompressor(options, onData) {

        var headerLength = 1
        var totalBytesRead = 0
        override fun push(
            chunk: UByteArray,
            isFinal: Boolean,
        ) {
            super.accumulateChunk(chunk)
            totalBytesRead += chunk.size
            if (headerLength != 0) {
                val pendingBufferCopy = pendingBuffer.sliceArray(headerLength - 1 until pendingBuffer.size)
                val size = if (pendingBufferCopy.size > 3)
                    writeGzipStart(pendingBufferCopy)
                else
                    4
                if (size > pendingBufferCopy.size) {
                    if (!isFinal) return
                }
                pendingBuffer = pendingBufferCopy.sliceArray(size until pendingBufferCopy.size)
                headerLength = 0
            }

            super.processInput(isFinal)

            if (inflateState.finalFlag == null && inflateState.literalMap != null) {
                headerLength = shiftToNextByte((inflateState.position ?: 0) + 9)
                inflateState.lastCheck = 0
                outputBuffer = UByteArray(0)
                super.push(chunk, isFinal)
            } else if (isFinal) {
                super.push(chunk, isFinal)
            }
        }
    }

    class ZlibStreamCompressor(
        override val options: DeflateOptions = DeflateOptions(),
        onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
    ) : RawStreamCompressor(options, onData) {

        var isFirstChunk = true
        val adler = Adler32Checksum()

        init {
            options.dictionary?.let {
                it.copyInto(buffer, destinationOffset = 32768 - it.size)
                deflateState.index = 32768 - it.size
            }
        }

        override fun push(chunk: UByteArray, isFinal: Boolean) {
            adler.update(chunk)
            super.push(chunk, isFinal)
        }

        override fun processDeflate(chunk: UByteArray, isFinal: Boolean) {
            val headerLength = if (isFirstChunk) {
                if (options.dictionary == null) 6 else 2
            } else
                0

            val raw = deflateWithOptions(chunk, options, headerLength, if (isFinal) 4 else 0, deflateState)
            if (isFirstChunk) {
                writeZlibHeader(raw, options)
                isFirstChunk = false
            }
            if (isFinal) {
                writeBytes(raw, raw.size - 4, adler.getChecksum().toLong())
            }
            onData(raw, isFinal)
        }
    }

    class ZlibStreamDecompressor(
        options: InflateOptions = InflateOptions(),
        onData: (chunk: UByteArray, isFinal: Boolean) -> Unit
    ) : RawStreamDecompressor(options, onData) {
        var headerState = if (options.dictionary != null) 1 else 0
        override fun push(chunk: UByteArray, isFinal: Boolean) {
            accumulateChunk(chunk)
            if (headerState != 0) {
                if (pendingBuffer.size < 6 && !isFinal) return
                pendingBuffer = pendingBuffer.copyOfRange(
                    writeZlibStart(pendingBuffer, headerState == 1),
                    pendingBuffer.size
                )
                headerState = 0
            }

            if (isFinal) {
                if (pendingBuffer.size < 4) throw IllegalArgumentException("Invalid zlib data")
                pendingBuffer = sliceArray(
                    pendingBuffer,
                    0,
                    pendingBuffer.size - 4
                )
            }
            processInput(isFinal)
        }
    }
}
