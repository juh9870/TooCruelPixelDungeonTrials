package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff
import com.shatteredpixel.shatteredpixeldungeon.levels.Level
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Arrowhead
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.BossRush
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.BrimstoneNeutralizer
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.CasualApproach
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.ControlledRandomness
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.GoldenBody
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.GrassIgniter
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Insomnia
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Intoxication
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.InvisibleResting
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.OnBossSlainBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.OnXpGainBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Pandemonium
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.PerfectInformation
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.PermaBlind
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.PrisonExpress
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.RacingTheDeath
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.RetieredBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.SafetyBuffer
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.SteelBody
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.SwitchLevelBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.UninspiredToLearnBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.XpMultiplierBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.forEachBuff
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
        if (Modifier.GOLDEN_COLOSSUS.active()) {
            Buff.affect(this, GoldenBody::class.java)
        } else {
            Buff.affect(this, SteelBody::class.java)
        }
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
    if (Modifier.PRISON_EXPRESS.active()) {
        Buff.affect(this, PrisonExpress::class.java)
    }
    if (Modifier.BOSS_RUSH.active()) {
        Buff.affect(this, BossRush::class.java)
    }
    if (Modifier.CASUAL_APPROACH.active()) {
        Buff.affect(this, CasualApproach::class.java)
    }
    if (Modifier.LET_THEM_REST.active()) {
        Buff.affect(this, InvisibleResting::class.java)
    }
    if (Modifier.DOMAIN_OF_HELL.active()) {
        Buff.affect(this, BrimstoneNeutralizer::class.java)
    }
    if (Modifier.DOMAIN_OF_HELL.active()) {
        Buff.affect(this, GrassIgniter::class.java)
    }
    if (Modifier.PERFECT_INFORMATION.active()) {
        Buff.affect(this, PerfectInformation::class.java).pushBackInTime()
    }
    if (Modifier.SAFETY_BUFFER.active()) {
        Buff.affect(this, SafetyBuffer::class.java)
    }
    if (Modifier.UNINSPIRED_TO_LEARN.active()) {
        Buff.affect(this, UninspiredToLearnBuff::class.java)
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
            sprite.parent.add(
                object : Delayer(
                    (
                        Dungeon.level.distance(
                            pos,
                            nextStep,
                        ) - 1
                    ) * CharSprite.DEFAULT_MOVE_INTERVAL,
                ) {
                    init {
                        listener =
                            Listener {
                                if (clearWater) {
                                    GameScene.updateMap(curStep)
                                    CellEmitter.get(curStep).burst(Speck.factory(Speck.STEAM), 5)
                                }
                                GameScene.ripple(curStep)
                            }
                    }
                },
            )
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

fun Hero.switchLevelHook() {
    forEachBuff<SwitchLevelBuff> {
        it.onSwitchLevel()
    }
}

fun Hero.wandProcHook(
    target: Char,
    wand: Wand,
    chargesUsed: Int,
) {
}

fun Hero.wandUsedHook(wand: Wand) {
    if (Modifier.PANDEMONIUM.active()) {
        buff(Pandemonium::class.java)?.wandUsed(wand)
    }
}

fun hungerDisabled(): Boolean = Modifier.CERTAINTY_OF_STEEL.active()

fun regenerationDisabled(): Boolean = Modifier.CERTAINTY_OF_STEEL.active()

fun Hero.subclassChoice(): Array<HeroSubClass> {
    if (Modifier.MULTICLASSING.active()) {
        Random.pushGenerator(Dungeon.seed)
        val classes = ArrayList<HeroSubClass>()

        for (heroClass in HeroClass.entries) {
            if (heroClass == this.heroClass) continue
            classes.add(Random.element(heroClass.subClasses()))
        }

        Random.shuffle(classes)
        Random.popGenerator()
        return classes.slice(0 until 3).toTypedArray()
    } else {
        return heroClass.subClasses()
    }
}

fun Hero.subClassPicked() {
    if (subClass == HeroSubClass.NONE || heroClass.subClasses().contains(subClass)) return

    for (cl in HeroClass.entries) {
        if (!cl.subClasses().contains(subClass)) continue
        when (cl) {
            HeroClass.WARRIOR ->
                Dungeon.level
                    .drop(BrokenSeal().identify(), pos)
                    .sprite
                    ?.drop()

            HeroClass.MAGE ->
                Dungeon.level
                    .drop(MagesStaff().identify(), pos)
                    .sprite
                    ?.drop()

            HeroClass.ROGUE ->
                Dungeon.level
                    .drop(CloakOfShadows().identify(), pos)
                    .sprite
                    ?.drop()

            HeroClass.HUNTRESS ->
                Dungeon.level
                    .drop(SpiritBow().identify(), pos)
                    .sprite
                    ?.drop()

            HeroClass.DUELIST -> {} // shame on her
            HeroClass.CLERIC ->
                Dungeon.level
                    .drop(HolyTome().identify(), pos)
                    .sprite
                    ?.drop()
        }
        break
    }
}

fun Hero.earnExpHook(
    exp: Int,
    source: Class<*>,
): Int {
    var fExp = exp.toFloat()

    forEachBuff<XpMultiplierBuff> {
        fExp *= it.xpMultiplier(source)
    }
    if (Modifier.SKELETON_CREW.active()) fExp *= 2f

    val newExp = fExp.toInt()

    if (!AscensionChallenge::class.java.isAssignableFrom(source)) {
        val diff = newExp - exp
        if (diff > 0) {
            sprite.showStatusWithIcon(CharSprite.POSITIVE, "$diff", FloatingText.EXPERIENCE)
        } else if (diff < 0) {
            sprite.showStatusWithIcon(CharSprite.NEGATIVE, "$diff", FloatingText.EXPERIENCE)
        }
    }

    forEachBuff<OnXpGainBuff> {
        it.onXpGained(newExp, source)
    }

    return newExp
}

fun Hero.onBossSlain() {
    forEachBuff<OnBossSlainBuff> {
        it.onBossSlain()
    }
}
