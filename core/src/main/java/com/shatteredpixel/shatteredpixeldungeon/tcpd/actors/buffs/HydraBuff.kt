package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.watabou.utils.Bundle

class HydraBuff : Buff() {
    private var wantSpawn = 0

    override fun act(): Boolean {
        doSpawns()
        if (wantSpawn == 0) {
            diactivate()
        } else {
            spend(TICK)
        }
        return true
    }

    fun addSpawns(count: Int) {
        wantSpawn += count
        doSpawns()
        if (wantSpawn > 0) {
            spend(-cooldown())
        }
    }

    private fun doSpawns() {
        while (wantSpawn > 0 && Dungeon.level.spawnMob(6)) wantSpawn--
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(WANT_SPAWN, wantSpawn)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        wantSpawn = bundle.getInt(WANT_SPAWN)
    }

    companion object {
        private const val WANT_SPAWN = "want_spawn"
    }

    class EnemyTracker :
        Buff(),
        OnDeathEffectBuff {
        override fun onDeathProc() {
            if (!Dungeon.hero.isAlive) return
            affect(Dungeon.hero, HydraBuff::class.java).addSpawns(2)
        }
    }
}
