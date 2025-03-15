package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.transmuteInventoryItem
import com.watabou.utils.Bundle
import com.watabou.utils.Random

class ControlledRandomness : Buff(), DefenseProcBuff {
    private var rerollChance = CHANCE_BASE

    override fun attachTo(target: Char?): Boolean {
        if (target !is Hero) {
            return false
        }
        return super.attachTo(target)
    }

    override fun defenseProc(enemy: Char, damage: Int) {
        if (Random.Float() < rerollChance) {
            rerollChance = CHANCE_BASE
            val hero = target as Hero

            if (hero.belongings.ring() != null) {
                transmuteInventoryItem(hero.belongings.ring(), hero)
            }
            if (hero.belongings.misc() != null) {
                transmuteInventoryItem(hero.belongings.misc(), hero)
            }
            if (hero.belongings.artifact() != null) {
                transmuteInventoryItem(hero.belongings.artifact(), hero)
            }
        } else {
            rerollChance += CHANCE_INCREMENT
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(REROLL_CHANCE, rerollChance)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        rerollChance = bundle.getFloat(REROLL_CHANCE)
    }

    companion object {
        private const val CHANCE_INCREMENT = 0.05f
        private const val CHANCE_BASE = 0.05f

        private const val REROLL_CHANCE = "reroll_chance"
    }
}
