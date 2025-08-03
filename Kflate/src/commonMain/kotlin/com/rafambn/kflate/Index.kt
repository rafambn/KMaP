@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * For a given length symbol, this table stores the number of extra bits to read
 * to determine the final match length.
 */
val FIXED_LENGTH_EXTRA_BITS = ubyteArrayOf(
    0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u, 1u, 1u, 1u, 1u, 2u, 2u, 2u, 2u, 3u, 3u, 3u, 3u, 4u, 4u, 4u, 4u, 5u, 5u, 5u, 5u, 0u, 0u, 0u, 0u
)

/**
 * For a given distance symbol, this table stores the number of extra bits to read
 * to determine the final match distance.
 */
val FIXED_DISTANCE_EXTRA_BITS = ubyteArrayOf(
    0u, 0u, 0u, 0u, 1u, 1u, 2u, 2u, 3u, 3u, 4u, 4u, 5u, 5u, 6u, 6u, 7u, 7u, 8u, 8u, 9u, 9u, 10u, 10u, 11u, 11u, 12u, 12u, 13u, 13u, 0u, 0u
)

/**
 * Maps the order of code length codes in the stream to their actual values (0-18).
 * Per RFC 1951, section 3.2.7.
 */
val CODE_LENGTH_INDEX_MAP = ubyteArrayOf(
    16u, 17u, 18u, 0u, 8u, 7u, 9u, 6u, 10u, 5u, 11u, 4u, 12u, 3u, 13u, 2u, 14u, 1u, 15u
)

data class HuffmanTable(
    val baseLengths: UShortArray,
    val reverseLookup: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HuffmanTable

        if (!baseLengths.contentEquals(other.baseLengths)) return false
        if (!reverseLookup.contentEquals(other.reverseLookup)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = baseLengths.contentHashCode()
        result = 31 * result + reverseLookup.contentHashCode()
        return result
    }
}

// get base, reverse index map from extra bits
fun generateHuffmanTable(extraBits: UByteArray, startValue: Int): HuffmanTable {
    val baseLengths = UShortArray(31)
    var currentStart = startValue
    for (i in 0 until 31) {
        val extraBit = if (i == 0) 0 else extraBits[i - 1].toInt()
        currentStart += 1 shl extraBit
        baseLengths[i] = currentStart.toUShort()
    }

    // numbers here are at max 18 bits
    val reverseLookup = IntArray(baseLengths[30].toInt())
    for (i in 1 until 30) {
        for (j in baseLengths[i].toInt() until baseLengths[i + 1].toInt()) {
            reverseLookup[j] = ((j - baseLengths[i].toInt()) shl 5) or i
        }
    }
    return HuffmanTable(baseLengths, reverseLookup)
}

val fixedLengthBase = generateHuffmanTable(FIXED_LENGTH_EXTRA_BITS, 2).baseLengths.apply {
    this[28] = 258u
}
val fixedLengthReverseLookup = generateHuffmanTable(FIXED_LENGTH_EXTRA_BITS, 2).reverseLookup.apply {
    this[258] = 28
}
val fixedDistanceBase = generateHuffmanTable(FIXED_DISTANCE_EXTRA_BITS, 0).baseLengths
val fixedDistanceReverseLookup = generateHuffmanTable(FIXED_DISTANCE_EXTRA_BITS, 0).reverseLookup

val reverseTable = UShortArray(32768).apply {
    for (value in this.indices) {
        var reversedBits = ((value and 0xAAAA) shr 1) or ((value and 0x5555) shl 1)
        reversedBits = ((reversedBits and 0xCCCC) shr 2) or ((reversedBits and 0x3333) shl 2)
        reversedBits = ((reversedBits and 0xF0F0) shr 4) or ((reversedBits and 0x0F0F) shl 4)
        this[value] = ((((reversedBits and 0xFF00) shr 8) or ((reversedBits and 0x00FF) shl 8)) shr 1).toUShort()
    }
}

fun createHuffmanTree(codeLengths: UByteArray, maxBits: Int, isReversed: Boolean): UShortArray {
    val codeLengthSize = codeLengths.size
    val lengths = UShortArray(maxBits)

    for (i in 0 until codeLengthSize) {
        if (codeLengths[i].toInt() != 0) {
            lengths[codeLengths[i].toInt() - 1]++
        }
    }

    val minCodes = IntArray(maxBits)
    for (i in 1 until maxBits) {
        minCodes[i] = (minCodes[i - 1] + lengths[i - 1].toInt()) shl 1
    }

    val codes: UShortArray
    if (isReversed) {
        codes = UShortArray(1 shl maxBits)
        val reverseBits = 15 - maxBits

        for (i in 0 until codeLengthSize) {
            if (codeLengths[i].toInt() != 0) {
                val codeLength = codeLengths[i].toInt()
                val symbolAndBits = (i shl 4) or codeLength
                val remainingBits = maxBits - codeLength

                val startValue = minCodes[codeLength - 1]
                minCodes[codeLength - 1]++
                var value = startValue shl remainingBits

                val endValue = value or ((1 shl remainingBits) - 1)
                while (value <= endValue) {
                    codes[reverseTable[value].toInt() shr reverseBits] = symbolAndBits.toUShort()
                    value++
                }
            }
        }
    } else {
        codes = UShortArray(codeLengthSize)
        for (i in 0 until codeLengthSize) {
            if (codeLengths[i].toInt() != 0) {
                val codeLength = codeLengths[i].toInt()
                // Fix: Get current value, then increment
                val currentCode = minCodes[codeLength - 1]
                minCodes[codeLength - 1]++
                codes[i] = (reverseTable[currentCode].toInt() shr (15 - codeLength)).toUShort()
            }
        }
    }
    return codes
}

// fixed length tree
val fixedLengthTree = UByteArray(288).apply {
    for (i in 0 until 144) this[i] = 8u
    for (i in 144 until 256) this[i] = 9u
    for (i in 256 until 280) this[i] = 7u
    for (i in 280 until 288) this[i] = 8u
}

// fixed distance tree
val fixedDistanceTree = UByteArray(32).apply {
    for (i in 0 until 32) this[i] = 5u
}

val fixedLengthMap = createHuffmanTree(fixedLengthTree, 9, false)
val fixedDistanceMap = createHuffmanTree(fixedDistanceTree, 5, false)

fun findMaxValue(array: UByteArray): UByte {
    var maxValue = array[0]
    for (index in 1 until array.size) {
        if (array[index] > maxValue) maxValue = array[index]
    }
    return maxValue
}

fun readBits(data: UByteArray, bitPosition: Int, bitMask: Int): Int {
    val byteOffset = bitPosition / 8
    return ((data[byteOffset].toInt() and 0xFF) or
            ((data[byteOffset + 1].toInt() and 0xFF) shl 8)) shr (bitPosition and 7) and bitMask
}

fun readBits16(data: UByteArray, bitPosition: Int): Int {
    val byteOffset = bitPosition / 8
    return ((data[byteOffset].toInt() and 0xFF) or
            ((data[byteOffset + 1].toInt() and 0xFF) shl 8) or
            ((data[byteOffset + 2].toInt() and 0xFF) shl 16)) shr (bitPosition and 7)
}

fun shiftToNextByte(bitPosition: Int): Int {
    return (bitPosition + 7) / 8
}

fun sliceArray(sourceArray: UByteArray, startIndex: Int, endIndex: Int? = null): UByteArray {
    val start = maxOf(0, startIndex)
    val end = endIndex ?: sourceArray.size
    val actualEnd = minOf(end, sourceArray.size)
    return sourceArray.copyOfRange(start, actualEnd)
}

data class InflateState(
    var literalMap: UShortArray? = null,
    var distanceMap: UShortArray? = null,
    var literalBits: Int? = null,
    var distanceBits: Int? = null,
    var finalFlag: Int? = null,
    var position: Int? = null,
    var byte: Int? = null,
    var lastCheck: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InflateState

        if (literalBits != other.literalBits) return false
        if (distanceBits != other.distanceBits) return false
        if (finalFlag != other.finalFlag) return false
        if (position != other.position) return false
        if (byte != other.byte) return false
        if (lastCheck != other.lastCheck) return false
        if (!literalMap.contentEquals(other.literalMap)) return false
        if (!distanceMap.contentEquals(other.distanceMap)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = literalBits ?: 0
        result = 31 * result + (distanceBits ?: 0)
        result = 31 * result + (finalFlag ?: 0)
        result = 31 * result + (position ?: 0)
        result = 31 * result + (byte ?: 0)
        result = 31 * result + lastCheck
        result = 31 * result + (literalMap?.contentHashCode() ?: 0)
        result = 31 * result + (distanceMap?.contentHashCode() ?: 0)
        return result
    }
}

enum class FlateErrorCode(val code: Int, val message: String? = null) {
    UNEXPECTED_EOF(0),
    INVALID_BLOCK_TYPE(1),
    INVALID_LENGTH_LITERAL(2),
    INVALID_DISTANCE(3),
    STREAM_FINISHED(4),
    NO_STREAM_HANDLER(5),
    INVALID_HEADER(6),
    NO_CALLBACK(7),
    INVALID_UTF8(8),
    EXTRA_FIELD_TOO_LONG(9),
    INVALID_DATE(10),
    FILENAME_TOO_LONG(11),
    STREAM_FINISHING(12),
    INVALID_ZIP_DATA(13),
    UNKNOWN_COMPRESSION_METHOD(14)
}

private val errorMessages = listOf(
    "unexpected EOF",
    "invalid block type",
    "invalid length/literal",
    "invalid distance",
    "stream finished",
    "no stream handler",
    null,
    "no callback",
    "invalid UTF-8 data",
    "extra field too long",
    "date not in range 1980-2099",
    "filename too long",
    "stream finishing",
    "invalid zip data",
    null
)

class FlateError(code: Int, message: String = "") : Exception(
    if (message.isNotEmpty()) message
    else if (code < errorMessages.size) errorMessages[code]
    else "Unknown error code: $code"
)

fun createFlateError(errorCode: Int): Nothing {
    throw FlateError(errorCode)
}

fun inflate(
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
            // Read block header
            isFinalBlock = readBits(inputData, currentBitPosition, 1)
            val blockType = readBits(inputData, currentBitPosition + 1, 3)
            currentBitPosition += 3

            when (blockType) {
                0 -> { // No compression - stored block
                    val blockStartByte = shiftToNextByte(currentBitPosition) + 4
                    val blockLength = (inputData[blockStartByte - 4].toInt() and 0xFF) or
                            ((inputData[blockStartByte - 3].toInt() and 0xFF) shl 8)
                    val blockEndByte = blockStartByte + blockLength

                    if (blockEndByte > sourceLength) {
                        if (hasNoStoredState != 0) createFlateError(FlateErrorCode.UNEXPECTED_EOF.code)
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

                1 -> { // Fixed Huffman codes
                    literalLengthMap = fixedLengthMap
                    distanceMap = fixedDistanceMap
                    literalMaxBits = 9
                    distanceMaxBits = 5
                }

                2 -> { // Dynamic Huffman codes
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

                    // Decode literal/length and distance code lengths
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

                    // Split into literal/length and distance trees
                    val literalLengthCodeLengths = allCodeLengths.copyOfRange(0, numLiteralCodes)
                    val distanceCodeLengths = allCodeLengths.copyOfRange(numLiteralCodes, totalCodes)

                    literalMaxBits = findMaxValue(literalLengthCodeLengths).toInt()
                    distanceMaxBits = findMaxValue(distanceCodeLengths).toInt()

                    literalLengthMap = createHuffmanTree(literalLengthCodeLengths, literalMaxBits, true)
                    distanceMap = createHuffmanTree(distanceCodeLengths, distanceMaxBits, true)
                }

                else -> createFlateError(FlateErrorCode.INVALID_BLOCK_TYPE.code)
            }

            if (currentBitPosition > totalAvailableBits) {
                if (hasNoStoredState != 0) createFlateError(FlateErrorCode.UNEXPECTED_EOF.code)
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
                if (hasNoStoredState != 0) createFlateError(FlateErrorCode.UNEXPECTED_EOF.code)
                break
            }

            if (literalCode.toInt() == 0) createFlateError(FlateErrorCode.INVALID_LENGTH_LITERAL.code)

            when {
                symbol < 256 -> {
                    // Literal byte
                    workingBuffer[bytesWrittenToOutput++] = symbol.toUByte()
                }

                symbol == 256 -> {
                    // End of block
                    savedBitPosition = currentBitPosition
                    literalLengthMap = null
                    break
                }

                else -> {
                    // Length/distance pair
                    var matchLength = symbol - 254

                    // Handle extra length bits
                    if (symbol > 264) {
                        val lengthIndex = symbol - 257
                        val extraBits = FIXED_LENGTH_EXTRA_BITS[lengthIndex]
                        matchLength =
                            readBits(inputData, currentBitPosition, (1 shl extraBits.toInt()) - 1) + fixedLengthBase[lengthIndex].toInt()
                        currentBitPosition += extraBits.toInt()
                    }

                    // Decode distance
                    val distanceCode = distanceMap!![readBits16(inputData, currentBitPosition) and distanceBitMask]
                    val distanceSymbol = distanceCode.toInt() shr 4
                    if (distanceCode.toInt() == 0) createFlateError(FlateErrorCode.INVALID_DISTANCE.code)
                    currentBitPosition += (distanceCode.toInt() and 15)

                    var matchDistance = fixedDistanceBase[distanceSymbol].toInt()
                    if (distanceSymbol > 3) {
                        val extraBits = FIXED_DISTANCE_EXTRA_BITS[distanceSymbol].toInt()
                        matchDistance += readBits16(inputData, currentBitPosition) and ((1 shl extraBits - 1))
                        currentBitPosition += extraBits
                    }

                    if (currentBitPosition > totalAvailableBits)
                        createFlateError(FlateErrorCode.UNEXPECTED_EOF.code)

                    if (needsResize) ensureBufferCapacity(bytesWrittenToOutput + 131072)

                    val copyEndIndex = bytesWrittenToOutput + matchLength

                    if (bytesWrittenToOutput < matchDistance) {
                        val dictionaryOffset = dictionaryLength - matchDistance
                        val dictionaryEndIndex = minOf(matchDistance, copyEndIndex)
                        if (dictionaryOffset + bytesWrittenToOutput < 0) {
                            createFlateError(FlateErrorCode.INVALID_DISTANCE.code)
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

fun writeBits(data: UByteArray, bitPosition: Int, value: Int) {
    val shiftedValue = value shl (bitPosition and 7)
    val byteIndex = bitPosition / 8
    data[byteIndex] = data[byteIndex] or shiftedValue.toUByte()
    data[byteIndex + 1] = data[byteIndex + 1] or (shiftedValue shr 8).toUByte()
}

fun writeBits16(data: UByteArray, bitPosition: Int, value: Int) {
    val shiftedValue = value shl (bitPosition and 7)
    val byteIndex = bitPosition / 8
    data[byteIndex] = data[byteIndex] or shiftedValue.toUByte()
    data[byteIndex + 1] = data[byteIndex + 1] or (shiftedValue shr 8).toUByte()
    data[byteIndex + 2] = data[byteIndex + 2] or (shiftedValue shr 16).toUByte()
}

private data class HuffmanNode(
    val symbol: Int,
    val frequency: Int,
    var leftChild: HuffmanNode? = null,
    var rightChild: HuffmanNode? = null
)

private data class HuffmanTreeResult(val tree: UByteArray, val maxBits: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HuffmanTreeResult

        if (maxBits != other.maxBits) return false
        if (!tree.contentEquals(other.tree)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxBits
        result = 31 * result + tree.contentHashCode()
        return result
    }
}

private fun buildHuffmanTreeFromFrequencies(frequencies: UShortArray, maxBits: Int): HuffmanTreeResult {
    val nodes = mutableListOf<HuffmanNode>()
    for (i in frequencies.indices) {
        if (frequencies[i] > 0u) {
            nodes.add(HuffmanNode(symbol = i, frequency = frequencies[i].toInt()))
        }
    }

    val nodeCount = nodes.size
    if (nodeCount == 0) {
        return HuffmanTreeResult(UByteArray(0), 0)
    }
    if (nodeCount == 1) {
        val codeLengths = UByteArray(nodes[0].symbol + 1)
        codeLengths[nodes[0].symbol] = 1u
        return HuffmanTreeResult(codeLengths, 1)
    }

    nodes.sortBy { it.frequency }

    val combinedNodes = ArrayList(nodes)
    combinedNodes.add(HuffmanNode(symbol = -1, frequency = 25001)) // Sentinel value

    var lowFreqIndex = 0
    var highFreqIndex = 1
    var combinedIndex = 2

    val firstNode = combinedNodes[0]
    val secondNode = combinedNodes[1]
    combinedNodes[0] = HuffmanNode(
        symbol = -1,
        frequency = firstNode.frequency + secondNode.frequency,
        leftChild = firstNode,
        rightChild = secondNode
    )

    while (highFreqIndex != nodeCount - 1) {
        val node1 = if (combinedNodes[lowFreqIndex].frequency < combinedNodes[combinedIndex].frequency) combinedNodes[lowFreqIndex++] else combinedNodes[combinedIndex++]
        val node2 = if (lowFreqIndex != highFreqIndex && combinedNodes[lowFreqIndex].frequency < combinedNodes[combinedIndex].frequency) combinedNodes[lowFreqIndex++] else combinedNodes[combinedIndex++]
        combinedNodes[highFreqIndex++] = HuffmanNode(
            symbol = -1,
            frequency = node1.frequency + node2.frequency,
            leftChild = node1,
            rightChild = node2
        )
    }

    val maxSymbol = nodes.maxOf { it.symbol }
    val codeLengths = UShortArray(maxSymbol + 1)
    var currentMaxBits = assignCodeLengthsAndGetMaxDepth(combinedNodes[highFreqIndex - 1], codeLengths, 0)

    if (currentMaxBits > maxBits) {
        var debt = 0
        val costShift = currentMaxBits - maxBits
        val cost = 1 shl costShift

//        nodes.sortByDescending { codeLengths[it.symbol].toInt() }
        nodes.sortWith(compareByDescending<HuffmanNode> { codeLengths[it.symbol].toInt() }.thenBy { it.frequency })

        for (i in 0 until nodeCount) {
            val symbol = nodes[i].symbol
            if (codeLengths[symbol] > maxBits.toUShort()) {
                debt += cost - (1 shl (currentMaxBits - codeLengths[symbol].toInt()))
                codeLengths[symbol] = maxBits.toUShort()
            } else {
                break
            }
        }

        debt = debt shr costShift

        var i = nodeCount - 1
        while (debt > 0) {
            val symbol = nodes[i].symbol
            if (codeLengths[symbol] < maxBits.toUShort()) {
                debt -= 1 shl (maxBits - codeLengths[symbol].toInt() - 1)
                codeLengths[symbol]++
            } else {
                i++
            }
        }

        i = nodeCount - 1
        while (i >= 0 && debt != 0) {
            val symbol = nodes[i].symbol
            if (codeLengths[symbol] == maxBits.toUShort()) {
                codeLengths[symbol]--
                debt++
            }
            i--
        }
        currentMaxBits = maxBits
    }

    return HuffmanTreeResult(UByteArray(codeLengths.size) { codeLengths[it].toUByte() }, currentMaxBits)
}

private fun assignCodeLengthsAndGetMaxDepth(node: HuffmanNode, lengths: UShortArray, depth: Int): Int {
    return if (node.symbol != -1) {
        lengths[node.symbol] = depth.toUShort()
        depth
    } else {
        maxOf(
            assignCodeLengthsAndGetMaxDepth(node.leftChild!!, lengths, depth + 1),
            assignCodeLengthsAndGetMaxDepth(node.rightChild!!, lengths, depth + 1)
        )
    }
}

fun generateLengthCodes(codeLengths: UByteArray): Pair<UShortArray, Int> {
    var maxSymbol = codeLengths.size
    while (maxSymbol > 0 && codeLengths[maxSymbol - 1] == 0.toUByte()) {
        maxSymbol--
    }

    val compactCodes = UShortArray(maxSymbol)
    var compactCodeIndex = 0
    var currentCode = codeLengths[0]
    var runLength = 1

    val writeCode = { value: Int -> compactCodes[compactCodeIndex++] = value.toUShort() }

    for (i in 1..maxSymbol) {
        if (i < maxSymbol && codeLengths[i] == currentCode) {
            runLength++
        } else {
            if (currentCode.toInt() == 0 && runLength > 2) {
                while (runLength > 138) {
                    writeCode(32754)
                    runLength -= 138
                }
                if (runLength > 2) {
                    writeCode(if (runLength > 10) ((runLength - 11) shl 5) or 28690 else ((runLength - 3) shl 5) or 12305)
                    runLength = 0
                }
            } else if (runLength > 3) {
                writeCode(currentCode.toInt())
                runLength--
                while (runLength > 6) {
                    writeCode(8304)
                    runLength -= 6
                }
                if (runLength > 2) {
                    writeCode(((runLength - 3) shl 5) or 8208)
                    runLength = 0
                }
            }
            while (runLength-- > 0) {
                writeCode(currentCode.toInt())
            }
            runLength = 1
            if (i < maxSymbol) {
                currentCode = codeLengths[i]
            }
        }
    }
    return Pair(compactCodes.sliceArray(0 until compactCodeIndex), maxSymbol)
}

fun calculateCodeLength(codeFrequencies: UShortArray, codeLengths: UByteArray): Int {
    var length = 0
    for (i in codeLengths.indices) {
        length += codeFrequencies[i].toInt() * codeLengths[i].toInt()
    }
    return length
}

fun writeFixedBlock(output: UByteArray, bitPosition: Int, data: UByteArray): Int {
    val dataLength = data.size
    val bytePosition = shiftToNextByte(bitPosition + 2)
    output[bytePosition] = (dataLength and 255).toUByte()
    output[bytePosition + 1] = (dataLength shr 8).toUByte()
    output[bytePosition + 2] = (output[bytePosition].toInt() xor 255).toUByte()
    output[bytePosition + 3] = (output[bytePosition + 1].toInt() xor 255).toUByte()
    for (i in data.indices) {
        output[bytePosition + i + 4] = data[i]
    }
    return (bytePosition + 4 + dataLength) * 8
}

fun writeBlock(
    data: UByteArray,
    output: UByteArray,
    isFinal: Boolean,
    symbols: IntArray,
    literalFrequencies: UShortArray,
    distanceFrequencies: UShortArray,
    extraBits: Int,
    symbolCount: Int,
    blockStart: Int,
    blockLength: Int,
    bitPosition: Int
): Int {
    var currentBitPosition = bitPosition
    writeBits(output, currentBitPosition++, if (isFinal) 1 else 0)
    literalFrequencies[256]++

    val (dynamicLiteralTree, maxLiteralBits) = buildHuffmanTreeFromFrequencies(literalFrequencies, 15)
    val (dynamicDistanceTree, maxDistanceBits) = buildHuffmanTreeFromFrequencies(distanceFrequencies, 15)
    val (literalCodeLengths, numLiteralCodes) = generateLengthCodes(dynamicLiteralTree)
    val (distanceCodeLengths, numDistanceCodes) = generateLengthCodes(dynamicDistanceTree)

    val codeLengthFrequencies = UShortArray(19)
    for (i in literalCodeLengths.indices) {
        codeLengthFrequencies[(literalCodeLengths[i].toInt() and 31)]++
    }
    for (i in distanceCodeLengths.indices) {
        codeLengthFrequencies[(distanceCodeLengths[i].toInt() and 31)]++
    }

    val (codeLengthTree, maxCodeLengthBits) = buildHuffmanTreeFromFrequencies(codeLengthFrequencies, 7)
    var numCodeLengthCodes = 19
    while (numCodeLengthCodes > 4 && codeLengthTree[CODE_LENGTH_INDEX_MAP[numCodeLengthCodes - 1].toInt()].toInt() == 0) {
        numCodeLengthCodes--
    }

    val fixedBlockLength = (blockLength + 5) shl 3
    val fixedTypedLength = calculateCodeLength(literalFrequencies, fixedLengthTree) +
            calculateCodeLength(distanceFrequencies, fixedDistanceTree) + extraBits
    val dynamicTypedLength = calculateCodeLength(literalFrequencies, dynamicLiteralTree) +
            calculateCodeLength(distanceFrequencies, dynamicDistanceTree) + extraBits + 14 + 3 * numCodeLengthCodes +
            calculateCodeLength(codeLengthFrequencies, codeLengthTree) + 2 * codeLengthFrequencies[16].toInt() +
            3 * codeLengthFrequencies[17].toInt() + 7 * codeLengthFrequencies[18].toInt()

    if (blockStart >= 0 && fixedBlockLength <= fixedTypedLength && fixedBlockLength <= dynamicTypedLength) {
        return writeFixedBlock(output, currentBitPosition, data.sliceArray(blockStart until blockStart + blockLength))
    }

    var literalMap: UShortArray
    var literalLengths: UByteArray
    var distanceMap: UShortArray
    var distanceLengths: UByteArray

    writeBits(output, currentBitPosition, 1 + if (dynamicTypedLength < fixedTypedLength) 1 else 0)
    currentBitPosition += 2

    if (dynamicTypedLength < fixedTypedLength) {
        literalMap = createHuffmanTree(dynamicLiteralTree, maxLiteralBits, false)
        literalLengths = dynamicLiteralTree
        distanceMap = createHuffmanTree(dynamicDistanceTree, maxDistanceBits, false)
        distanceLengths = dynamicDistanceTree

        val codeLengthMap = createHuffmanTree(codeLengthTree, maxCodeLengthBits, false)
        writeBits(output, currentBitPosition, numLiteralCodes - 257)
        writeBits(output, currentBitPosition + 5, numDistanceCodes - 1)
        writeBits(output, currentBitPosition + 10, numCodeLengthCodes - 4)
        currentBitPosition += 14

        if (bitPosition >= 36400000)
            println("zika")
        for (i in 0 until numCodeLengthCodes) {
            writeBits(output, currentBitPosition + 3 * i, codeLengthTree[CODE_LENGTH_INDEX_MAP[i].toInt()].toInt())
        }
        currentBitPosition += 3 * numCodeLengthCodes

        val codeLengthTrees = arrayOf(literalCodeLengths, distanceCodeLengths)
        for (tree in codeLengthTrees) {
            for (i in tree.indices) {
                val len = tree[i].toInt() and 31
                writeBits(output, currentBitPosition, codeLengthMap[len].toInt())
                currentBitPosition += codeLengthTree[len].toInt()
                if (len > 15) {
                    writeBits(output, currentBitPosition, (tree[i].toInt() shr 5) and 127)
                    currentBitPosition += (tree[i].toInt() shr 12)
                }
            }
        }
    } else {
        literalMap = fixedLengthMap
        literalLengths = fixedLengthTree
        distanceMap = fixedDistanceMap
        distanceLengths = fixedDistanceTree
    }

    for (i in 0 until symbolCount) {
        val symbol = symbols[i]
        if (symbol > 255) {
            val lengthSymbol = (symbol shr 18) and 31
            writeBits16(output, currentBitPosition, literalMap[lengthSymbol + 257].toInt())
            currentBitPosition += literalLengths[lengthSymbol + 257].toInt()
            if (lengthSymbol > 7) {
                writeBits(output, currentBitPosition, (symbol shr 23) and 31)
                currentBitPosition += FIXED_LENGTH_EXTRA_BITS[lengthSymbol].toInt()
            }
            val distanceSymbol = symbol and 31
            writeBits16(output, currentBitPosition, distanceMap[distanceSymbol].toInt())
            currentBitPosition += distanceLengths[distanceSymbol].toInt()
            if (distanceSymbol > 3) {
                writeBits16(output, currentBitPosition, (symbol shr 5) and 8191)
                currentBitPosition += FIXED_DISTANCE_EXTRA_BITS[distanceSymbol].toInt()
            }
        } else {
            writeBits16(output, currentBitPosition, literalMap[symbol].toInt())
            currentBitPosition += literalLengths[symbol].toInt()
        }
    }

    writeBits16(output, currentBitPosition, literalMap[256].toInt())
    return currentBitPosition + literalLengths[256].toInt()
}

val DEFLATE_OPTIONS = intArrayOf(65540, 131080, 131088, 131104, 262176, 1048704, 1048832, 2114560, 2117632)

val EMPTY_BYTE_ARRAY = ubyteArrayOf()

data class DeflateState(
    var head: UShortArray? = null,
    var prev: UShortArray? = null,
    var index: Int = 0,
    var endIndex: Int = 0,
    var waitIndex: Int = 0,
    var remainderByteInfo: Int = 0,
    var isLastChunk: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DeflateState

        if (index != other.index) return false
        if (endIndex != other.endIndex) return false
        if (waitIndex != other.waitIndex) return false
        if (remainderByteInfo != other.remainderByteInfo) return false
        if (isLastChunk != other.isLastChunk) return false
        if (!head.contentEquals(other.head)) return false
        if (!prev.contentEquals(other.prev)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + endIndex
        result = 31 * result + waitIndex
        result = 31 * result + remainderByteInfo
        result = 31 * result + isLastChunk
        result = 31 * result + (head?.contentHashCode() ?: 0)
        result = 31 * result + (prev?.contentHashCode() ?: 0)
        return result
    }
}

fun deflate(
    data: UByteArray,
    level: Int,
    compressionLevel: Int,
    prefixSize: Int,
    postfixSize: Int,
    state: DeflateState
): UByteArray {
    val dataSize = state.endIndex.takeIf { it != 0 } ?: data.size
    val output = UByteArray(prefixSize + dataSize + 5 * (1 + (dataSize / 7000).toInt()) + postfixSize)
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
        val baseShift1 = (compressionLevel / 3).toInt()
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
                    symbols[symbolIndex++] = 268435456 or (fixedLengthReverseLookup[length] shl 18) or fixedDistanceReverseLookup[distance]
                    val lenIndex = fixedLengthReverseLookup[length] and 31
                    val distIndex = fixedDistanceReverseLookup[distance] and 31
                    extraBits += FIXED_LENGTH_EXTRA_BITS[lenIndex].toInt() + FIXED_DISTANCE_EXTRA_BITS[distIndex].toInt()
                    literalFrequencies[257 + lenIndex]++
                    distanceFrequencies[distIndex]++
                    waitIndex = i + length
                    literalCount++
                } else {
                    symbols[symbolIndex++] = data[i].toInt()
                    literalFrequencies[data[i].toInt()]++
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
    return output.sliceArray(0 until prefixSize + shiftToNextByte(bitPosition) + postfixSize)
}

fun deflateWithOptions(
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
    else options.mem +12

    return deflate(
        workingData,
        compressionLevel,
        memoryUsage,
        prefixSize,
        suffixSize,
        workingState
    )
}

fun readTwoBytes(data: UByteArray, offset: Int): Int {
    return (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
}

fun readFourBytes(data: UByteArray, offset: Int): Long {
    return (data[offset].toLong() and 0xFF) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)
}

fun readEightBytes(data: UByteArray, offset: Int): Long {
    return readFourBytes(data, offset) + (readFourBytes(data, offset + 4) * 4294967296L)
}

fun writeBytes(data: UByteArray, offset: Int, value: Long) {
    var v = value
    var i = offset
    while (v > 0) {
        data[i++] = (v and 0xFF).toUByte()
        v = v ushr 8
    }
}

fun writeGzipHeader(output: UByteArray, options: GzipOptions) {
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
    if (mtime != null && mtime is Number && mtime.toLong() != 0L) {
        writeBytes(output, 4, mtime.toLong() / 1000)
    }

    options.filename?.let {
        output[3] = 8u
        for (i in it.indices) {
            output[i + 10] = it[i].code.toUByte()
        }
        output[it.length + 10] = 0u
    }
}

fun writeGzipStart(data: UByteArray): Int {
    if (data[0].toInt() != 31 || data[1].toInt() != 139 || data[2].toInt() != 8) {
        createFlateError(FlateErrorCode.INVALID_HEADER.code)//TODO "invalid gzip data"
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

fun getGzipUncompressedSize(data: UByteArray): Long {
    val length = data.size
    return readFourBytes(data, length - 4)
}

fun getGzipHeaderSize(options: GzipOptions): Int {
    return 10 + if (options.filename != null) options.filename.length + 1 else 0
}

fun writeZlibHeader(output: UByteArray, options: DeflateOptions) {
    val level = options.level
    val compressionLevelFlag = when {
        level == 0 -> 0
        level < 6 -> 1
        level == 9 -> 3
        else -> 2
    }
    output[0] = 120u
    var headerByte1 = (compressionLevelFlag shl 6) or (if (options.dictionary != null) 32 else 0)
    headerByte1 = headerByte1 or (31 - ((output[0].toInt() shl 8) or headerByte1) % 31)
    output[1] = headerByte1.toUByte()

    options.dictionary?.let {
        val checksum = Adler32Checksum()
        checksum.update(it)
        writeBytes(output, 2, checksum.getChecksum().toLong())
    }
}

fun writeZlibStart(data: UByteArray, hasDictionary: Boolean): Int {
    val cmf = data[0].toInt()
    val flg = data[1].toInt()
    if ((cmf and 15) != 8 || (cmf ushr 4) > 7 || ((cmf shl 8 or flg) % 31) != 0) {
        createFlateError(FlateErrorCode.INVALID_HEADER.code) //TODO "invalid zlib data"
    }
    val needsDictionary = (flg and 32) != 0
    if (needsDictionary != hasDictionary) {
//        val message = "invalid zlib data: ${if (needsDictionary) "need" else "unexpected"} dictionary"
        createFlateError(FlateErrorCode.INVALID_HEADER.code) //TODO
    }
    return (if ((flg ushr 3 and 4) != 0) 4 else 0) + 2
}


interface ChecksumGenerator {
    fun update(data: UByteArray)
    fun getChecksum(): Int
}

val CRC32_TABLE = IntArray(256).apply {
    for (i in 0 until 256) {
        var c = i
        var k = 9
        while (--k > 0) {
            c = if (c and 1 != 0) -306674912 else 0 xor (c ushr 1)
        }
        this[i] = c
    }
}

class Crc32Checksum : ChecksumGenerator {
    private var crc = -1

    override fun update(data: UByteArray) {
        for (byte in data) {
            crc = CRC32_TABLE[(crc and 255) xor byte.toInt()] xor (crc ushr 8)
        }
    }

    override fun getChecksum(): Int {
        return crc.inv()
    }
}

class Adler32Checksum : ChecksumGenerator {
    private var a = 1
    private var b = 0

    override fun update(data: UByteArray) {
        val len = data.size
        var i = 0
        while (i < len) {
            val end = minOf(i + 2655, len)
            while (i < end) {
                b += a + data[i].toInt()
                i++
            }
            a = (a and 65535) + 15 * (a ushr 16)
            b = (b and 65535) + 15 * (b ushr 16)
        }
    }

    override fun getChecksum(): Int {
        a %= 65521
        b %= 65521
        return ((a and 255) shl 24) or ((a and 0xFF00) shl 8) or ((b and 255) shl 8) or (b ushr 8)
    }
}
