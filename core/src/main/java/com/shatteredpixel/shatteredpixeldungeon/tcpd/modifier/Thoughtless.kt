package com.shatteredpixel.shatteredpixeldungeon.tcpd.modifier

import com.shatteredpixel.shatteredpixeldungeon.Assets
import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.items.Stylus
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMastery
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfForesight
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMysticalEnergy
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Alchemize
import com.shatteredpixel.shatteredpixeldungeon.items.spells.CurseInfusion
import com.shatteredpixel.shatteredpixeldungeon.items.spells.MagicalInfusion
import com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Recycle
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAugmentation
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfClairvoyance
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.transformItems
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions
import com.watabou.noosa.Game
import com.watabou.noosa.audio.Sample
import com.watabou.utils.Reflection

class Thoughtless {
    companion object {
        private val AFFECTED_ITEMS =
            listOf(
                ScrollOfUpgrade::class.java,
                ScrollOfEnchantment::class.java,
                ScrollOfMagicMapping::class.java,
                ScrollOfRecharging::class.java,
                ScrollOfTransmutation::class.java,
                ScrollOfMysticalEnergy::class.java,
                ScrollOfForesight::class.java,
                Stylus::class.java,
                StoneOfAugmentation::class.java,
                StoneOfEnchantment::class.java,
                StoneOfIntuition::class.java,
                StoneOfClairvoyance::class.java,
                PotionOfStrength::class.java,
                PotionOfMastery::class.java,
                ElixirOfMight::class.java,
                MagicalInfusion::class.java,
                CurseInfusion::class.java,
                Alchemize::class.java,
                ReclaimTrap::class.java,
                Recycle::class.java,
            )

        val AFFECTED_ITEMS_NAMES by lazy {
            AFFECTED_ITEMS.map {
                val inst = Reflection.newInstance(it)
                Messages.titleCase(inst.trueName())
            }
        }

        private var thoughtlessConfirmed = false

        fun isItemAffected(item: Item): Boolean = AFFECTED_ITEMS.any { it.isInstance(item) }

        private fun affectedItems(): Pair<List<Item>, List<Item>> {
            val belongings =
                Dungeon.hero.belongings.filter { isItemAffected(it) }

            val affectedHeaps = mutableListOf<Item>()
            for (heap in Dungeon.level.heaps.values()) {
                if (heap.seen && isItemAffected(heap.peek())) {
                    affectedHeaps.add(heap.peek())
                }
            }
            return Pair(belongings, affectedHeaps)
        }

        fun confirmAffectedItems(
            hero: Hero,
            transition: LevelTransition,
        ): Boolean {
            if (thoughtlessConfirmed) {
                thoughtlessConfirmed = false
                return true
            }

            val (affectedInventory, affectedHeaps) = affectedItems()
            if (affectedInventory.isEmpty() && affectedHeaps.isEmpty()) {
                return true
            }

            val heroPos = hero.pos

            Game.runOnRenderThread {
                GameScene.show(
                    object : WndOptions(
                        Messages.get(
                            Thoughtless::class.java,
                            "ask_title",
                        ),
                        Messages.get(
                            Thoughtless::class.java,
                            if (affectedInventory.isNotEmpty() && affectedHeaps.isNotEmpty()) {
                                "ask_description_both"
                            } else if (affectedInventory.isNotEmpty()) {
                                "ask_description_inventory"
                            } else {
                                "ask_description_heaps"
                            },
                            affectedInventory.joinToString(", ") { "_${Messages.titleCase(it.title())}_" },
                            affectedHeaps.joinToString(", ") { "_${Messages.titleCase(it.title())}_" },
                        ),
                        Messages.get(
                            Thoughtless::class.java,
                            "yes",
                        ),
                        Messages.get(
                            Thoughtless::class.java,
                            "no",
                        ),
                    ) {
                        private var elapsed = 0f

                        @Synchronized
                        override fun update() {
                            super.update()
                            elapsed += Game.elapsed
                        }

                        override fun hide() {
                            if (elapsed > 0.2f) {
                                super.hide()
                            }
                        }

                        override fun onSelect(index: Int) {
                            if (index == 0 && elapsed > 0.2f) {
                                if (Dungeon.hero.pos == heroPos) {
                                    thoughtlessConfirmed = true
                                    hero.lastAction = HeroAction.LvlTransition(heroPos)
                                    hero.resume()
                                }
                            }
                        }
                    },
                )
            }
            return false
        }

        fun clearAffectedItems() {
            var inventoryAffected = false
            for (item in Dungeon.hero.belongings.toList()) {
                if (isItemAffected(item)) {
                    inventoryAffected = true
                    item.detachAll(Dungeon.hero.belongings.backpack)
                }
            }

            var heapsAffected = false
            Dungeon.level.transformItems {
                if (isItemAffected(it)) {
                    heapsAffected = true
                    null
                } else {
                    it
                }
            }

            if (heapsAffected || inventoryAffected) {
                val buff = Buff.affect(Dungeon.hero, BurstVisual::class.java)
                buff.heaps = heapsAffected
                buff.inventory = inventoryAffected
            }
        }
    }

    class BurstVisual : Buff() {
        init {
            actPriority = VFX_PRIO
        }

        var heaps: Boolean = false
        var inventory: Boolean = false

        override fun attachTo(target: Char?): Boolean =
            super.attachTo(target).also {
                if (it) {
                    timeToNow()
                    spend(-1f)
                }
            }

        override fun act(): Boolean {
            if (heaps && !inventory) {
                GLog.n(Messages.get(Thoughtless::class.java, "destroyed_heaps"))
            } else if (inventory) {
                GLog.n(Messages.get(Thoughtless::class.java, "destroyed_inventory"))
                if (target.isAlive) {
                    target?.sprite?.emitter()?.burst(ElmoParticle.FACTORY, 6)
                    Sample.INSTANCE.play(Assets.Sounds.BURNING)
                }
            }
            detach()
            return true
        }
    }
}
