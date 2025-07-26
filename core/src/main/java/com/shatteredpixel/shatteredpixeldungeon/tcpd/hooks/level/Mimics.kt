package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob
import com.shatteredpixel.shatteredpixeldungeon.items.Heap
import com.shatteredpixel.shatteredpixeldungeon.levels.Level
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.ConnectionRoom
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.entrance.EntranceRoom
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.DelayedBeckon
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Exterminating
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.HoldingHeap
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.InvulnerableUntilSeen
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.RecursiveResizing
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Resizing
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.mobs.HolderMimic
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.mobs.StoredHeapData
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.isDoor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.isLevelBossOrSpecial
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.LevelCreationHooks
import com.watabou.utils.DeviceCompat
import com.watabou.utils.PathFinder
import com.watabou.utils.Random
import kotlin.math.pow

fun Level.mimicsEncaseHeap(h: Heap): Boolean = mimicsEncaseHeap(h, Modifier.MIMICS_ALL.active(), Modifier.MIMICS_GRIND.active())

fun Level.mimicsEncaseHeap(
    h: Heap,
    allItems: Boolean,
    grind: Boolean,
): Boolean {
    if (h.type == Heap.Type.CHEST || allItems) {
        StoredHeapData.transformHeapIntoMimic(
            this,
            h,
            extraLoot = grind,
            weakHolders = !grind,
        )
        return true
    }
    return false
}

@LevelCreationHooks
fun Level.applyMimics() {
    val allItems = Modifier.MIMICS_ALL.active()
    val grind = Modifier.MIMICS_GRIND.active()
    val iter = heaps.iterator()
    while (iter.hasNext()) {
        val h = iter.next()
        if (mimicsEncaseHeap(h.value, allItems, grind)) {
            iter.remove()
        }
    }
}

@LevelCreationHooks
fun Level.applyJackInTheBox() {
    for (mob in mobs.toTypedArray()) {
        if (mob.properties().contains(Char.Property.BOSS) ||
            mob
                .properties()
                .contains(Char.Property.MINIBOSS) ||
            mob
                .properties()
                .contains(Char.Property.IMMOVABLE)
        ) {
            continue
        }

        val heapData = StoredHeapData.fromMob(mob)
        val newMob = HolderMimic()
        newMob.setLevel(Dungeon.scalingDepth())
        newMob.pos = mob.pos
        Buff.affect(newMob, HoldingHeap::class.java).set(heapData)

        transferHoldingBuffs(mob, newMob)

        mobs.remove(mob)
        mobs.add(newMob)
    }
}

@LevelCreationHooks
fun Level.applyBoxed() {
    if (isLevelBossOrSpecial()) return
    if (this !is RegularLevel) return

    for (room in rooms()) {
        if (room is EntranceRoom || room is ConnectionRoom) continue
        for (x in (room.left)..(room.right)) {
            cellLoop@ for (y in (room.top)..(room.bottom)) {
                if (x > room.left + 1 && x < room.right - 1 && y > room.top + 1 && y < room.bottom - 1) {
                    continue
                }

                val i = x + y * width()
                if (solid[i] || pit[i] || !(passable[i] || avoid[i])) continue
                var nearSolid = false
                for (j in PathFinder.CIRCLE8.indices) {
                    val o = PathFinder.CIRCLE8[j]
                    val cell = i + o

                    if (solid[cell]) nearSolid = true

                    if (j % 2 == 1 && isDoor(cell)) {
                        continue@cellLoop
                    }
                }

                if (!nearSolid) continue
                if (findMob(i) != null) continue

                val mob = createMob()
                val heap = StoredHeapData.fromMob(mob)

                val mimic = HolderMimic()
                Buff.affect(mimic, HoldingHeap::class.java).set(heap)
                Buff.affect(mimic, InvulnerableUntilSeen::class.java)
                mimic.setLevel(Dungeon.scalingDepth())
                mimic.pos = i
                mobs.add(mimic)
            }
        }
    }
}

@LevelCreationHooks
fun Level.applyRecursiveHierarchy() {
    for (mob in mobs.toTypedArray()) {
        if (mob is Mimic) {
            var newMob: Mob = mob
            val maxRecursion = Dungeon.scalingDepth() / 5 + 2
            var steps = 0
            while ((Random.Float() < 0.5 || DeviceCompat.isDebug()) && steps++ < maxRecursion) {
                val heap = StoredHeapData.fromMob(newMob)
                newMob = HolderMimic()
                newMob.setLevel(Dungeon.scalingDepth())
                newMob.pos = mob.pos
                Buff.affect(newMob, HoldingHeap::class.java).set(heap)
            }
            if (newMob != mob) {
                mobs.remove(mob)
                mobs.add(newMob)
                transferHoldingBuffs(mob, newMob)
                Buff.affect(newMob, Resizing::class.java).let {
                    it.multiply(1 / 1.1f.pow(steps - 1))
                    if (Modifier.CROWD_DIVERSITY.active()) it.multiplyRandom()
                }
                Buff.affect(newMob, RecursiveResizing::class.java).set(1.1f)
            }
        }
    }
}

private fun transferHoldingBuffs(
    from: Mob,
    to: Mob,
) {
    if (from.buff(Exterminating::class.java) != null) {
        Buff.affect(to, Exterminating::class.java)
    }
    from.buff(DelayedBeckon::class.java)?.let {
        Buff.affect(to, DelayedBeckon::class.java).start(it.getTicker())
    }
}
