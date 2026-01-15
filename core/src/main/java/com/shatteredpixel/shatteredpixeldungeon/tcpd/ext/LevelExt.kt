package com.shatteredpixel.shatteredpixeldungeon.tcpd.ext

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel
import com.shatteredpixel.shatteredpixeldungeon.levels.CityLevel
import com.shatteredpixel.shatteredpixeldungeon.levels.HallsLevel
import com.shatteredpixel.shatteredpixeldungeon.levels.Level
import com.shatteredpixel.shatteredpixeldungeon.levels.Level.Feeling
import com.shatteredpixel.shatteredpixeldungeon.levels.Level.set
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel
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
        placeRevealedTrapAndChangeTerrain(pos, Reflection.newInstance(trap))
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
    val heaps = heaps.iterator()
    while (heaps.hasNext()) {
        val h = heaps.next()
        h.value.transformItems(cb)
        if (h.value.isEmpty) {
            heaps.remove()
        }
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
        terrain == Terrain.REGION_DECO ||
        terrain == Terrain.REGION_DECO_ALT ||
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

fun Level.placeRevealedTrapAndChangeTerrain(
    pos: Int,
    trap: Trap,
) {
    val t = setTrap(trap, pos)
    val old = map[pos]
    set(pos, Terrain.TRAP, this)
    t.reveal()
    GameScene.updateMap(pos)
    if (Dungeon.level.heroFOV[pos]) {
        GameScene.discoverTile(pos, old)
        ScrollOfMagicMapping.discover(pos)
    }
}

fun Level.randomTrap(): Class<out Trap> {
    if (this is RegularLevel) {
        _hackSmuggleTrapClasses()[Random.chances(_hackSmuggleTrapChances())]
    }
    val (traps, chances) =
        when (Dungeon.depth) {
            in 1..5 -> sewerTrapsChances
            in 6..10 -> prisonTrapsChances
            in 11..15 -> cavesTrapChances
            in 16..20 -> cityTrapChances
            in 21..25, 26 -> hallsTrapChances
            else -> sewerTrapsChances // fallback to sewer traps
        }

    @Suppress("UNCHECKED_CAST")
    return traps[Random.chances(chances)] as Class<out Trap>
}

private fun getTrapData(l: RegularLevel) = l._hackSmuggleTrapClasses() to l._hackSmuggleTrapChances()

private val sewerTrapsChances by lazy {
    val d = Dungeon.depth
    Dungeon.depth = 2
    val data = getTrapData(SewerLevel())
    Dungeon.depth = d
    data
}
private val prisonTrapsChances by lazy { getTrapData(PrisonLevel()) }
private val cavesTrapChances by lazy { getTrapData(CavesLevel()) }
private val cityTrapChances by lazy { getTrapData(CityLevel()) }
private val hallsTrapChances by lazy { getTrapData(HallsLevel()) }

fun isLevelBossOrSpecial(): Boolean = Dungeon.bossLevel() || Dungeon.depth == 26 || Dungeon.branch == 1
