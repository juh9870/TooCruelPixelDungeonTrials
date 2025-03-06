package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Arrowhead
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Intoxication
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.PermaBlind
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.RacingTheDeath

fun Hero.heroLiveHook() {
    if(Modifier.RACING_THE_DEATH.active()) {
        Buff.affect(this, RacingTheDeath::class.java)
    }
    if(Modifier.THUNDERSTRUCK.active()) {
        Buff.affect(this, Arrowhead::class.java).set(9001)
    }
    if(Modifier.BLINDNESS.active()) {
        Buff.affect(this, PermaBlind::class.java)
    }
    if(Modifier.TOXIC_WATER.active()) {
        Buff.affect(this, Intoxication.ToxicWaterTracker::class.java)
    }
}
fun Hero.heroSpendConstantHook(time: Float) {
    if(time > 0) {
        buff(RacingTheDeath::class.java)?.tick()
    }
}