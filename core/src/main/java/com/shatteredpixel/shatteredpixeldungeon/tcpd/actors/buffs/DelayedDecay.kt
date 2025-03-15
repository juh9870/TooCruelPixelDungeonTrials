package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.Assets
import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle
import com.watabou.noosa.audio.Sample
import com.watabou.utils.Bundle

/**
 * A buff that kills the target after a delay
 *
 * This bypassed the damage system and kills the target directly
 *
 * A target killed this way doesn't give any experience
 */
class DelayedDecay : Buff(), InvulnerabilityBuff {
    var fx: Boolean = false
    var invulnerable = false

    fun set(delay: Float, fx: Boolean, invulnerable: Boolean) {
        timeToNow()
        postpone(delay)
        this.invulnerable = invulnerable
        this.fx = fx
    }

    override fun act(): Boolean {
        if (fx && Dungeon.level.heroFOV[target.pos]) {
            Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.25f)
            target.sprite.emitter().burst(ShadowParticle.UP, 10)
        }
        invulnerable = false
        if (target is Mob) {
            val mob = target as Mob
            mob.EXP = 0
        }
        target.destroy()
        target.sprite.die()
        spend(TICK)
        return true
    }

    override fun isInvulnerable(effect: Class<out Any>): Boolean {
        return invulnerable
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(FX, fx)
        bundle.put(INVULNERABLE, invulnerable)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        fx = bundle.getBoolean(FX)
        invulnerable = bundle.getBoolean(INVULNERABLE)
    }

    companion object {
        const val FX: String = "show_death_fx"
        const val INVULNERABLE: String = "invulnerable"
    }
}