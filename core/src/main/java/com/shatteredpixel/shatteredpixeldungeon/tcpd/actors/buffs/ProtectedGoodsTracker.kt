package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Awareness
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.furrowCell
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.mimicsEncaseHeap
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.weightedPair
import com.watabou.utils.BArray
import com.watabou.utils.Bundle
import com.watabou.utils.PathFinder
import kotlin.math.pow

class ProtectedGoodsTracker :
    Buff(),
    OnBossSlainBuff {
    init {
        // act just before hero
        actPriority = HERO_PRIO + 1
    }

    private val items: MutableList<Item> = mutableListOf()

    fun addItem(item: Item) {
        items.add(item.duplicate())
    }

    override fun attachTo(target: Char?): Boolean = super.attachTo(target).also { if (it) diactivate() }

    override fun act(): Boolean {
        if (items.isEmpty()) {
            diactivate()
            return true
        }

        PathFinder.buildDistanceMap(
            target.pos,
            BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null),
        )
        val validCells = mutableListOf<Pair<Float, Int>>()

        for (i in 0 until Dungeon.level.length()) {
            val distance = PathFinder.distance[i]
            if (distance <= 0) continue
            val terrain = Dungeon.level.map[i]
            if (
                distance < Int.MAX_VALUE &&
                !Dungeon.level.pit[i] &&
                terrain != Terrain.TRAP &&
                terrain != Terrain.SECRET_TRAP &&
                Dungeon.level.getTransition(i) == null
            ) {
                validCells.add(weightedPair(1f / distance.toFloat().pow(1), i))
            }
        }

        validCells.sortWith(
            compareBy<Pair<Float, Int>> { it.first }
                .thenBy { it.second },
        )
        if (validCells.isEmpty()) {
            validCells.add(Pair(0f, target.pos))
        }

        for (i in items.indices) {
            val item = items[i]
            val cell = validCells[i % validCells.size].second
            Dungeon.level.furrowCell(cell)
            Dungeon.level.pressCell(cell)
            val heap = Dungeon.level.drop(item, cell)
            // in case of mimics modifiers
            if (Dungeon.level.mimicsEncaseHeap(heap)) {
                Dungeon.level.heaps.remove(cell)
            } else {
                if (Dungeon.level.heroFOV[cell]) {
                    heap.sprite.drop()
                }
            }
        }
        items.clear()
        affect(target, Awareness::class.java, Awareness.DURATION)
        Dungeon.observe()
        GameScene.updateMap()

        diactivate()
        return true
    }

    override fun onBossSlain() {
        spend(-cooldown())
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(ITEMS, items)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        val items = bundle.getCollection(ITEMS)
        this.items.clear()
        for (item in items) {
            this.items.add(item as Item)
        }
    }

    companion object {
        private const val ITEMS = "items"
    }
}
