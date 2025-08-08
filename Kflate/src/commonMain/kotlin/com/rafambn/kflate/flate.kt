@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalTime::class)

package com.rafambn.kflate

import com.rafambn.kflate.error.FlateErrorCode
import com.rafambn.kflate.error.createFlateError
import com.rafambn.kflate.options.DeflateOptions
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.time.ExperimentalTime

internal fun inflate(
    inputData: UByteArray,
    inflateState: InflateState,
    outputBuffer: UByteArray? = null,
    dictionary: UByteArray? = null
): UByteArray {
    val sourceLength = inputData.size
    val dictionaryLength = dictionary?.size ?: 0

    if (sourceLength == 0 || (inflateState.finalFlag != null &&
                inflateState.finalFlag != 0 && inflateState.literalMap == null)
    ) {
        return outputBuffer ?: UByteArray(0)
    }

    var workingBuffer = outputBuffer
    val isBufferProvided = workingBuffer != null

    val needsResize = !isBufferProvided || inflateState.lastCheck != 2
    val hasNoStoredState = inflateState.lastCheck

    if (!isBufferProvided)
        workingBuffer = UByteArray(sourceLength * 3)

    val ensureBufferCapacity = { requiredSize: Int ->
        val currentSize = workingBuffer!!.size
        if (requiredSize > currentSize) {
            val newSize = maxOf(currentSize * 2, requiredSize)
            val newBuffer = UByteArray(newSize)
            workingBuffer!!.copyInto(newBuffer)
            workingBuffer = newBuffer
        }
    }

    var isFinalBlock = inflateState.finalFlag ?: 0
    var currentBitPosition = inflateState.position ?: 0
    var bytesWrittenToOutput = inflateState.byte ?: 0
    var literalLengthMap = inflateState.literalMap
    var distanceMap = inflateState.distanceMap
    var literalMaxBits = inflateState.literalBits
    var distanceMaxBits = inflateState.distanceBits

    val totalAvailableBits = sourceLength * 8

    do {
        if (literalLengthMap == null) {
            isFinalBlock = readBits(inputData, currentBitPosition, 1)
            val blockType = readBits(inputData, currentBitPosition + 1, 3)
            currentBitPosition += 3

            when (blockType) {
                0 -> {
                    val blockStartByte = shiftToNextByte(currentBitPosition) + 4
                    val blockLength = (inputData[blockStartByte - 4].toInt() and 0xFF) or
                            ((inputData[blockStartByte - 3].toInt() and 0xFF) shl 8)
                    val blockEndByte = blockStartByte + blockLength

                    if (blockEndByte > sourceLength) {
                        if (hasNoStoredState != 0) createFlateError(FlateErrorCode.UNEXPECTED_EOF)
                        break
                    }

                    if (needsResize) ensureBufferCapacity(bytesWrittenToOutput + blockLength)

                    inputData.copyInto(
                        workingBuffer,
                        destinationOffset = bytesWrittenToOutput,
                        startIndex = blockStartByte,
                        endIndex = blockEndByte
                    )

                    bytesWrittenToOutput += blockLength
                    currentBitPosition = blockEndByte * 8

                    inflateState.byte = bytesWrittenToOutput
                    inflateState.position = currentBitPosition
                    inflateState.finalFlag = isFinalBlock
                    continue
                }

                1 -> {
                    literalLengthMap = FIXED_LENGTH_REVERSE_MAP
                    distanceMap = FIXED_DISTANCE_REVERSE_MAP
                    literalMaxBits = 9
                    distanceMaxBits = 5
                }

                2 -> {
                    val numLiteralCodes = readBits(inputData, currentBitPosition, 31) + 257
                    val numDistanceCodes = readBits(inputData, currentBitPosition + 5, 31) + 1
                    val numCodeLengthCodes = readBits(inputData, currentBitPosition + 10, 15) + 4
                    val totalCodes = numLiteralCodes + numDistanceCodes
                    currentBitPosition += 14

                    val codeLengthTree = UByteArray(19)
                    for (i in 0 until numCodeLengthCodes) {
                        codeLengthTree[CODE_LENGTH_INDEX_MAP[i].toInt()] = readBits(inputData, currentBitPosition + i * 3, 7).toUByte()
                    }
                    currentBitPosition += numCodeLengthCodes * 3

                    val codeLengthMaxBits = findMaxValue(codeLengthTree).toInt()
                    val codeLengthBitMask = (1 shl codeLengthMaxBits) - 1
                    val codeLengthHuffmanMap = createHuffmanTree(codeLengthTree, codeLengthMaxBits, true)

                    val allCodeLengths = UByteArray(totalCodes)
                    var codeIndex = 0

                    while (codeIndex < totalCodes) {
                        val huffmanCode = codeLengthHuffmanMap[readBits(inputData, currentBitPosition, codeLengthBitMask)]
                        currentBitPosition += (huffmanCode.toInt() and 15)
                        val symbol = huffmanCode.toInt() shr 4

                        when {
                            symbol < 16 -> {
                                allCodeLengths[codeIndex++] = symbol.toUByte()
                            }

                            symbol == 16 -> {
                                val repeatCount = 3 + readBits(inputData, currentBitPosition, 3)
                                currentBitPosition += 2
                                val valueToRepeat = allCodeLengths[codeIndex - 1]
                                repeat(repeatCount) { allCodeLengths[codeIndex++] = valueToRepeat }
                            }

                            symbol == 17 -> {
                                val repeatCount = 3 + readBits(inputData, currentBitPosition, 7)
                                currentBitPosition += 3
                                repeat(repeatCount) { allCodeLengths[codeIndex++] = 0u }
                            }

                            symbol == 18 -> {
                                val repeatCount = 11 + readBits(inputData, currentBitPosition, 127)
                                currentBitPosition += 7
                                repeat(repeatCount) { allCodeLengths[codeIndex++] = 0u }
                            }
                        }
                    }

                    val literalLengthCodeLengths = allCodeLengths.copyOfRange(0, numLiteralCodes)
                    val distanceCodeLengths = allCodeLengths.copyOfRange(numLiteralCodes, totalCodes)

                    literalMaxBits = findMaxValue(literalLengthCodeLengths).toInt()
                    distanceMaxBits = findMaxValue(distanceCodeLengths).toInt()

                    literalLengthMap = createHuffmanTree(literalLengthCodeLengths, literalMaxBits, true)
                    distanceMap = createHuffmanTree(distanceCodeLengths, distanceMaxBits, true)
                }

                else -> createFlateError(FlateErrorCode.INVALID_BLOCK_TYPE)
            }

            if (currentBitPosition > totalAvailableBits) {
                if (hasNoStoredState != 0) createFlateError(FlateErrorCode.UNEXPECTED_EOF)
                break
            }
        }

        if (needsResize) ensureBufferCapacity(bytesWrittenToOutput + 131072)

        val literalBitMask = (1 shl literalMaxBits!!) - 1
        val distanceBitMask = (1 shl distanceMaxBits!!) - 1
        var savedBitPosition = currentBitPosition

        while (true) {
            val literalCode = literalLengthMap!![readBits16(inputData, currentBitPosition) and literalBitMask]
            val symbol = literalCode.toInt() shr 4
            currentBitPosition += (literalCode.toInt() and 15)

            if (currentBitPosition > totalAvailableBits) {
                if (hasNoStoredState != 0) createFlateError(FlateErrorCode.UNEXPECTED_EOF)
                break
            }

            if (literalCode.toInt() == 0) createFlateError(FlateErrorCode.INVALID_LENGTH_LITERAL)

            when {
                symbol < 256 -> {
                    workingBuffer[bytesWrittenToOutput++] = symbol.toUByte()
                }

                symbol == 256 -> {
                    savedBitPosition = currentBitPosition
                    literalLengthMap = null
                    break
                }

                else -> {
                    var matchLength = symbol - 254

                    if (symbol > 264) {
                        val lengthIndex = symbol - 257
                        val extraBits = FIXED_LENGTH_EXTRA_BITS[lengthIndex]
                        matchLength =
                            readBits(inputData, currentBitPosition, (1 shl extraBits.toInt()) - 1) + FIXED_LENGTH_BASE[lengthIndex].toInt()
                        currentBitPosition += extraBits.toInt()
                    }

                    val distanceCode = distanceMap!![readBits16(inputData, currentBitPosition) and distanceBitMask]
                    val distanceSymbol = distanceCode.toInt() shr 4
                    if (distanceCode.toInt() == 0) createFlateError(FlateErrorCode.INVALID_DISTANCE)
                    currentBitPosition += (distanceCode.toInt() and 15)

                    var matchDistance = FIXED_DISTANCE_BASE[distanceSymbol].toInt()
                    if (distanceSymbol > 3) {
                        val extraBits = FIXED_DISTANCE_EXTRA_BITS[distanceSymbol].toInt()
                        matchDistance += readBits16(inputData, currentBitPosition) and ((1 shl extraBits) - 1)
                        currentBitPosition += extraBits
                    }

                    if (currentBitPosition > totalAvailableBits)
                        createFlateError(FlateErrorCode.UNEXPECTED_EOF)

                    if (needsResize) ensureBufferCapacity(bytesWrittenToOutput + 131072)

                    val copyEndIndex = bytesWrittenToOutput + matchLength

                    if (bytesWrittenToOutput < matchDistance) {
                        val dictionaryOffset = dictionaryLength - matchDistance
                        val dictionaryEndIndex = minOf(matchDistance, copyEndIndex)
                        if (dictionaryOffset + bytesWrittenToOutput < 0) {
                            createFlateError(FlateErrorCode.INVALID_DISTANCE)
                        }

                        while (bytesWrittenToOutput < dictionaryEndIndex) {
                            workingBuffer[bytesWrittenToOutput] = dictionary!![dictionaryOffset + bytesWrittenToOutput]
                            bytesWrittenToOutput++
                        }
                    }

                    while (bytesWrittenToOutput < copyEndIndex) {
                        workingBuffer[bytesWrittenToOutput] = workingBuffer[bytesWrittenToOutput - matchDistance]
                        bytesWrittenToOutput++
                    }
                }
            }
        }

        inflateState.literalMap = literalLengthMap
        inflateState.position = savedBitPosition
        inflateState.byte = bytesWrittenToOutput
        inflateState.finalFlag = isFinalBlock

        if (literalLengthMap != null) {
            isFinalBlock = 1
            inflateState.literalBits = literalMaxBits
            inflateState.distanceMap = distanceMap
            inflateState.distanceBits = distanceMaxBits
        }

    } while (isFinalBlock == 0)

    return workingBuffer.copyOfRange(0, bytesWrittenToOutput)
}

internal fun deflate(
    data: UByteArray,
    level: Int,
    compressionLevel: Int,
    prefixSize: Int,
    postfixSize: Int,
    state: DeflateState
): UByteArray {
    val dataSize = state.endIndex.takeIf { it != 0 } ?: data.size
    val output = UByteArray(prefixSize + dataSize + 5 * (1 + ceil((dataSize / 7000.0)).toInt()) + postfixSize)
    val writeBuffer = output.sliceArray(prefixSize until output.size - postfixSize)
    val isLastBlock = state.isLastChunk
    var bitPosition = state.remainderByteInfo and 7

    if (level > 0) {
        if (bitPosition != 0) {
            writeBuffer[0] = (state.remainderByteInfo shr 3).toUByte()
        }
        val option = DEFLATE_OPTIONS[level - 1]
        val niceLength = option shr 13
        val chainLength = option and 8191
        val mask = (1 shl compressionLevel) - 1
        val prev = state.prev ?: UShortArray(32768)
        val head = state.head ?: UShortArray(mask + 1)
        val baseShift1 = ceil(compressionLevel / 3.0).toInt()
        val baseShift2 = 2 * baseShift1
        val hash = { i: Int -> (data[i].toInt() xor (data[i + 1].toInt() shl baseShift1) xor (data[i + 2].toInt() shl baseShift2)) and mask }

        val symbols = IntArray(25000)
        val literalFrequencies = UShortArray(288)
        val distanceFrequencies = UShortArray(32)
        var literalCount = 0
        var extraBits = 0
        var i = state.index
        var symbolIndex = 0
        var waitIndex = state.waitIndex
        var blockStart = 0

        while (i + 2 < dataSize) {
            val hashValue = hash(i)
            var iMod = i and 32767
            var pIMod = head[hashValue].toInt()
            prev[iMod] = pIMod.toUShort()
            head[hashValue] = iMod.toUShort()

            if (waitIndex <= i) {
                val remaining = dataSize - i
                if ((literalCount > 7000 || symbolIndex > 24576) && (remaining > 423 || isLastBlock == 0)) {
                    bitPosition = writeBlock(
                        data, writeBuffer, false, symbols, literalFrequencies, distanceFrequencies,
                        extraBits, symbolIndex, blockStart, i - blockStart, bitPosition
                    )
                    symbolIndex = 0
                    literalCount = 0
                    extraBits = 0
                    blockStart = i
                    literalFrequencies.fill(0u, 0, 286)
                    distanceFrequencies.fill(0u, 0, 30)
                }

                var length = 2
                var distance = 0
                var currentChain = chainLength
                var diff = (iMod - pIMod) and 32767

                if (remaining > 2 && hashValue == hash(i - diff)) {
                    val maxN = minOf(niceLength, remaining) - 1
                    val maxD = minOf(32767, i)
                    val maxLength = minOf(258, remaining)

                    while (diff <= maxD && --currentChain != 0 && iMod != pIMod) {
                        if (data[i + length] == data[i + length - diff]) {
                            var newLength = 0
                            while (newLength < maxLength && data[i + newLength] == data[i + newLength - diff]) {
                                newLength++
                            }
                            if (newLength > length) {
                                length = newLength
                                distance = diff
                                if (newLength > maxN) break

                                val minMatchDiff = minOf(diff, newLength - 2)
                                var maxDiff = 0
                                for (j in 0 until minMatchDiff) {
                                    val tI = (i - diff + j) and 32767
                                    val pTI = prev[tI].toInt()
                                    val cD = (tI - pTI) and 32767
                                    if (cD > maxDiff) {
                                        maxDiff = cD
                                        pIMod = tI
                                    }
                                }
                            }
                        }
                        iMod = pIMod
                        pIMod = prev[iMod].toInt()
                        diff += (iMod - pIMod) and 32767
                    }
                }

                if (distance != 0) {
                    symbols[symbolIndex++] = 268435456 or (FIXED_LENGTH_REVERSE_LOOKUP[length] shl 18) or FIXED_DISTANCE_REVERSE_LOOKUP[distance]
                    val lenIndex = FIXED_LENGTH_REVERSE_LOOKUP[length] and 31
                    val distIndex = FIXED_DISTANCE_REVERSE_LOOKUP[distance] and 31
                    extraBits += FIXED_LENGTH_EXTRA_BITS[lenIndex].toInt() + FIXED_DISTANCE_EXTRA_BITS[distIndex].toInt()
                    ++literalFrequencies[257 + lenIndex]
                    ++distanceFrequencies[distIndex]
                    waitIndex = i + length
                    ++literalCount
                } else {
                    symbols[symbolIndex++] = data[i].toInt()
                    ++literalFrequencies[data[i].toInt()]
                }
            }
            i++
        }

        i = maxOf(i, waitIndex)
        while (i < dataSize) {
            symbols[symbolIndex++] = data[i].toInt()
            literalFrequencies[data[i].toInt()]++
            i++
        }

        bitPosition = writeBlock(
            data, writeBuffer, isLastBlock != 0, symbols, literalFrequencies, distanceFrequencies,
            extraBits, symbolIndex, blockStart, i - blockStart, bitPosition
        )

        if (isLastBlock == 0) {
            state.remainderByteInfo = (bitPosition and 7) or (writeBuffer[bitPosition / 8].toInt() shl 3)
            bitPosition -= 7
            state.head = head
            state.prev = prev
            state.index = i
            state.waitIndex = waitIndex
        }
    } else {
        var i = state.waitIndex
        while (i < dataSize + isLastBlock) {
            var end = i + 65535
            if (end >= dataSize) {
                writeBuffer[bitPosition / 8] = isLastBlock.toUByte()
                end = dataSize
            }
            bitPosition = writeFixedBlock(writeBuffer, bitPosition + 1, data.sliceArray(i until end))
            i += 65535
        }
        state.index = dataSize
    }
    writeBuffer.copyInto(output, destinationOffset = prefixSize)
    return output.sliceArray(0 until prefixSize + shiftToNextByte(bitPosition) + postfixSize)
}

internal fun deflateWithOptions(
    inputData: UByteArray,
    options: DeflateOptions = DeflateOptions(),
    prefixSize: Int,
    suffixSize: Int,
    deflateState: DeflateState? = null
): UByteArray {
    var workingState = deflateState
    var workingData = inputData

    if (workingState == null) {
        workingState = DeflateState(isLastChunk = 1)

        if (options.dictionary != null) {
            val dictionary = options.dictionary

            val combinedData = UByteArray(dictionary.size + inputData.size)

            dictionary.copyInto(combinedData, destinationOffset = 0)

            inputData.copyInto(combinedData, destinationOffset = dictionary.size)

            workingData = combinedData
            workingState.waitIndex = dictionary.size
        }
    }

    val compressionLevel = options.level

    val memoryUsage = if (options.mem == null) if (workingState.isLastChunk != 0) {
        ceil(max(8.0, min(13.0, ln(workingData.size.toDouble()))) * 1.5).toInt()
    } else {
        20
    }
    else options.mem + 12

    return deflate(
        workingData,
        compressionLevel,
        memoryUsage,
        prefixSize,
        suffixSize,
        workingState
    )
}
