package com.shatteredpixel.shatteredpixeldungeon.tcpd.ext

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.Statistics
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck
import com.shatteredpixel.shatteredpixeldungeon.effects.Transmuting
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog

fun transmuteInventoryItem(item: Item, hero: Hero): Item? {
    var result = ScrollOfTransmutation.changeItem(item)

    if (result != null) {
        if (result !== item) {
            val slot = Dungeon.quickslot.getSlot(item)
            if (item.isEquipped(Dungeon.hero)) {
                item.cursed = false //to allow it to be unequipped
                if (item is Artifact && result is Ring) {
                    //if we turned an equipped artifact into a ring, ring goes into inventory
                    (item as EquipableItem).doUnequip(Dungeon.hero, false)
                    if (!result.collect()) {
                        Dungeon.level.drop(result, hero.pos).sprite.drop()
                    }
                } else if (item is KindOfWeapon && Dungeon.hero.belongings.secondWep() === item) {
                    (item as EquipableItem).doUnequip(Dungeon.hero, false)
                    (result as KindOfWeapon).equipSecondary(Dungeon.hero)
                } else {
                    (item as EquipableItem).doUnequip(Dungeon.hero, false)
                    (result as EquipableItem).doEquip(Dungeon.hero)
                }
                Dungeon.hero.spend(-Dungeon.hero.cooldown()) //cancel equip/unequip time
            } else {
                item.detach(Dungeon.hero.belongings.backpack)
                if (!result.collect()) {
                    Dungeon.level.drop(result, hero.pos).sprite.drop()
                } else if (result.stackable && Dungeon.hero.belongings.getSimilar(result) != null) {
                    result = Dungeon.hero.belongings.getSimilar(result)
                }
            }
            if (slot != -1 && result!!.defaultAction() != null && !Dungeon.quickslot.isNonePlaceholder(
                    slot
                )
                && Dungeon.hero.belongings.contains(result)
            ) {
                Dungeon.quickslot.setSlot(slot, result)
            }
        }
        if (result!!.isIdentified) {
            Catalog.setSeen(result.javaClass)
            Statistics.itemTypesDiscovered.add(result.javaClass)
        }
        showItemTransmuted(hero, item, result)
        return result
    }

    return null
}

fun showItemTransmuted(hero: Hero, item: Item, result: Item) {
    Transmuting.show(hero, item, result)
    hero.sprite.emitter().start(Speck.factory(Speck.CHANGE), 0.2f, 10)
    GLog.p(Messages.get(ScrollOfTransmutation::class.java, "morph"))
}