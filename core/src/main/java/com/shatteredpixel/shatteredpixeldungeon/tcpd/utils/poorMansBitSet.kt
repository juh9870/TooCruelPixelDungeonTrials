package com.shatteredpixel.shatteredpixeldungeon.tcpd.utils


fun ByteArray.asBits(): BooleanArray {
    val result = BooleanArray(this.size * 8)

    for (i in indices) {
        val index = i * 8
        val num = this[i].toInt()
        result[index + 0] = (num and 0x80) != 0
        result[index + 1] = (num and 0x40) != 0
        result[index + 2] = (num and 0x20) != 0
        result[index + 3] = (num and 0x10) != 0
        result[index + 4] = (num and 0x8) != 0
        result[index + 5] = (num and 0x4) != 0
        result[index + 6] = (num and 0x2) != 0
        result[index + 7] = (num and 0x1) != 0
    }

    return result
}

/**
 * This will pad to the nearest number of bytes. So the last few booleans will be set to the padValue.
 * Eg: If there are 9 booleans, then the last 7 will be added as the padValue (making 16 booleans).
 *
 * @param padValue
 * @return
 */
fun BooleanArray.asBytes(padValue: Boolean): ByteArray {
    val paddedBooleans: BooleanArray
    val remainder = this.size % 8

    // Booleans are already divisible by 8, nothing to pad
    if (remainder == 0) {
        paddedBooleans = this
    } else {
        val padAmount = 8 - remainder
        paddedBooleans = this.copyOf(this.size + padAmount)

        // Pad with the padValue
        if (padValue) {
            for (i in this.size until paddedBooleans.size) {
                paddedBooleans[i] = true
            }
        }
    }

    // Convert the boolean array into a byte array
    val result = ByteArray(paddedBooleans.size / 8)

    for (i in result.indices) {
        val index = i * 8
        val b = ((if (paddedBooleans[index + 0]) 1 shl 7 else 0) +
                (if (paddedBooleans[index + 1]) 1 shl 6 else 0) +
                (if (paddedBooleans[index + 2]) 1 shl 5 else 0) +
                (if (paddedBooleans[index + 3]) 1 shl 4 else 0) +
                (if (paddedBooleans[index + 4]) 1 shl 3 else 0) +
                (if (paddedBooleans[index + 5]) 1 shl 2 else 0) +
                (if (paddedBooleans[index + 6]) 1 shl 1 else 0) +
                (if (paddedBooleans[index + 7]) 1 else 0)).toByte()
        result[i] = b
    }

    return result
}

/**
 * This will return a new byte array with all the trailing 0 bytes removed
 */
fun ByteArray.trimEnd(): ByteArray {
    for (i in this.size - 1 downTo 0) {
        if (this[i] == 0.toByte()) {
            continue
        }
        return this.copyOf(i + 1)
    }
    return this.copyOf()
}

/**
 * This will return a new boolean array with removed trailing values
 */
fun BooleanArray.trimEnd(toRemove: Boolean = false): BooleanArray {
    for (i in this.size - 1 downTo 0) {
        if (this[i] == toRemove) {
            continue
        }
        return this.copyOf(i + 1)
    }
    return this.copyOf()
}