package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.blobs

import com.shatteredpixel.shatteredpixeldungeon.Assets
import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter
import com.shatteredpixel.shatteredpixeldungeon.items.Heap
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.levels.Level
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Exterminating
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.mobs.StoredHeapData
import com.shatteredpixel.shatteredpixeldungeon.tcpd.effects.CustomBlobCellEmission
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.isLevelBossOrSpecial
import com.shatteredpixel.shatteredpixeldungeon.utils.BitPaint
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog
import com.watabou.noosa.audio.Sample
import com.watabou.noosa.particles.Emitter
import com.watabou.noosa.particles.PixelParticle.Shrinking
import com.watabou.utils.Bundle
import com.watabou.utils.Random

class ExterminationItemLock :
    Blob(),
    CustomBlobCellEmission {
    override fun evolve() {
        evolveUnchanged(off)
        if (isLevelBossOrSpecial()) {
            unlockAll(Dungeon.level)
        }
    }

    private val originalHeaps: MutableMap<Int, StoredHeapData> = mutableMapOf()

    override fun tileDesc(): String = Messages.get(this, "desc")

    override fun use(emitter: BlobEmitter) {
        super.use(emitter)
        emitter.pour(LockParticle.FACTORY, 0.1f)
    }

    fun unlockAll(level: Level): Boolean {
        if (volume <= 0) return true

        for ((pos, heap) in originalHeaps) {
            heap.restoreAtPos(level, pos, spawnPassive = true)
        }
        originalHeaps.clear()
        fullyClear(level)
        if (Modifier.MIMICS_ALL.active()) {
            var added = 0
            for (mob in level.mobs) {
                if (mob is Mimic || mob is Statue) {
                    Buff.affect(mob, Exterminating::class.java)
                    Buff.affect(mob, Exterminating.Reveal::class.java)
                    added++
                }
            }

            if (added > 0) {
                // only notify and update if we are operating on the "live" level
                if (level == Dungeon.level) {
                    GLog.w(Messages.get(Modifier::class.java, "extermination_second_round", added))
                    Sample.INSTANCE.play(Assets.Sounds.DEGRADE, 0.5f, 1.5f)
                    Dungeon.observe()
                    GameScene.updateFog()
                    Dungeon.hero.checkVisibleMobs()
                }
                return false
            }
        }
        return true
    }

    fun transformItems(cb: (Item) -> Item?) {
        for (heap in originalHeaps.values) {
            heap.transformItems(cb)
        }
    }

    fun lockItem(
        level: Level,
        heap: Heap,
    ) {
        if (cur == null || cur[heap.pos] <= 0) {
            seed(level, heap.pos, 1)
        }

        originalHeaps[heap.pos] = StoredHeapData.fromHeap(heap)
        if (level == Dungeon.level) {
            heap.destroy()
        } else {
            level.heaps.remove(heap.pos)
        }
    }

    fun lockMimic(
        level: Level,
        mimic: Mimic,
    ) {
        if (cur == null || cur[mimic.pos] <= 0) {
            seed(level, mimic.pos, 1)
        }

        originalHeaps[mimic.pos] = StoredHeapData.fromMimic(mimic)

        level.mobs.remove(mimic)
        Actor.remove(mimic)
        mimic.sprite?.killAndErase()
    }

    fun lockStatue(
        level: Level,
        statue: Statue,
    ) {
        if (cur == null || cur[statue.pos] <= 0) {
            seed(level, statue.pos, 1)
        }

        originalHeaps[statue.pos] = StoredHeapData.fromStatue(statue)

        level.mobs.remove(statue)
        Actor.remove(statue)
        statue.sprite?.killAndErase()
    }

    fun lockedItems(): Map<Int, StoredHeapData> = originalHeaps

    override fun emit(
        emitter: BlobEmitter,
        factory: Emitter.Factory,
        index: Int,
        cellX: Int,
        cellY: Int,
        cell: Int,
        tileSize: Float,
    ) {
        Random.shuffle(indices)
        for (i in 0 until indices.size / 2) {
            val imageScale = 0.1f
            val idx = indices[i]
            val x = dots[idx * 2] * imageScale
            val y = dots[idx * 2 + 1] * imageScale

            factory.emit(
                emitter,
                index,
                (cellX + 0.5f + x) * tileSize,
                (cellY + 0.5f + y) * tileSize,
            )
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        val (pos, heaps) = originalHeaps.entries.map { it.key to it.value }.unzip()

        bundle.put(ORIGINAL_HEAPS_IDX, pos.toIntArray())
        bundle.put(ORIGINAL_HEAPS_TY, heaps)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)

        val pos = bundle.getIntArray(ORIGINAL_HEAPS_IDX)
        val heaps = bundle.getCollection(ORIGINAL_HEAPS_TY).toTypedArray()

        originalHeaps.clear()
        for (i in pos.indices) {
            originalHeaps[pos[i]] = heaps[i] as StoredHeapData
        }
    }

    companion object {
        private const val ORIGINAL_HEAPS_IDX = "original_heaps_idx"
        private const val ORIGINAL_HEAPS_TY = "original_heaps_types"

        private val dots =
            BitPaint(9)
                .also {
                    it.addHLine(2, 6, 0)
                    it.addHLine(0, 8, 4)
                    it.addHLine(0, 8, 9)
                    it.addVLine(1, 2, 4)
                    it.addVLine(7, 2, 4)
                    it.addVLine(0, 5, 8)
                    it.addVLine(8, 5, 8)
                    it.addVLine(4, 6, 7)
                }.toPairsArray(-4, -7)

        private val indices = Array(dots.size / 2) { it }
    }
}

open class LockParticle : Shrinking() {
    init {
        color(0x15C7EB)
        lifespan = 0.6f
    }

    fun reset(
        x: Float,
        y: Float,
    ) {
        revive()

        this.x = x
        this.y = y

        left = lifespan

        size = 2f
        angularSpeed = Random.Float() * 360 - 180
        speed.set(0f)
    }

    override fun update() {
        super.update()
        val p = left / lifespan
        am = if (p > 0.8f) (1 - p) * 5 else 1f
    }

    companion object {
        val FACTORY: Emitter.Factory =
            object : Emitter.Factory() {
                override fun emit(
                    emitter: Emitter,
                    index: Int,
                    x: Float,
                    y: Float,
                ) {
                    (emitter.recycle(LockParticle::class.java) as LockParticle).reset(x, y)
                }

                override fun lightMode(): Boolean = false
            }
    }
}
