package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level

import com.badlogic.gdx.math.MathUtils
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC
import com.shatteredpixel.shatteredpixeldungeon.levels.Level
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GatewayTrap
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GrimTrap
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.isLevelBossOrSpecial
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.LevelCreationHooks
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.reportRecoverableError
import com.watabou.utils.BArray
import com.watabou.utils.PathFinder
import com.watabou.utils.Random
import kotlin.math.max

@LevelCreationHooks
fun Level.applyOverTheEdge() {
    if (isLevelBossOrSpecial()) return
    buildFlagMaps()
    // Cells that aren't required to be accessible but still can't be set to void
    val noVoidCells = mutableSetOf<Int>()
    val voidCellsSet = mutableSetOf<Int>()

    val entrance = getTransition(LevelTransition.Type.REGULAR_ENTRANCE).cell()

    val technicallyPassable = BArray.not(pit, null)
    BArray.and(avoid, technicallyPassable, technicallyPassable)
    BArray.or(passable, technicallyPassable, technicallyPassable)

    for (i in width() until length() - width()) {
        if (canCellBeVoid(i)) {
            voidCellsSet.add(i)
        }
        val terrain = map[i]
        if (terrain == Terrain.DOOR ||
            terrain == Terrain.LOCKED_DOOR ||
            terrain == Terrain.OPEN_DOOR ||
            terrain == Terrain.SECRET_DOOR ||
            terrain == Terrain.CRYSTAL_DOOR ||
            terrain == Terrain.BARRICADE ||
            terrain == Terrain.BOOKSHELF ||
            terrain == Terrain.ALCHEMY ||
            terrain == Terrain.PEDESTAL ||
            terrain == Terrain.WELL
        ) {
            noVoidCells.add(i)
            technicallyPassable[i] = true
            for (o in PathFinder.NEIGHBOURS4) {
                if (passable[i + o]) {
                    noVoidCells.add(i + o)
                }
            }
        }
    }
    for (p in plants) {
        noVoidCells.add(p.key)
    }
    for (mob in mobs) {
        // NPCs are not passable, but their neighbours are important
        if (mob is NPC) {
            voidCellsSet.remove(mob.pos)
            technicallyPassable[mob.pos] = false
            noVoidCells.remove(mob.pos)
            for (i in PathFinder.NEIGHBOURS4) {
                if (technicallyPassable[mob.pos + i]) {
                    noVoidCells.add(mob.pos + i)
                }
            }
        } else {
            noVoidCells.add(mob.pos)
        }
    }
    for (h in heaps) {
        noVoidCells.add(h.key)
    }

    for (t in traps.entries()) {
        noVoidCells.add(t.key)
        technicallyPassable[t.key] = true
        if (t.value is GatewayTrap || t.value is GrimTrap) {
            for (i in PathFinder.NEIGHBOURS4) {
                if (technicallyPassable[t.key + i]) {
                    noVoidCells.add(t.key + i)
                }
            }
        }
    }

    for (customTile in customTiles) {
        for (x in customTile.tileX until customTile.tileX + customTile.tileW) {
            for (y in customTile.tileY until customTile.tileY + customTile.tileH) {
                val cell = x + y * width()
                noVoidCells.add(cell)
            }
        }
    }

    val randomDestinations = voidCellsSet.toTypedArray()
    Random.shuffle(randomDestinations)
    for (i in 0 until randomDestinations.size / 10) {
        val cell = randomDestinations[i]
        noVoidCells.add(cell)
    }

    // Important cells that must remain accessible. Includes all no-void cells
    // that are accessible normally, plus entrances and the cells around them
    val importantCells = mutableSetOf<Int>()
    importantCells.addAll(noVoidCells)

    PathFinder.buildDistanceMap(entrance, technicallyPassable)
    importantCells.iterator().let {
        while (it.hasNext()) {
            val cell = it.next()
            if (PathFinder.distance[cell] == Integer.MAX_VALUE) {
                it.remove()
            }
        }
    }

    // Transition cells are always important
    for (t in transitions) {
        val cell = t.cell()
        if (PathFinder.distance[cell] == Integer.MAX_VALUE) {
            reportRecoverableError("Transition cell is unreachable, Over The Edge application canceled: $cell, ${t.type}")
            return
        }
        importantCells.add(t.cell())
        for (i in PathFinder.NEIGHBOURS4) {
            if (technicallyPassable[cell + i]) {
                importantCells.add(cell + i)
            }
        }
    }

    voidCellsSet.removeAll(importantCells)
    voidCellsSet.removeAll(noVoidCells)
    val pickedIndices = mutableSetOf<Int>()

    val maxTries = 100
    var tries = 0

    val tempPassable = technicallyPassable.copyOf()

    val toDelete = mutableSetOf<Int>()

    while (voidCellsSet.isNotEmpty() && tries < maxTries) {
        val voidCells = voidCellsSet.toList()
        val batchSize =
            if (tries > maxTries / 2) {
                MathUtils.clamp(voidCells.size / 10, 1, 1)
            } else {
                max(voidCells.size / 10, 1)
            }

        pickedIndices.clear()
        for (i in 0 until batchSize) {
            val idx = Random.Int(voidCells.size)
            if (pickedIndices.add(idx)) {
                tempPassable[voidCells[idx]] = false
            }
        }

        PathFinder.buildDistanceMap(entrance, tempPassable)

        var anyBlocked = false
        for (importantCell in importantCells) {
            if (PathFinder.distance[importantCell] == Integer.MAX_VALUE) {
                anyBlocked = true
                break
            }
        }
        if (anyBlocked) {
            technicallyPassable.copyInto(tempPassable)
            tries++
            continue
        } else {
            tempPassable.copyInto(technicallyPassable)
            for (idx in pickedIndices.sortedBy { it }.reversed()) {
                val cell = voidCells[idx]
                voidCellsSet.remove(cell)
                toDelete.add(cell)
            }
            tries = 0
        }
    }

    for (i in toDelete) {
        if (Random.Float() < 0.8f && !heaps.containsKey(i)) {
            map[i] = Terrain.CHASM
        }
    }

    buildFlagMaps()
}

private fun Level.canCellBeVoid(cell: Int): Boolean {
    if (!insideMap(cell)) return false
    val terrain = map[cell]
    return terrain == Terrain.EMPTY ||
        terrain == Terrain.EMPTY_SP ||
        terrain == Terrain.EMPTY_DECO ||
        terrain == Terrain.EMBERS ||
        terrain == Terrain.GRASS ||
        terrain == Terrain.HIGH_GRASS ||
        terrain == Terrain.FURROWED_GRASS ||
        terrain == Terrain.WATER
}
