package com.shatteredpixel.shatteredpixeldungeon.tcpd.utils

import kotlin.math.pow

fun easeInOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c2 = c1 * 1.525f

    return if (x < 0.5f)
        ((2 * x).pow(2.0f) * ((c2 + 1) * 2 * x - c2)) / 2
    else
        ((2 * x - 2).pow(2.0f) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2
}

fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1

    return 1 + c3 * (x - 1).pow(3f) + c1 * (x - 1).pow(2f)
}