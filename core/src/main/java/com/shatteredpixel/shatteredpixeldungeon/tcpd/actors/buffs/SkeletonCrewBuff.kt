package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff

class SkeletonCrewBuff :
    Buff(),
    XpMultiplierBuff {
    override fun xpMultiplier(source: Any?): Float = 2f
}
