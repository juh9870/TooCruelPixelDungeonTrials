package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff

class ModifiersAppliedTracker : Buff() {
    init {
        revivePersists = true
    }
}
