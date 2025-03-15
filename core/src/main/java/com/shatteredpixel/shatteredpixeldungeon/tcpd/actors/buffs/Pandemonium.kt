package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.items.Generator
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.showItemTransmuted
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.transmuteInventoryItem
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.PityRandom
import com.watabou.utils.Bundle
import com.watabou.utils.DeviceCompat

class Pandemonium : Buff(), AttackProcBuff {
    private var weaponRandom =
        PityRandom(if (DeviceCompat.isDebug()) 1f else WEP_CHANCE_BASE, WEP_CHANCE_INCREMENT)
    private var wandRandom =
        PityRandom(if (DeviceCompat.isDebug()) 1f else RANGED_CHANCE_BASE, RANGED_CHANCE_INCREMENT)
    private var misRandom =
        PityRandom(if (DeviceCompat.isDebug()) 1f else RANGED_CHANCE_BASE, RANGED_CHANCE_INCREMENT)

    override fun attachTo(target: Char?): Boolean {
        if (target !is Hero) {
            return false
        }
        return super.attachTo(target)
    }

    fun wandUsed(wand: Wand) {
        if (wandRandom.roll()) {
            val hero = target as Hero
            val mageStaff = hero.belongings.getItem(MagesStaff::class.java)
            if (mageStaff != null && mageStaff.wand == wand) {
                val newWand = ScrollOfTransmutation.changeItem(wand) as Wand
                newWand.level(0)
                newWand.identify()
                newWand.curCharges = 0
                showItemTransmuted(hero, wand, newWand)
                mageStaff.imbueWand(newWand, null)
            } else {
                transmuteInventoryItem(wand, hero)
            }
        }
    }

    fun missileUsed(missile: MissileWeapon): MissileWeapon {
        if (misRandom.roll()) {
            val wep = ScrollOfTransmutation.changeItem(missile) as MissileWeapon?
            if (wep != null) {
                showItemTransmuted(target as Hero, missile, wep)
                return wep
            }
        }
        return missile
    }

    fun spiritBowUsed(bow: SpiritBow) {
        if (misRandom.roll()) {
            val hero = target as Hero
            val mis = Generator.randomMissile()
            mis.quantity(1)
            bow.detach(hero.belongings.backpack)
            mis.collect()
            mis.upgrade(bow.level())
            if (bow.enchantment != null) {
                mis.enchant()
            }
            showItemTransmuted(hero, bow, mis)
        }
    }

    override fun attackProc(enemy: Char, damage: Int) {
        val hero = target as Hero

        if (weaponRandom.roll()) {
            val weapon = hero.belongings.attackingWeapon()
            if (weapon is MissileWeapon) {
                return // Missile weapons have their own reroll logic
            }
            // Ability weapons only reroll themselves
            if (weapon == hero.belongings.abilityWeapon) {
                reroll(
                    weapon,
                )
            } else {
                reroll(
                    hero.belongings.weapon(), hero.belongings.secondWep()
                )
            }
        }
    }

    private fun reroll(vararg items: Item?) {
        val hero = target as Hero

        for (item in items) {
            if (item != null) {
                transmuteInventoryItem(item, hero)
            }
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(WEP_REROLL_CHANCE, weaponRandom.chance)
        bundle.put(WAND_REROLL_CHANCE, wandRandom.chance)
        bundle.put(MISSILE_REROLL_CHANCE, misRandom.chance)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        weaponRandom.chance = bundle.getFloat(WEP_REROLL_CHANCE)
        wandRandom.chance = bundle.getFloat(WAND_REROLL_CHANCE)
        if (bundle.contains(MISSILE_REROLL_CHANCE)) {
            misRandom.chance = bundle.getFloat(MISSILE_REROLL_CHANCE)
        }
    }

    companion object {
        private const val WEP_CHANCE_BASE = 0.05f
        private const val WEP_CHANCE_INCREMENT = 0.05f
        private const val RANGED_CHANCE_BASE = 0.1f
        private const val RANGED_CHANCE_INCREMENT = 0.1f

        private const val WEP_REROLL_CHANCE = "weapon_reroll_chance"
        private const val WAND_REROLL_CHANCE = "wand_reroll_chance"
        private const val MISSILE_REROLL_CHANCE = "missile_reroll_chance"

        fun rerollMissile(weapon: MissileWeapon): MissileWeapon {
            return Dungeon.hero.buff(Pandemonium::class.java)?.missileUsed(weapon) ?: weapon
        }

        fun spiritBowUsed(bow: SpiritBow) {
            Dungeon.hero.buff(Pandemonium::class.java)?.spiritBowUsed(bow)
        }
    }
}
