package com.shatteredpixel.shatteredpixeldungeon.tcpd.ext

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping
import com.shatteredpixel.shatteredpixeldungeon.levels.Level
import com.shatteredpixel.shatteredpixeldungeon.levels.Level.Feeling
import com.shatteredpixel.shatteredpixeldungeon.levels.Level.set
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.blobs.ExterminationItemLock
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.blobs.findBlob
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.mobs.transformItems
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.transformItems
import com.watabou.utils.PathFinder
import com.watabou.utils.Random
import com.watabou.utils.Reflection

fun Level.placeDuplicatorTraps(trap: Class<out Trap>) {
    if (trap.isAnonymousClass) return
    val cells = randomDuplicatorTrapCells()
    Random.shuffle(cells)
    val nTraps = 2

    for (i in 0 until minOf(nTraps, cells.size)) {
        val pos = cells[i]
        val t = setTrap(Reflection.newInstance(trap), pos)
        val old = map[pos]
        set(pos, Terrain.TRAP, this)
        t.reveal()
        GameScene.updateMap(pos)
        if (Dungeon.level.heroFOV[pos]) {
            GameScene.discoverTile(pos, old)
            ScrollOfMagicMapping.discover(pos)
        }
    }
}

fun Level.randomDuplicatorTrapCells(): List<Int> {
    val cells: MutableList<Int> = mutableListOf()
    for (i in 0 until length()) {
        if (isValidDuplicatorTrapPos(i)) {
            cells.add(i)
        }
    }
    return cells
}

private fun Level.isValidDuplicatorTrapPos(pos: Int): Boolean {
    if (pos < 0 || pos >= length()) return false
    return when (map[pos]) {
        Terrain.EMPTY, Terrain.GRASS, Terrain.HIGH_GRASS, Terrain.EMBERS,
        Terrain.EMPTY_DECO, Terrain.EMPTY_SP, Terrain.INACTIVE_TRAP, Terrain.WATER,
        -> true

        else -> false
    }
}

fun Level.furrowCell(cell: Int) {
    if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
        map[cell] = Terrain.GRASS
        losBlocking[cell] = false
    }
}

fun Level.isDoor(cell: Int): Boolean {
    val terrain = map[cell]
    return terrain == Terrain.DOOR ||
        terrain == Terrain.OPEN_DOOR ||
        terrain == Terrain.LOCKED_DOOR ||
        terrain == Terrain.SECRET_DOOR ||
        terrain == Terrain.CRYSTAL_DOOR
}

inline fun Level.transformItems(crossinline cb: (Item) -> Item?) {
    for (h in heaps) {
        h.value.transformItems(cb)
    }

    val replacementMobs = mutableMapOf<Mob, Mob>()
    for (mob in mobs) {
        mob.transformItems(cb)?.let {
            replacementMobs[mob] = it
        }
    }

    for (mob in replacementMobs) {
        mobs.remove(mob.key)
        mobs.add(mob.value)
    }

    findBlob<ExterminationItemLock>()?.transformItems { cb(it) }
}

fun Level.destroyWall(cell: Int) {
    val terrain = map[cell]
    if (terrain == Terrain.WALL ||
        terrain == Terrain.WALL_DECO ||
        terrain == Terrain.STATUE ||
        terrain == Terrain.STATUE_SP ||
        terrain == Terrain.SECRET_DOOR ||
        terrain == Terrain.CRYSTAL_DOOR ||
        terrain == Terrain.BOOKSHELF
    ) {
        strongDestroy(cell)
    }
}

fun Level.strongDestroy(
    cell: Int,
    replaceWith: Int = Terrain.EMBERS,
) {
    if (!insideMap(cell)) return
    set(cell, replaceWith)
    for (o in PathFinder.NEIGHBOURS4) {
        val n = cell + o
        val terrain = map[n]
        if (terrain == Terrain.DOOR || terrain == Terrain.OPEN_DOOR || terrain == Terrain.CRYSTAL_DOOR || terrain == Terrain.LOCKED_DOOR) {
            strongDestroy(n)
        }
    }
    destroy(cell)
}

fun Level.defaultNItems(): Int {
    // drops 3/4/5 items 60%/30%/10% of the time
    var nItems = 3 + Random.chances(floatArrayOf(6f, 3f, 1f))

    if (feeling == Feeling.LARGE) {
        nItems += 2
    }
    return nItems
}
