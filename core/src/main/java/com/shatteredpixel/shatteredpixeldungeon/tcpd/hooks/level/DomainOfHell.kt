package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew
import com.shatteredpixel.shatteredpixeldungeon.levels.Level
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.blobs.findBlob
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.isNoneOr
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.LevelCreationHooks
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.guaranteedItemsToPlaceNearEntrance
import com.watabou.utils.PathFinder
import com.watabou.utils.Random

@LevelCreationHooks
fun Level.applyDomainOfHell() {
    guaranteedItemsToPlaceNearEntrance.add(AquaBrew())
    getTransition(LevelTransition.Type.REGULAR_ENTRANCE)?.let { transition ->
        val entrance = transition.cell()
        val valid = mutableListOf<Int>()
        for (o in PathFinder.NEIGHBOURS8) {
            val pos = entrance + o
            if (passable[pos]) {
                valid.add(pos)
            }
        }
        val targetCell = Random.element(valid) ?: entrance
        drop(AquaBrew(), targetCell)

        (this as? RegularLevel)?.room(entrance)?.let { room ->
            for (point in room.points) {
                val pos = pointToCell(point)
                if (pos != entrance && flamable[pos] && !solid[pos]) {
                    Level.set(pos, Terrain.WATER, this)
                }
            }
        }
    }

    igniteAllGrass()
}

fun Level.igniteAllGrass() {
    val fire = findBlob<Fire>()
    for (i in 0 until length()) {
        val t = map[i]
        if (t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS) {
            if (fire.isNoneOr { it.cur[i] <= 0 }) {
                Blob.seed(i, 4, Fire::class.java, this)
            }
        }
    }
}
