package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import kotlin.math.min

class FullSceneUpdater : Buff() {
    init {
        // act before everything, so even the VFX update properly
        actPriority = VFX_PRIO + 1
    }

    companion object {
        fun request() {
            if (Dungeon.hero == null) {
                return
            }
            val buff = affect(Dungeon.hero, FullSceneUpdater::class.java);
            buff.timeToNow()
            // Spend a tiny amount of time to ensure the buff is processed after the current turn
            // If The hery cooldown is negative for some reason, snap the time to right before
            // it, to ensure the buff is processed before the hero acts
            buff.spendConstant(min(0.1f, Dungeon.hero.cooldown() - 0.1f))
        }
    }

    override fun act(): Boolean {
        Dungeon.level.buildFlagMaps();
        Dungeon.level.cleanWalls();
        GameScene.updateMap();
        GameScene.updateFog();
        Dungeon.observe();

//        if(DeviceCompat.isDebug()) {
//            GLog.i("FullSceneUpdater: updated scene at " + now())
//        }

        diactivate()
        return true
    }
}