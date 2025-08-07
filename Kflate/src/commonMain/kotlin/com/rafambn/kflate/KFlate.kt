@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalAtomicApi::class)

package com.rafambn.kflate

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.select
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

object KFlate {
    object Flate {
        fun inflate(data: UByteArray, options: InflateOptions): UByteArray =
            inflate(data, InflateState(lastCheck = 2), null, options.dictionary)

        fun deflate(data: UByteArray, options: DeflateOptions): UByteArray =
            deflateWithOptions(data, options, 0, 0)
    }

    object Gzip {

        fun compress(data: UByteArray, options: GzipOptions): UByteArray {
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

        fun decompress(data: UByteArray, options: DeflateOptions): UByteArray {
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
        fun compress(data: UByteArray, options: DeflateOptions): UByteArray {
            val adler = Adler32Checksum()
            adler.update(data)
            val deflatedData = deflateWithOptions(data, options, if (options.dictionary != null) 6 else 2, 4)
            writeZlibHeader(deflatedData, options)
            writeBytes(deflatedData, deflatedData.size - 4, adler.getChecksum().toLong())
            return deflatedData
        }

        fun decompress(data: UByteArray, options: InflateOptions): UByteArray {
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

    open class FlateStream(protected val scope: CoroutineScope) {

        @OptIn(ExperimentalAtomicApi::class)
        fun decompress(
            options: InflateOptions,
            inputChannel: ReceiveChannel<FlateStreamData>,
            outputChannel: Channel<FlateStreamData>
        ) {
            val inflateState = InflateState(lastCheck = 0, byte = options.dictionary?.size ?: 0)
            var outputBuffer = AtomicReference(UByteArray(32768))
            var pendingInput = AtomicReference(UByteArray(0))
            val isFinished = AtomicBoolean(false)
            options.dictionary?.copyInto(outputBuffer.load())

            scope.launch(Dispatchers.Default) {
                while (isActive)
                    select {
                        inputChannel.onReceive { (chunk, isFinal) ->
                            pendingInflate(chunk, isFinished, pendingInput)
                            processInflate(isFinal, isFinished, inflateState, outputBuffer, pendingInput, outputChannel)
                        }
                    }
            }
        }

        protected fun pendingInflate(
            chunk: UByteArray,
            isFinished: AtomicBoolean,
            pendingInput: AtomicReference<UByteArray>,
        ) {
            if (isFinished.load()) createFlateError(FlateErrorCode.STREAM_FINISHED.code)
            if (pendingInput.load().isEmpty()) {
                pendingInput.load().copyInto(chunk)
            } else {
                val newPendingInput = UByteArray(pendingInput.load().size + chunk.size)
                pendingInput.load().copyInto(newPendingInput)
                chunk.copyInto(newPendingInput, pendingInput.load().size, 0, chunk.size)
                pendingInput.store(newPendingInput)
            }
        }

        protected suspend fun processInflate(
            isFinal: Boolean,
            isFinished: AtomicBoolean,
            inflateState: InflateState,
            outputBuffer: AtomicReference<UByteArray>,
            pendingInput: AtomicReference<UByteArray>,
            outputChannel: Channel<FlateStreamData>
        ) {
            inflateState.lastCheck = if (isFinal) 1 else 0
            val result = inflate(pendingInput.load(), inflateState, outputBuffer.load())
            outputChannel.send(
                FlateStreamData(
                    sliceArray(result, 0, inflateState.byte),
                    isFinished.load()
                )
            )
            outputBuffer.store(sliceArray(result, inflateState.byte!! - 32768))
            inflateState.byte = outputBuffer.load().size
            pendingInput.store(sliceArray(pendingInput.load(), (inflateState.position!! / 8)))
            inflateState.position = inflateState.position!! and 7
        }

        fun compress(
            options: DeflateOptions,
            inputChannel: ReceiveChannel<FlateStreamData>,
            outputChannel: Channel<FlateStreamData>
        ) {
            val deflateState = DeflateState(isLastChunk = 0, index = 32768, waitIndex = 32768, endIndex = 32768)
            var buffer = UByteArray(98304)

            options.dictionary?.let {
                it.copyInto(buffer, destinationOffset = 32768 - it.size)
                deflateState.index = 32768 - it.size
            }

            scope.launch(Dispatchers.Default) {
                while (isActive)
                    select {
                        inputChannel.onReceive { (chunk, isFinal) ->
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
                                processDeflate(buffer, false, outputChannel, options, deflateState)

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
                                processDeflate(buffer, isFinal, outputChannel, options, deflateState)
                                deflateState.waitIndex = deflateState.index
                                deflateState.index -= 2
                            }
                        }
                    }
            }
        }

        protected open suspend fun processDeflate(
            data: UByteArray,
            isFinal: Boolean,
            outputChannel: Channel<FlateStreamData>,
            options: DeflateOptions,
            deflateState: DeflateState
        ) {
            outputChannel.send(FlateStreamData(deflateWithOptions(data, options, 0, 0, deflateState), isFinal))
        }
    }

    class GzipStream(scope: CoroutineScope) : FlateStream(scope) {

        fun compress(
            options: GzipOptions,
            inputChannel: ReceiveChannel<FlateStreamData>,
            outputChannel: Channel<FlateStreamData>
        ) {
            val deflateState = DeflateState(isLastChunk = 0, index = 32768, waitIndex = 32768, endIndex = 32768)
            var buffer = UByteArray(98304)
            var totalUncompressedLength = 0
            val isFirstChunk = AtomicReference(true)
            val crc = Crc32Checksum()

            options.dictionary?.let {
                it.copyInto(buffer, destinationOffset = 32768 - it.size)
                deflateState.index = 32768 - it.size
            }

            scope.launch(Dispatchers.Default) {
                while (isActive)
                    select {
                        inputChannel.onReceive { (chunk, isFinal) ->
                            crc.update(chunk)
                            totalUncompressedLength += chunk.size
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
                                processGzipDeflate(
                                    buffer,
                                    false,
                                    outputChannel,
                                    options,
                                    deflateState,
                                    isFirstChunk,
                                    crc,
                                    totalUncompressedLength
                                )

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
                                processGzipDeflate(
                                    buffer,
                                    isFinal,
                                    outputChannel,
                                    options,
                                    deflateState,
                                    isFirstChunk,
                                    crc,
                                    totalUncompressedLength
                                )
                                deflateState.waitIndex = deflateState.index
                                deflateState.index -= 2
                            }
                        }
                    }
            }
        }

        suspend fun processGzipDeflate(
            data: UByteArray,
            isFinal: Boolean,
            outputChannel: Channel<FlateStreamData>,
            options: GzipOptions,
            deflateState: DeflateState,
            isFirstChunk: AtomicReference<Boolean>,
            crcCalculator: Crc32Checksum,
            totalUncompressedLength: Int,
        ) {
            val raw = deflateWithOptions(data, options, getGzipHeaderSize(options), if (isFinal) 8 else 0, deflateState)
            if (isFirstChunk.load()) {
                writeGzipHeader(raw, options)
                isFirstChunk.store(false)
            }

            if (isFinal) {
                writeBytes(raw, raw.size - 8, crcCalculator.getChecksum().toLong())
                writeBytes(raw, raw.size - 4, totalUncompressedLength.toLong())
            }

            outputChannel.send(FlateStreamData(raw, isFinal))
        }

        fun decompress(
            options: InflateOptions,
            inputChannel: Channel<FlateStreamData>,
            outputChannel: Channel<FlateStreamData>
        ) {
            val inflateState = InflateState(lastCheck = 0, byte = options.dictionary?.size ?: 0)
            val outputBuffer = AtomicReference(UByteArray(32768))
            val pendingInput = AtomicReference(UByteArray(0))
            val isFinished = AtomicBoolean(false)
            options.dictionary?.copyInto(outputBuffer.load())
            val headerLength = AtomicReference(1)
            val totalBytesRead = AtomicReference(0)

            scope.launch {
                select {
                    inputChannel.onReceive { (chunk, isFinal) ->
                        pendingInflate(chunk, isFinished, pendingInput)
                        totalBytesRead.store(totalBytesRead.load() + chunk.size)
                        if (headerLength.load() != 0) {
                            val p = pendingInput.load().sliceArray(headerLength.load() - 1 until pendingInput.load().size)
                            val s = if (p.size > 3) writeGzipStart(p) else 4
                            if (s > p.size) {
                                if (!isFinal) return@onReceive
                            }
                            pendingInput.store(p.sliceArray(s until p.size))
                            headerLength.store(0)
                        }

                        processInflate(isFinal, isFinished, inflateState, outputBuffer, pendingInput, outputChannel)

                        if (inflateState.finalFlag == null && inflateState.literalMap != null) {
                            headerLength.store(shiftToNextByte((inflateState.position ?: 0) + 9))
                            inflateState.lastCheck = 0
                            outputBuffer.store(UByteArray(0))
                            inputChannel.send(FlateStreamData(UByteArray(0), false))
                        } else if (isFinal) {
                            processInflate(isFinal, isFinished, inflateState, outputBuffer, pendingInput, outputChannel)
                        }
                    }
                }
            }
        }
    }

    class ZlibStream(scope: CoroutineScope) : FlateStream(scope) {

        fun compress(
            options: GzipOptions,
            inputChannel: ReceiveChannel<FlateStreamData>,
            outputChannel: Channel<FlateStreamData>
        ) {
            val deflateState = DeflateState(isLastChunk = 0, index = 32768, waitIndex = 32768, endIndex = 32768)
            var buffer = UByteArray(98304)
            val isFirstChunk = AtomicReference(true)
            val adler = Adler32Checksum()

            options.dictionary?.let {
                it.copyInto(buffer, destinationOffset = 32768 - it.size)
                deflateState.index = 32768 - it.size
            }

            scope.launch(Dispatchers.Default) {
                while (isActive)
                    select {
                        inputChannel.onReceive { (chunk, isFinal) ->
                            adler.update(chunk)
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
                                processZlibDeflate(buffer, false, outputChannel, options, deflateState, isFirstChunk, adler)

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
                                processZlibDeflate(buffer, isFinal, outputChannel, options, deflateState, isFirstChunk, adler)
                                deflateState.waitIndex = deflateState.index
                                deflateState.index -= 2
                            }
                        }
                    }
            }
        }

        private suspend fun processZlibDeflate(
            data: UByteArray,
            isFinal: Boolean,
            outputChannel: Channel<FlateStreamData>,
            options: DeflateOptions,
            deflateState: DeflateState,
            isFirstChunk: AtomicReference<Boolean>,
            adlerCalculator: Adler32Checksum,
        ) {
            val headerLength = if (isFirstChunk.load()) {
                if (options.dictionary == null) 6 else 2
            } else {
                0
            }
            val raw = deflateWithOptions(data, options, headerLength, if (isFinal) 4 else 0, deflateState)

            if (isFirstChunk.load()) {
                writeZlibHeader(raw, options)
                isFirstChunk.store(false)
            }

            if (isFinal) {
                writeBytes(raw, raw.size - 4, adlerCalculator.getChecksum().toLong())
            }

            outputChannel.send(FlateStreamData(raw, isFinal))
        }

        fun decompress(
            options: InflateOptions,
            inputChannel: Channel<FlateStreamData>,
            outputChannel: Channel<FlateStreamData>
        ) {
            val inflateState = InflateState(lastCheck = 0, byte = options.dictionary?.size ?: 0)
            var outputBuffer = AtomicReference(UByteArray(32768))
            var pendingInput = AtomicReference(UByteArray(0))
            val isFinished = AtomicBoolean(false)
            val isFirstChunk = AtomicReference(true)
            options.dictionary?.copyInto(outputBuffer.load())
            var headerState = if (options.dictionary != null) 1 else 0

            scope.launch {
                select {
                    inputChannel.onReceive { (chunk, isFinal) ->
                        pendingInflate(chunk, isFinished, pendingInput)
                        if (headerState != 0) {
                            if (pendingInput.load().size < 6 && !isFinal) return@onReceive
                            pendingInput.store(
                                pendingInput.load().copyOfRange(
                                    writeZlibStart(pendingInput.load(), headerState  == 1),
                                    pendingInput.load().size
                                )
                            )
                            headerState = 0
                        }

                        if (isFinal) {
                            if (pendingInput.load().size < 4) throw IllegalArgumentException("Invalid zlib data")
                            pendingInput.store(
                                sliceArray(
                                    pendingInput.load(),
                                    0,
                                    pendingInput.load().size - 4
                                )
                            )
                        }
                        processInflate(isFinal, isFinished, inflateState, outputBuffer, pendingInput, outputChannel)
                    }
                }
            }
        }
    }
}
