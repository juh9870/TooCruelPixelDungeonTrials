package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite

class UninspiredToLearnBuff :
    Buff(),
    XpMultiplierBuff,
    OnBossSlainBuff {
    override fun xpMultiplier(source: Class<*>): Float =
        if (Char::class.java.isAssignableFrom(source)) {
            0f
        } else {
            1f
        }

    override fun onBossSlain() {
        val hero = target as? Hero ?: return
        // full free level of xp
        val expToGive = hero.maxExp()
        hero.sprite.showStatusWithIcon(
            CharSprite.POSITIVE,
            expToGive.toString(),
            FloatingText.EXPERIENCE,
        )
        hero.earnExp(expToGive, PotionOfExperience::class.java)
    }
}
