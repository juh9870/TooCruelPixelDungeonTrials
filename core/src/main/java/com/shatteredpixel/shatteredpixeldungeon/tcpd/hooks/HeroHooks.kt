package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand
import com.shatteredpixel.shatteredpixeldungeon.levels.Level
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Arrowhead
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.ControlledRandomness
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Insomnia
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Intoxication
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Pandemonium
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.PermaBlind
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.RacingTheDeath
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.RetieredBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.SteelBody
import com.watabou.noosa.tweeners.Delayer
import com.watabou.noosa.tweeners.Tweener.Listener
import com.watabou.utils.Random


fun Hero.heroLiveHook() {
    if (Modifier.RACING_THE_DEATH.active()) {
        Buff.affect(this, RacingTheDeath::class.java)
    }
    if (Modifier.THUNDERSTRUCK.active()) {
        Buff.affect(this, Arrowhead::class.java).set(9001)
    }
    if (Modifier.BLINDNESS.active()) {
        Buff.affect(this, PermaBlind::class.java)
    }
    if (Modifier.TOXIC_WATER.active()) {
        Buff.affect(this, Intoxication.ToxicWaterTracker::class.java)
    }
    if (Modifier.CERTAINTY_OF_STEEL.active()) {
        Buff.affect(this, SteelBody::class.java)
    }
    if (Modifier.RETIERED.active()) {
        Buff.affect(this, RetieredBuff::class.java)
    }
    if (Modifier.UNSTABLE_ACCESSORIES.active()) {
        Buff.affect(this, ControlledRandomness::class.java)
    }
    if (Modifier.PANDEMONIUM.active()) {
        Buff.affect(this, Pandemonium::class.java)
    }
    if (Modifier.INSOMNIA.active()) {
        Buff.affect(this, Insomnia::class.java)
    }
}

fun Hero.heroSpendConstantHook(time: Float) {
    if (time > 0) {
        buff(RacingTheDeath::class.java)?.tick()
    }
}

@Suppress("NAME_SHADOWING")
fun Hero.moveHook(step: Int): Int {
    var step = step
    if (Modifier.SLIDING.active()) {
        val move: Int = step - pos
        var tilesSlid = 0
        do {
            val curStep = step
            val nextStep = step + move
            if (!Dungeon.level.water[step]) break
            if (Actor.findChar(nextStep) != null) break
            if (!Dungeon.level.passable[nextStep] && !Dungeon.level.avoid[nextStep]) break
            val clearWater = Random.Float() < .20f
            if (clearWater) {
                Level.set(curStep, Terrain.EMPTY)
            }
            sprite.parent.add(object : Delayer(
                (Dungeon.level.distance(
                    pos,
                    nextStep
                ) - 1) * CharSprite.DEFAULT_MOVE_INTERVAL
            ) {
                init {
                    listener = Listener {
                        if (clearWater) {
                            GameScene.updateMap(curStep)
                            CellEmitter.get(curStep).burst(Speck.factory(Speck.STEAM), 5)
                        }
                        GameScene.ripple(curStep)
                    }
                }
            })
            step = nextStep
            tilesSlid++
        } while (true)
        if (tilesSlid > 0) {
            interrupt()
            if (tilesSlid >= Dungeon.level.viewDistance / 2) {
                Dungeon.observe()
                GameScene.updateFog()
            }
        }
    }
    return step
}

fun Hero.wandProcHook(target: Char, wand: Wand, chargesUsed: Int) {

}

fun Hero.wandUsedHook(wand: Wand) {
    if (Modifier.PANDEMONIUM.active()) {
        buff(Pandemonium::class.java)?.wandUsed(wand)
    }
}

fun hungerDisabled(): Boolean {
    return Modifier.CERTAINTY_OF_STEEL.active()
}

fun regenerationDisabled(): Boolean {
    return Modifier.CERTAINTY_OF_STEEL.active()
}