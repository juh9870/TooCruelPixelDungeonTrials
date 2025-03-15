package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.badlogic.gdx.math.MathUtils
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.items.Generator
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.watabou.utils.Bundlable
import com.watabou.utils.Bundle
import com.watabou.utils.Random

class RetieredBuff : Buff() {
    init {
        // act right after hero to process new weapons
        actPriority = HERO_PRIO - 1
    }

    private var tiers: TiersMap = TiersMap()

    override fun attachTo(target: Char?): Boolean {
        if (target !is Hero) {
            return false
        }
        return super.attachTo(target).also { attached ->
            if (attached) {
                processInventory(target)
            }
        }
    }

    override fun act(): Boolean {
        if (target !is Hero) {
            detach()
            return true
        }

        val hero = target as Hero

        processInventory(hero)

        spend(TICK)
        return true
    }

    fun processInventory(hero: Hero) {
        val untiered = Modifier.UNTIERED.active()

        for (item in hero.belongings) {
            var tiersDeducted = 0
            if (item is MeleeWeapon) {
                val originalTier = item.tier
                item.tier = tiers.tierFor(item.javaClass, item.tier)
                tiersDeducted = originalTier - item.tier
            } else if (item is MissileWeapon) {
                val originalTier = item.tier
                item.tier = tiers.tierFor(item.javaClass, item.tier)
                tiersDeducted = originalTier - item.tier
            }
            if (untiered && tiersDeducted > 0) {
                item.level(item.level() + tiersDeducted)
            }
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(TIERS, tiers)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        tiers = bundle.get(TIERS) as TiersMap
    }

    companion object {
        private const val TIERS = "tiers"
    }

    class TiersMap : Bundlable {
        private val tiers: MutableMap<Class<out Any>, Int> = mutableMapOf()

        init {
            pickTierSix(
                arrayOf(
                    Generator.Category.MIS_T2,
                    Generator.Category.MIS_T3,
                    Generator.Category.MIS_T4,
                    Generator.Category.MIS_T5
                )
            )
            pickTierSix(
                arrayOf(
                    Generator.Category.WEP_T2,
                    Generator.Category.WEP_T3,
                    Generator.Category.WEP_T4,
                    Generator.Category.WEP_T5
                )
            )
        }

        fun tierFor(weapon: Class<out Any>, curTier: Int): Int {
            return tiers.getOrPut(weapon) {
                if (Modifier.UNTIERED.active()) {
                    1
                } else {
                    // T1: 50% chance for -1, 25% for no change, 25% for +1, 0% for completely random
                    // Other tiers: 50% chance for -1, 20% for no change, 25% for +1, 5% for completely random
                    val roll = Random.chances(
                        floatArrayOf(
                            0.5f,
                            if (curTier != 1) 0.20f else 0.25f,
                            0.25f,
                            if (curTier != 1) 0.05f else 0f
                        )
                    );
                    if (roll == 3) {
                        Random.Int(1, 6)
                    } else {
                        MathUtils.clamp(curTier + (roll - 1), 1, 5)
                    }
                }
            }
        }

        private fun pickTierSix(categories: Array<Generator.Category>) {
            val items: MutableList<Class<out Any>> = mutableListOf()
            for (cat in categories) {
                for (cl in cat.classes) {
                    if (cl != null) {
                        items.add(cl)
                    }
                }
            }
            val item = items[Random.index(items)]
            tiers[item] = 6
        }

        override fun restoreFromBundle(bundle: Bundle) {
            val tiers = bundle.getIntArray(TIERS)
            val classes = bundle.getClassArray(CLASSES)
            this.tiers.clear()
            for (i in tiers.indices) {
                this.tiers[classes[i]!!] = tiers[i]
            }
        }

        override fun storeInBundle(bundle: Bundle) {
            val tiers = IntArray(tiers.size)
            val classes = arrayOfNulls<Class<out Any>>(tiers.size)
            var i = 0
            for (tier in this.tiers) {
                classes[i] = tier.key
                tiers[i] = tier.value
                i++
            }
            bundle.put(TIERS, tiers)
            bundle.put(CLASSES, classes)
        }

        companion object {
            private const val TIERS = "tiers"
            private const val CLASSES = "classes"
        }
    }
}