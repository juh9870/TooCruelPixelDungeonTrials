package com.shatteredpixel.shatteredpixeldungeon.tcpd.ext

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.levels.Level

@Suppress("NAME_SHADOWING")
fun Item.collectOrDrop(hero: Hero? = null, level: Level? = null) {
    val hero = hero ?: Dungeon.hero
    val level = level ?: Dungeon.level
    if (isEquipped(hero)) {
        return
    }
    if (!collect()) {
        level.drop(this, hero.pos).sprite.drop()
    }
}