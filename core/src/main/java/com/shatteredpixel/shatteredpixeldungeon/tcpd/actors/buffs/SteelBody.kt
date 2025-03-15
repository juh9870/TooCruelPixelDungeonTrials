package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.badlogic.gdx.math.MathUtils
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import com.watabou.utils.Bundle
import com.watabou.utils.Random

class SteelBody : NoDetachShieldBuff() {
    init {
        // Act after the Hero and other healing effects
        actPriority = HERO_PRIO - 2
    }

    var partialShielding = 0f

    override fun attachTo(target: Char?): Boolean {
        return super.attachTo(target).also { attached ->
            if (attached) {
                if (this.target.HP == this.target.HP && shielding() == 0) {
                    this.target.HP = 1
                    this.incShield(this.target.HT)
                }
            }
        }
    }

    override fun act(): Boolean {
        if (target.HP > 1) {
            var addShield = (target.HP - 1).toFloat()
            val reductionThreshold = target.HT
            val maxShield = target.HT * 2
            if (shielding() > reductionThreshold) {
                val ratio = MathUtils.clamp(
                    (maxShield - shielding()) / reductionThreshold.toFloat(),
                    0f,
                    1f
                )
                addShield *= ratio
            }

            addShield += partialShielding

            if (addShield > 0) {
                val intAdd = addShield.toInt()
                partialShielding = addShield - intAdd
                incShield(intAdd)
            }

            if (shielding() > maxShield) {
                decShield(shielding() - maxShield)
            }

            target.HP = 1
        }
        target.buff(MagicalSleep::class.java)?.let {
            GameScene.add(
                Blob.seed(
                    target.pos, 1,
                    Electricity::class.java
                )
            )

            // in case if the target is immune to electricity, wake them up after a while
            if (Random.Float() < 0.1f) {
                it.detach()
            }
        }

        spend(TICK)
        return true
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(PARTIAL_SHIELDING, partialShielding)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        partialShielding = bundle.getFloat(PARTIAL_SHIELDING)
    }

    companion object {
        private const val PARTIAL_SHIELDING = "partialShielding"
    }
}