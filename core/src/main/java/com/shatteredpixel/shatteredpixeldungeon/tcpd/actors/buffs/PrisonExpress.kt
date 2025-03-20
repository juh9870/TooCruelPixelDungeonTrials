package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Levitation
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PitfallParticle
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.transitionNow
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog
import com.watabou.utils.Bundle
import com.watabou.utils.PathFinder

class PrisonExpress : Buff() {
    private var announcements = MAX_ANNOUNCEMENTS

    override fun act(): Boolean {
        if (Dungeon.depth >= 6) {
            detach()

            PixelScene.shake(4f, 1f)
            target.buff(Roots::class.java)?.detach()
            prolong(target, Cripple::class.java, Cripple.DURATION)
            GLog.i(Messages.get(PrisonExpress::class.java, "landing"))
            return true
        } else if (Dungeon.depth >= 2 || announcements < MAX_ANNOUNCEMENTS) {
            if (announcements <= 0) {
                Dungeon.level.transitionNow(LevelTransition.Type.REGULAR_EXIT, true)
                return false
            } else {
                affect(target, Roots::class.java, Roots.DURATION)
                target.buff(Levitation::class.java)?.detach()
                val pos = target.pos
                for (i in PathFinder.NEIGHBOURS9) {
                    val cell = pos + i
                    if (!Dungeon.level.solid[cell] || Dungeon.level.passable[cell]) {
                        CellEmitter.floor(pos + i).pour(PitfallParticle.FACTORY4, 0.1f)
                    }
                }

                GLog.i(Messages.get(PrisonExpress::class.java, "announce_$announcements"))

                announcements--
            }
        }
        spend(TICK)
        return true
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(ANNOUNCEMENTS, announcements)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        announcements = bundle.getInt(ANNOUNCEMENTS)
    }

    companion object {
        const val MAX_ANNOUNCEMENTS = 3
        const val ANNOUNCEMENTS = "announcements"
    }
}
