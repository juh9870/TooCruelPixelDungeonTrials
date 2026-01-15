package com.shatteredpixel.shatteredpixeldungeon.tcpd.ext

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon
import com.shatteredpixel.shatteredpixeldungeon.levels.Level

@Suppress("NAME_SHADOWING")
fun Item.collectOrDrop(
    hero: Hero? = null,
    level: Level? = null,
) {
    val hero = hero ?: Dungeon.hero
    val level = level ?: Dungeon.level
    if (isEquipped(hero)) {
        return
    }
    if (!collect()) {
        level.drop(this, hero.pos).sprite.drop()
    }
}

fun Item.curseIfAllowed(known: Boolean) {
    if (this is Weapon) {
        this.cursed = true
        this.cursedKnown = this.cursedKnown || known
        val w = this
        if (w.enchantment == null) {
            w.enchant(Weapon.Enchantment.randomCurse())
        }
    }
    if (this is Armor) {
        this.cursed = true
        this.cursedKnown = this.cursedKnown || known
        val a = this
        if (a.glyph == null) {
            a.inscribe(Armor.Glyph.randomCurse())
        }
    }
    if (this is Wand || this is Ring || this is Artifact) {
        this.cursed = true
        this.cursedKnown = this.cursedKnown || known
    }
}
