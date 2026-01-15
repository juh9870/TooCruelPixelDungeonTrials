package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.isLevelBossOrSpecial

class ForcedPacifismBuff :
    Buff(),
    InvulnerabilityBuff {
    override fun attachTo(target: Char): Boolean {
        super.attachTo(target).let {
            if (!it) return false
            if (target.properties().contains(Char.Property.BOSS) ||
                target
                    .properties()
                    .contains(Char.Property.BOSS_MINION) ||
                target
                    .properties()
                    .contains(Char.Property.MINIBOSS)
            ) {
                return false
            }
            return true
        }
    }

    override fun act(): Boolean {
        if (isLevelBossOrSpecial()) {
            detach()
        }
        if (target.alignment != Char.Alignment.ENEMY) {
            detach()
        }
        spend(TICK)
        return true
    }

    override fun isInvulnerable(effect: Class<out Any>): Boolean = true
}
