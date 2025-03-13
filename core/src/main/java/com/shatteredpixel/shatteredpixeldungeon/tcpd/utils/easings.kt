package com.shatteredpixel.shatteredpixeldungeon.tcpd.utils

import kotlin.math.pow

fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c2 = c1 * 1.525f

    return if (x < 0.5f)
        ((2 * x).pow(2.0f) * ((c2 + 1) * 2 * x - c2)) / 2
    else
        ((2 * x - 2).pow(2.0f) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2
}