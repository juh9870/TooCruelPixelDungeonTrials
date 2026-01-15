package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.Assets
import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.updateFov
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator
import com.watabou.noosa.Image
import com.watabou.noosa.audio.Sample
import com.watabou.utils.Bundle
import com.watabou.utils.Random
import kotlin.math.max

class SafetyBuffer : Buff() {
    var stacks = MAX_STACKS
    var damage = SMITE_DAMAGE

    init {
        type = buffType.POSITIVE
    }

    fun refresh(): SafetyBuffer {
        stacks = MAX_STACKS
        return this
    }

    fun set(damage: Int): SafetyBuffer {
        if (damage > this.damage) this.damage = damage
        return this
    }

    override fun act(): Boolean {
        if (stacks <= 0) {
            detach()
            return true
        }

        val fov = target.updateFov(Dungeon.level)

        val validMobs = mutableListOf<Mob>()

        for (mob in Dungeon.level.mobs) {
            if (mob != target &&
                fov[mob.pos] &&
                mob.alignment != Alignment.ALLY &&
                mob !is NPC &&
                mob.buff(SafetySmite::class.java) == null &&
                mob.buff(SafetySmitePrep::class.java) == null
            ) {
                validMobs.add(mob)
            }
        }

        Random.shuffle(validMobs)
        for (mob in validMobs) {
            hitMob(mob)
        }

        spend(TICK)
        return true
    }

    private fun hitMob(mob: Mob) {
        affect(mob, SafetySmitePrep::class.java).set(damage)
    }

    override fun icon(): Int = BuffIndicator.WEAPON

    override fun tintIcon(icon: Image?) {
        icon?.hardlight(0x33FFFF)
    }

    override fun iconFadePercent(): Float = max(0f, (MAX_STACKS - stacks) / MAX_STACKS.toFloat())

    override fun iconTextDisplay(): String = stacks.toString()

    override fun desc(): String = Messages.get(this, "desc", stacks)

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(STACKS, stacks)
        bundle.put(DAMAGE, damage)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        stacks = bundle.getInt(STACKS)
        if (bundle.contains(DAMAGE)) {
            damage = bundle.getInt(DAMAGE)
        }
    }

    class SafetySmitePrep : Buff() {
        var damage = SMITE_DAMAGE

        fun set(damage: Int): SafetySmitePrep {
            this.damage = damage
            return this
        }

        override fun act(): Boolean {
            val hero = Dungeon.hero
            if (hero == null) {
                detach()
                return true
            }
            val safetyBuff = hero.buff(SafetyBuffer::class.java) ?: return true.also { detach() }
            if (safetyBuff.stacks <= 0 || !Dungeon.level.heroFOV[target.pos]) {
                detach()
                return true
            }

            affect(target, SafetySmite::class.java).set(damage)
            affect(target, Paralysis::class.java, Paralysis.DURATION)
            Sample.INSTANCE.play(Assets.Sounds.LIGHTNING)
            if (hero.sprite != null && target.sprite != null) {
                target.sprite.parent.add(
                    Lightning(
                        hero.sprite.center(),
                        target.sprite.center(),
                        null,
                    ),
                )
            }
            safetyBuff.stacks--
            detach()
            return true
        }

        override fun attachTo(target: Char?): Boolean =
            super.attachTo(target).also {
                // slight random to mess up ordering of smites
                spend(TICK * Random.Float(1f, 1.01f))
            }

        override fun storeInBundle(bundle: Bundle) {
            super.storeInBundle(bundle)
            bundle.put(DAMAGE, damage)
        }

        override fun restoreFromBundle(bundle: Bundle) {
            super.restoreFromBundle(bundle)
            if (bundle.contains(DAMAGE)) {
                damage = bundle.getInt(DAMAGE)
            }
        }
    }

    class SafetySmite :
        Buff(),
        DamageIconSource {
        var damage = SMITE_DAMAGE

        fun set(damage: Int): SafetySmite {
            this.damage = damage
            return this
        }

        override fun act(): Boolean {
            target.buff(Paralysis::class.java)?.detach()
            target.damage(damage, this)

            val origin = target.sprite.center()
            origin.y -= target.sprite.camera().screenHeight()

            if (Dungeon.level.heroFOV[target.pos]) {
                target.sprite?.parent?.add(
                    Lightning(
                        origin,
                        target.sprite.center(),
                        null,
                    ),
                )
                Sample.INSTANCE.play(Assets.Sounds.LIGHTNING)
                Sample.INSTANCE.play(Assets.Sounds.BLAST)
            }
            detach()
            return true
        }

        override fun damageIcon(): Int = FloatingText.AMULET

        override fun attachTo(target: Char?): Boolean =
            super.attachTo(target).also {
                spend(TICK)
            }

        override fun storeInBundle(bundle: Bundle) {
            super.storeInBundle(bundle)
            bundle.put(DAMAGE, damage)
        }

        override fun restoreFromBundle(bundle: Bundle) {
            super.restoreFromBundle(bundle)
            if (bundle.contains(DAMAGE)) {
                damage = bundle.getInt(DAMAGE)
            }
        }
    }

    companion object {
        private const val STACKS = "stacks"
        private const val DAMAGE = "damage"
        private const val MAX_STACKS = 10
        const val SMITE_DAMAGE = 10
    }
}
