package com.shatteredpixel.shatteredpixeldungeon.tcpd.utils

data class UnorderedPair<T>(
    val a: T,
    val b: T,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnorderedPair<*>) return false
        return (a == other.a && b == other.b) || (a == other.b && b == other.a)
    }

    override fun hashCode(): Int {
        var aHash = a?.hashCode() ?: 0
        var bHash = b?.hashCode() ?: 0
        if (aHash > bHash) {
            // Swap to ensure aHash is always less than or equal to bHash
            val temp = aHash
            aHash = bHash
            bHash = temp
        }
        var result = aHash
        result = 31 * result + bHash
        return result
    }
}
