package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.Assets
import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity.DeferedDamage
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog
import com.watabou.noosa.Image
import com.watabou.noosa.audio.Sample
import com.watabou.utils.Bundle
import com.watabou.utils.Random
import kotlin.math.max


class Intoxication : Buff() {
    protected var level: Float = 0f

    init {
        type = buffType.NEGATIVE
    }

    override fun act(): Boolean {
        if (level >= DANGER_1) {
            if (level > DANGER_2 || Random.Float() < (level - DANGER_1) / (DANGER_2 - DANGER_1)) {
                applyMinor(target)
            }

            if (level >= DANGER_2) {
                if (level > DANGER_3 || Random.Float() < (level - DANGER_3) / (DANGER_3 - DANGER_2)) {
                    applyMajor(target)
                }
                if (level > DANGER_3 || Random.Float() < (level - DANGER_3) / (DANGER_3 - DANGER_2)) {
                    applyMinor(target)
                }

                if (level >= DANGER_3) {
                    if (level > DANGER_4 || Random.Float() < (level - DANGER_3) / (DANGER_4 - DANGER_3)) {
                        applyMajor(target)
                    }
                }
            }
        }
        var rnd: Int = Random.Int(4, 6)

        if (level <= DANGER_2) rnd = (rnd * 1.6f).toInt()
        else if (level <= DANGER_3) rnd = (rnd * 1.2f).toInt()

        level -= rnd.toFloat()
        if (level <= 0) detach()
        spend(rnd.toFloat())
        return true
    }

    fun toxicLevel(): String {
        val levels = arrayOf(
            "t_none",
            "t_light",
            "t_medium",
            "t_heavy",
            "t_deadly",
        )
        val l = if (level > DANGER_4) levels[4]
        else if (level > DANGER_3) levels[3]
        else if (level > DANGER_2) levels[2]
        else if (level > DANGER_1) levels[1]
        else levels[0]
        return Messages.get(this, l)
    }

    override fun desc(): String {
        return Messages.get(this, "desc", level.toInt(), toxicLevel())
    }

    override fun iconTextDisplay(): String {
        return level.toInt().toString()
    }

    override fun toString(): String {
        return Messages.get(this, "name")
    }

    override fun icon(): Int {
        return BuffIndicator.POISON
    }

    override fun tintIcon(icon: Image) {
        if (level > DANGER_4) icon.hardlight(0x8000ff)
        else if (level > DANGER_3) icon.hardlight(0xff0000)
        else if (level > DANGER_2) icon.hardlight(0xffaa00)
        else if (level > DANGER_1) icon.hardlight(0xffee00)
        else icon.hardlight(0x00ff00)
    }

    fun set(level: Float) {
        this.level =
            max(this.level.toDouble(), level.toDouble()).toFloat()
    }

    fun extend(duration: Float) {
        val previous = this.level
        this.level += duration
        notifyLevelChange(previous)
    }

    fun processHit(damage: Int, source: Any?) {
        if (damage <= 0) return

        if (source is Hunger ||
            source is DeferedDamage ||
            source is Burning ||
            source is Blob ||
//            source is Countdown ||
            source is RacingTheDeath
        ) return

        var power = 1f * damage / target.HT

        //losing 25% hp equal to 1 base toxic level
        power *= 4 * BASE

        power /= 1.5.toFloat()

        //extra intoxication equal to received damage
        power += damage.toFloat()

        extend(power)
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(LEVEL, level)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        level = bundle.getInt(LEVEL).toFloat()
    }

    private fun notifyLevelChange(previous: Float) {
        if (previous < DANGER_1 && level >= DANGER_1) {
            GLog.w(Messages.get(this, "notify_light"))
        } else if (previous < DANGER_2 && level >= DANGER_2) {
            GLog.n(Messages.get(this, "notify_medium"))
        } else if (previous < DANGER_3 && level >= DANGER_3) {
            GLog.n(Messages.get(this, "notify_heavy"))
            Sample.INSTANCE.play(Assets.Sounds.HEALTH_WARN, 1f, 1.5f)
        } else if (previous < DANGER_4 && level >= DANGER_4) {
            GLog.n(Messages.get(this, "notify_deadly"))
            Sample.INSTANCE.play(Assets.Sounds.HEALTH_CRITICAL, 1f, 1.5f)
        }
    }

    class ToxicWaterTracker : Buff() {
        override fun act(): Boolean {
            if (Dungeon.level.map[target.pos] == Terrain.WATER) {
                affect(target, Intoxication::class.java).extend(
                    Random.NormalIntRange(5, 10).toFloat()
                )
            }
            spend(TICK)
            return true
        }
    }

    companion object {
        const val POION_INTOXICATION: Float = 55f
        const val EXOTIC_INTOXICATION: Float = 80f
        private const val BASE = 50f
        private const val DANGER_1 = BASE * 2f
        private const val DANGER_2 = BASE * 3f
        private const val DANGER_3 = BASE * 4f
        private const val DANGER_4 = BASE * 5f
        private const val LEVEL = "level"

        fun applyMajor(targ: Char) {
            when (Random.Int(6)) {
                0 -> affect(targ, Corrosion::class.java).set(
                    Random.NormalFloat(3f, 5f),
                    targ.HT / 20
                )

                1 -> prolong(targ, Paralysis::class.java, Random.NormalFloat(7f, 13f))
                2 -> prolong(targ, Weakness::class.java, Random.NormalFloat(15f, 25f))
                3 -> prolong(targ, Frost::class.java, Random.NormalFloat(15f, 25f))
                4 -> prolong(targ, Slow::class.java, Random.NormalFloat(20f, 35f))
                5 -> prolong(targ, Degrade::class.java, Random.NormalFloat(15f, 25f))
            }
        }

        fun applyMinor(targ: Char?) {
            when (Random.Int(6)) {
                0 -> affect(targ, Vulnerable::class.java, Random.Float(9f, 15f))
                1 -> prolong(targ, Blindness::class.java, Random.Float(9f, 15f))
                2 -> affect(targ, Chill::class.java, Random.NormalFloat(7f, 13f))
                3 -> if (Modifier.RACING_THE_DEATH.active()) applyMinor(targ)
                else prolong(targ, Vertigo::class.java, Random.NormalFloat(7f, 13f))

                4 -> prolong(targ, Cripple::class.java, Random.NormalFloat(7f, 13f))
                5 -> prolong(targ, Roots::class.java, Random.NormalFloat(7f, 13f))
            }
        }
    }
}