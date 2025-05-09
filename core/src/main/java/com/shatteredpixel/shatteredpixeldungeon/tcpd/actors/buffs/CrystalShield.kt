package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.Assets
import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText
import com.shatteredpixel.shatteredpixeldungeon.effects.ShieldHalo
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator
import com.watabou.noosa.Image
import com.watabou.noosa.audio.Sample

class CrystalShield : Buff() {
    init {
        type = buffType.POSITIVE
    }

    companion object {
        fun applyShieldingLayers(
            target: Char,
            amount: Int,
            text: String,
        ) {
            repeat(amount) {
                append(target, Layer::class.java).fx(true)
            }
            if (Dungeon.level.heroFOV[target.pos]) {
                target.sprite.showStatusWithIcon(
                    CharSprite.POSITIVE,
                    text,
                    FloatingText.SHIELDING,
                )
                Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 1.5f)
            }
        }

        /**
         * Block incoming damage if the target has layers of crystal shield
         *
         * @param target the target to block damage for
         * @param damage the incoming damage
         * @return true if the damage was blocked, false otherwise
         */
        fun blockIncomingDamage(
            target: Char,
            damage: Int,
        ): Boolean {
            target.buff(Layer::class.java)?.let { layer ->
                val crystal = target.buff(CrystalShield::class.java)
                if (crystal != null && damage <= crystal.damageThreshold()) {
                    target.sprite.showStatus(
                        CharSprite.POSITIVE,
                        Messages.get(CrystalShield::class.java, "blocked"),
                    )
                    return true
                }

                layer.detach()

                val hasMoreLayers = target.buff(Layer::class.java) != null

                if (!hasMoreLayers) {
                    if (target.buff(DeathMarker::class.java) != null) {
                        // Directly set HP to zero. This function is only
                        // called from damage method, so it will handle the actual death
                        target.HP = 0
                        return false
                    }
                    crystal?.breakShield()
                } else {
                    if (Dungeon.level.heroFOV[target.pos]) {
                        Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.25f, 1.5f)
                        target.sprite.showStatus(
                            CharSprite.NEGATIVE,
                            Messages.get(CrystalShield::class.java, "layer_broken"),
                        )
                    }
                }
                return true
            }

            return false
        }
    }

    fun damageThreshold(): Int = Dungeon.scalingDepth() / 2 + 1

    override fun act(): Boolean {
        if (target.buff(Layer::class.java) == null) {
            applyShieldingLayers(target, 1, Messages.get(this, "restored"))
        }
        diactivate()
        return true
    }

    fun active(): Boolean = target.buff(Layer::class.java) != null

    fun breakShield() {
        timeToNow()
        postpone(repairTime())

        if (Dungeon.level.heroFOV[target.pos]) {
            Splash.at(target.sprite.center(), 0x22ffe1, 10)
            Sample.INSTANCE.play(Assets.Sounds.SHATTER)
            target.sprite.showStatus(
                CharSprite.NEGATIVE,
                Messages.get(CrystalShield::class.java, "shattered"),
            )
        }
    }

    private fun repairTime(): Float = 10f

    override fun icon(): Int = if (active()) BuffIndicator.ARMOR else BuffIndicator.VULNERABLE

    override fun tintIcon(icon: Image) {
        icon.hardlight(1f, 0.5f, 1.5f)
    }

    override fun desc(): String =
        if (active()) {
            Messages.get(
                this,
                "desc",
                damageThreshold(),
            )
        } else {
            Messages.get(
                this,
                "desc_inactive",
                dispTurns(visualcooldown()),
            )
        }

    class Layer :
        Buff(),
        DefSkillChangeBuff {
        var shield: ShieldHalo? = null

        init {
            type = buffType.POSITIVE
        }

        override fun fx(on: Boolean) {
            if (on) {
                if (shield != null) shield?.killAndErase()
                shield = ShieldHalo(target.sprite)
                shield?.hardlight(0.5f, 1f, 2f)
                GameScene.effect(shield)
            } else {
                shield?.putOut()
            }
        }

        override fun icon(): Int = BuffIndicator.ARMOR

        override fun tintIcon(icon: Image) {
            icon.hardlight(0.5f, 1f, 2f)
        }

        override fun desc(): String =
            if (target.buff(CrystalShield::class.java) != null) {
                Messages.get(this, "desc")
            } else {
                Messages.get(this, "desc_standalone")
            }

        // Can't dodge while having a crystal shield layer
        override fun defRollMultiplier(attacker: Char): Float = 0f
    }

    class DeathMarker : Buff() {
        init {
            type = buffType.NEUTRAL
        }

        override fun icon(): Int = BuffIndicator.CORRUPT

        override fun tintIcon(icon: Image) {
            icon.hardlight(0.5f, 1f, 2f)
        }
    }
}
