package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.scalingChapter

class BlessingWorthyBuff :
    Buff(),
    OnBossSlainBuff {
    override fun onBossSlain() {
        val hero = target as? Hero ?: return
        PotionOfHealing.heal(hero)
        PotionOfCleansing.cleanse(target)

        prolong(hero, Bless::class.java, 100f)

        if (Modifier.SAFETY_BUFFER.active()) {
            affect(hero, SafetyBuffer::class.java)
                .refresh()
                .set(SafetyBuffer.SMITE_DAMAGE * (scalingChapter() + 1))
        }
    }
}
