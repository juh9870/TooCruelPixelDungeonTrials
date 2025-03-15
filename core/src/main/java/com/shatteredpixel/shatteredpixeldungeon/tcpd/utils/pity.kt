package com.shatteredpixel.shatteredpixeldungeon.tcpd.utils

import com.watabou.utils.Random

class PityRandom(val baseChance: Float, val increase: Float) {
    var chance = baseChance

    fun roll(): Boolean {
        return if (Random.Float() < chance) {
            chance = baseChance
            true
        } else {
            chance += increase
            false
        }
    }
}