package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.WellWater
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.items.Generator
import com.shatteredpixel.shatteredpixeldungeon.items.Heap
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade
import com.shatteredpixel.shatteredpixeldungeon.levels.Level
import com.shatteredpixel.shatteredpixeldungeon.levels.Level.Feeling
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.blobs.PATRON_SEED_BLESS
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.blobs.PatronSaintsBlob
import com.watabou.utils.PathFinder
import com.watabou.utils.Random
import com.watabou.utils.Reflection

fun RegularLevel.createItemsHook() {
    if (Modifier.HEAD_START.active() && Dungeon.depth == 1) {
        repeat(2) {
            drop(ScrollOfUpgrade(), placeItemPos())
            Dungeon.LimitedDrops.UPGRADE_SCROLLS.count++
        }
        drop(PotionOfStrength(), placeItemPos())
        Dungeon.LimitedDrops.UPGRADE_SCROLLS.count++
    }
}

fun Level.postCreateHook() {
    // Reveal all traps if THUNDERSTRUCK modifier is active, for fairness
    if (Modifier.THUNDERSTRUCK.active()) {
        for (trap in traps.valueList()) {
            trap.reveal()
        }
    }
    if (Modifier.SECOND_TRY.active()) {
        applySecondTry()
    }
    if (Modifier.HOLY_WATER.active()) {
        applyHolyWater()
    }
    if (Modifier.LOFT.active()) {
        applyLoft()
    }
    if (Modifier.LOOT_PARADISE.active()) {
        applyLootParadise()
    }
}

@Suppress("NAME_SHADOWING")
fun Level.updateFieldOfViewHook(
    c: Char, modifiableBlocking: BooleanArray, blocking: BooleanArray?
): BooleanArray? {
    var blocking = blocking
    if (c is Hero || c.alignment == Char.Alignment.ALLY) {
        if (Modifier.LOFT.active()) {
            blocking = blocking ?: initBlocking(modifiableBlocking)
            for (i in blocking.indices) {
                if (!blocking[i] && pit[i]) {
                    blocking[i] = true
                }
            }
        }
        if (Modifier.BULKY_FRAME.active()) {
            blocking = blocking ?: initBlocking(modifiableBlocking)
            for (mob in mobs) {
                blocking[mob.pos] = true
            }
        }
    }
    return blocking
}

private fun Level.initBlocking(modifiableBlocking: BooleanArray): BooleanArray {
    System.arraycopy(
        Dungeon.level.losBlocking, 0, modifiableBlocking, 0, modifiableBlocking.size
    )

    return modifiableBlocking
}


fun Level.placeDuplicatorTraps(trap: Class<out Trap>) {
    if (trap.isAnonymousClass) return
    val cells = randomDuplicatorTrapCells()
    Random.shuffle(cells)
    val nTraps = 2;

    for (i in 0 until minOf(nTraps, cells.size)) {
        val pos = cells[i]
        val t = setTrap(Reflection.newInstance(trap), pos)
        val old = map[pos]
        Level.set(pos, Terrain.TRAP, this)
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
    if (this is RegularLevel) {
        val rooms = rooms()
        if (rooms.isNotEmpty()) {
            for (room in rooms()) {
                for (point in room.trapPlaceablePoints()) {
                    val pos = pointToCell(point)
                    if (isValidDuplicatorTrapPos(pos)) {
                        cells.add(pos)
                    }
                }
            }
            return cells
        }
    }

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
        Terrain.EMPTY, Terrain.GRASS, Terrain.HIGH_GRASS, Terrain.EMBERS, Terrain.EMPTY_DECO, Terrain.EMPTY_SP, Terrain.INACTIVE_TRAP, Terrain.WATER -> true
        else -> false
    }
}

private fun RegularLevel.placeItemPos(roomType: Class<out Room?>? = null): Int {
    val cell: Int =
        if (roomType != null) randomDropCellExposedHook(roomType) else randomDropCellExposedHook()

    furrowDroppedItemPos(cell)
    return cell
}

private fun Level.furrowDroppedItemPos(cell: Int) {
    if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
        map[cell] = Terrain.GRASS
        losBlocking[cell] = false
    }
}

private fun Level.applySecondTry() {
    var barricades = 0
    for (i in 0 until length()) {
        if (map[i] == Terrain.LOCKED_DOOR) {
            Level.set(i, Terrain.DOOR, this)
        }
        if (map[i] == Terrain.BARRICADE) {
            Level.set(i, Terrain.EMBERS, this)
            barricades++
        }
    }

    var h: Heap
    for (c in heaps.keyArray()) {
        h = heaps.get(c)
        if (h.type == Heap.Type.FOR_SALE) continue
        for (item in ArrayList<Item>(h.items)) {
            if (item.unique) continue
            if (!guaranteedItems.contains(item) || item is Key) h.items.remove(item)
            if (item is PotionOfLiquidFlame && barricades-- > 0) h.items.remove(item)
        }
        if (h.items.isEmpty()) {
            heaps.remove(c)
        }
    }

    for (blob in blobs.values) {
        if (blob is WellWater) {
            blob.fullyClear(this)
        }
    }
}

private fun Level.applyHolyWater() {
    for (i in 0 until length()) {
        if (map[i] == Terrain.WATER) {
            Blob.seed(i, PATRON_SEED_BLESS, PatronSaintsBlob::class.java, this)
        }
    }
}

private fun Level.applyLoft() {
    for (i in 0 until length()) {
        if ((map[i] == Terrain.WALL || map[i] == Terrain.WALL_DECO) && insideMap(i)) {
            map[i] = Terrain.CHASM
        } else if (!insideMap(i)) {
            map[i] = Terrain.CHASM
        }
    }
    buildFlagMaps()
    cleanWalls()
}

private fun Level.applyLootParadise() {
    val validCells = mutableListOf<Int>()
    for (i in 0 until length()) {
        if(!solid[i] && !pit[i]) {
            validCells.add(i)
        }
    }
    for (t in transitions) {
        validCells.remove(t.cell())
    }
    Random.shuffle(validCells)
    val amount = defaultNItems() * 10
    for (i in 0 until amount) {
        if (validCells.size <= i) break
        val cell = validCells[i]

        val toDrop =
            Generator.random() ?: continue
        furrowDroppedItemPos(cell)
        drop(toDrop, cell)
    }

    var nItems = (this as RegularLevel)
}

fun Level.destroyWall(cell: Int) {
    val terrain = map[cell]
    if (terrain == Terrain.WALL ||
        terrain == Terrain.WALL_DECO ||
        terrain == Terrain.STATUE ||
        terrain == Terrain.STATUE_SP ||
        terrain == Terrain.SECRET_DOOR ||
        terrain == Terrain.CRYSTAL_DOOR
    ) {
        strongDestroy(cell)
    }
}

fun Level.strongDestroy(cell: Int, replaceWith: Int = Terrain.EMBERS) {
    if (!insideMap(cell)) return
    Level.set(cell, replaceWith)
    for (o in PathFinder.NEIGHBOURS8) {
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