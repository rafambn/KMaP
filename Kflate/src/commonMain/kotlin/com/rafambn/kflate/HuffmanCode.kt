@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate

internal data class HuffmanTable(
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

internal fun generateHuffmanTable(extraBits: UByteArray, startValue: Int): HuffmanTable {
    val baseLengths = UShortArray(31)
    var currentStart = startValue
    for (i in 0 until 31) {
        val extraBit = if (i == 0) 0 else extraBits[i - 1].toInt()
        currentStart += 1 shl extraBit
        baseLengths[i] = currentStart.toUShort()
    }

    val reverseLookup = IntArray(baseLengths[30].toInt())
    for (i in 1 until 30) {
        for (j in baseLengths[i].toInt() until baseLengths[i + 1].toInt()) {
            reverseLookup[j] = ((j - baseLengths[i].toInt()) shl 5) or i
        }
    }
    return HuffmanTable(baseLengths, reverseLookup)
}

internal fun createHuffmanTree(codeLengths: UByteArray, maxBits: Int, isReversed: Boolean): UShortArray {
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
                    codes[REVERSE_TABLE[value].toInt() shr reverseBits] = symbolAndBits.toUShort()
                    value++
                }
            }
        }
    } else {
        codes = UShortArray(codeLengthSize)
        for (i in 0 until codeLengthSize) {
            if (codeLengths[i].toInt() != 0) {
                val codeLength = codeLengths[i].toInt()
                val currentCode = minCodes[codeLength - 1]
                minCodes[codeLength - 1]++
                codes[i] = (REVERSE_TABLE[currentCode].toInt() shr (15 - codeLength)).toUShort()
            }
        }
    }
    return codes
}

internal data class HuffmanNode(
    val symbol: Int,
    val frequency: Int,
    var leftChild: HuffmanNode? = null,
    var rightChild: HuffmanNode? = null
)

internal data class HuffmanTreeResult(val tree: UByteArray, val maxBits: Int) {
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

internal fun buildHuffmanTreeFromFrequencies(frequencies: UShortArray, maxBits: Int): HuffmanTreeResult {
    val nodes = mutableListOf<HuffmanNode>()
    for (i in frequencies.indices) {
        if (frequencies[i] > 0u) {
            nodes.add(HuffmanNode(symbol = i, frequency = frequencies[i].toInt()))
        }
    }

    val nodeCount = nodes.size
    val originalNodes = ArrayList(nodes)

    if (nodeCount == 0) {
        return HuffmanTreeResult(UByteArray(0), 0)
    }
    if (nodeCount == 1) {
        val codeLengths = UByteArray(nodes[0].symbol + 1)
        codeLengths[nodes[0].symbol] = 1u
        return HuffmanTreeResult(codeLengths, 1)
    }

    val maxSymbol = originalNodes.maxOf { it.symbol }
    val codeLengths = UShortArray(maxSymbol + 1)

    nodes.sortBy { it.frequency }

    val combinedNodes = ArrayList(nodes)
    combinedNodes.add(HuffmanNode(symbol = -1, frequency = 25001))

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
        val node1 =
            if (combinedNodes[lowFreqIndex].frequency < combinedNodes[combinedIndex].frequency) combinedNodes[lowFreqIndex++] else combinedNodes[combinedIndex++]
        val node2 =
            if (lowFreqIndex != highFreqIndex && combinedNodes[lowFreqIndex].frequency < combinedNodes[combinedIndex].frequency) combinedNodes[lowFreqIndex++] else combinedNodes[combinedIndex++]
        combinedNodes[highFreqIndex++] = HuffmanNode(
            symbol = -1,
            frequency = node1.frequency + node2.frequency,
            leftChild = node1,
            rightChild = node2
        )
    }

    var currentMaxBits = assignCodeLengthsAndGetMaxDepth(combinedNodes[highFreqIndex - 1], codeLengths, 0)

    if (currentMaxBits > maxBits) {
        var debt = 0
        val costShift = currentMaxBits - maxBits
        val cost = 1 shl costShift

        originalNodes.sortWith(compareByDescending<HuffmanNode> { codeLengths[it.symbol].toInt() }
            .thenBy { it.frequency })

        var i = 0
        for (nodeIndex in 0 until nodeCount) {
            val symbol = originalNodes[nodeIndex].symbol
            if (codeLengths[symbol] > maxBits.toUShort()) {
                debt += cost - (1 shl (currentMaxBits - codeLengths[symbol].toInt()))
                codeLengths[symbol] = maxBits.toUShort()
            } else {
                i = nodeIndex
                break
            }
        }

        debt = debt shr costShift

        while (debt > 0) {
            val symbol = originalNodes[i].symbol
            if (codeLengths[symbol] < maxBits.toUShort()) {
                debt -= 1 shl (maxBits - codeLengths[symbol].toInt() - 1)
                codeLengths[symbol]++
            } else {
                i++
            }
        }

        i = nodeCount - 1
        while (i >= 0 && debt != 0) {
            val symbol = originalNodes[i].symbol
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

internal fun assignCodeLengthsAndGetMaxDepth(node: HuffmanNode, lengths: UShortArray, depth: Int): Int {
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

internal fun generateLengthCodes(codeLengths: UByteArray): Pair<UShortArray, Int> {
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

internal fun calculateCodeLength(codeFrequencies: UShortArray, codeLengths: UByteArray): Int {
    var length = 0
    for (i in codeLengths.indices) {
        length += codeFrequencies[i].toInt() * codeLengths[i].toInt()
    }
    return length
}
