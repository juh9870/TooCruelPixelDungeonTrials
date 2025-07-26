package com.shatteredpixel.shatteredpixeldungeon.tcpd.utils

import com.watabou.utils.Random
import kotlin.math.ln

/**
 * Weight factor can be used for weighted shuffling
 *
 * Higher weight makes item more likely to appear earlier in the list during
 * ascending sort
 */
fun <T> weightedPair(
    weight: Float,
    item: T,
): Pair<Float, T> {
    val factor =
        if (weight == 0f) {
            Float.POSITIVE_INFINITY
        } else if (!weight.isFinite()) {
            0f
        } else {
            1f / weight * -ln(Random.Float())
        }
    return factor to item
}
