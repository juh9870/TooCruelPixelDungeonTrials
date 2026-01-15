package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks

import com.shatteredpixel.shatteredpixeldungeon.Assets
import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Levitation
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SuperNovaTracker
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing.KingDamager
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb
import com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.blobs.PATRON_SEED_SOUL
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.blobs.PatronSaintsBlob
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Arrowhead
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Arrowhead.MobArrowhead
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.AtkSkillChangeBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.AttackAmplificationBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.AttackProcBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.AttackSpeedBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.BloodbagBleeding
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.CrystalShield
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.DamageAmplificationBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.DefSkillChangeBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.DefenseProcBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.DelayedBeckon
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.DrRollBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.ForcedPacifismBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.FullSceneUpdater
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.HtBoostBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.HydraBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.InsomniaSlowdown
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.InsomniaSpeed
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Intoxication
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.InvulnerabilityBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.ModifiersAppliedTracker
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.OnDamageTakenBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.OnDeathEffectBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Resizing
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.RevengeFury
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.RevengeRage
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.TargetedResistance
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.TimescaleBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.bombermobBomb
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.destroyWall
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.forEachBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.getFov
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.isLevelBossOrSpecial
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.placeRevealedTrapAndChangeTerrain
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.randomTrap
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.strongDestroy
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.updateFov
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog
import com.watabou.noosa.audio.Sample
import com.watabou.utils.PathFinder
import com.watabou.utils.Random
import com.watabou.utils.Reflection
import kotlin.math.max
import kotlin.math.roundToInt

fun sourceIgnored(src: Any?): Boolean = src is KingDamager

fun Char.recalculateHT() {
    if (this is Hero) {
        this.updateHT(true)
        return
    } else if (this is Mob) {
        // don't touch boss HT, screw that shit
        if (properties().contains(Char.Property.BOSS) || this is NPC) {
            return
        }

        if (this.tcpdData.originalHT < 0) {
            this.tcpdData.originalHT = HT
        }

        val curHPPercentage = HP / HT.toFloat()

        HT = (this.tcpdData.originalHT * htMultiplier()).toInt()

        HP = (HT * curHPPercentage).toInt()
    }
}

fun Char.htMultiplier(): Float {
    var mult = 1f
    forEachBuff<HtBoostBuff> {
        mult *= it.htMultiplier()
    }

    return mult
}

/**
 * Hook which is called when mob takes damage, but before any damage
 * reduction/amplification is applied.
 */
fun Char.incomingDamageHook(
    dmg: Int,
    src: Any?,
): Int {
    if (dmg < 0 || sourceIgnored(src)) {
        return dmg
    }
    return dmg
}

/**
 * Hook which is called when calculating damage multipliers
 *
 * This is called before the champion damage multiplier is applied
 */
@Suppress("NAME_SHADOWING")
fun Char.damageMultiplierHook(
    dmg: Int,
    damage: Float,
    src: Any?,
): Float {
    if (sourceIgnored(src)) {
        return damage
    }
    var damage = damage
    forEachBuff<DamageAmplificationBuff> {
        damage *= it.damageMultiplier(src)
    }
    return damage
}

/**
 * Hook which is called right before the damage is applied to the char's
 * shielding and, subsequently, HP
 */
fun Char.beforeDamageShieldedHook(
    dmg: Int,
    src: Any?,
): Int {
    if (sourceIgnored(src)) {
        return dmg
    }
    if (CrystalShield.blockIncomingDamage(this, dmg)) {
        return -1
    }
    if (this is Hero && Modifier.BLOODBAG.active() && src !is Bleeding) {
        Buff.affect(this, BloodbagBleeding::class.java).add(max(dmg / 2f, 1f))
        return -1
    }
    if ((Modifier.REVENGE.active() || Modifier.REVENGE_FURY.active()) && HP + shielding() <= dmg && this is Mob) {
        updateFov(Dungeon.level)
    }
    return dmg
}

/**
 * Hook which is called when char takes damage, after all damage
 * reduction/amplification is applied, and HP was reduced.
 */
fun Char.damageTakenHook(
    dmg: Int,
    shielded: Int,
    src: Any?,
) {
    if (this is Mob) {
        if (HP <= 0) {
            if (Modifier.CRYSTAL_BLOOD.active() &&
                buff(CrystalShield.DeathMarker::class.java) == null &&
                !properties().contains(
                    Char.Property.BOSS,
                )
            ) {
                HP = 1
                CrystalShield.applyShieldingLayers(this, 5, "barrier x5")
                Buff.affect(this, CrystalShield.DeathMarker::class.java)
            }
        }
        if (!isAlive() && alignment != Char.Alignment.ALLY) {
            val fury = Modifier.REVENGE_FURY.active()
            val rage = Modifier.REVENGE.active()
            val fov = getFov(Dungeon.level)
            if ((HP < 0 && rage) || fury) {
                for (mob in Dungeon.level.mobs) {
                    if (!fov[mob.pos]) continue
                    if (mob is NPC) continue
                    if (mob === this) continue
                    if (mob.alignment == Char.Alignment.ALLY) continue
                    if (rage && HP < 0) Buff.affect(mob, RevengeRage::class.java).add(-HP)
                    if (fury) Buff.append(mob, RevengeFury::class.java, 10f)
                    mob.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3)
                }
            }
        }
    } else if (this is Hero && Modifier.PLAGUE.active() && dmg > 0) {
        Buff.affect(this, Intoxication::class.java)
    }

    if (Modifier.WHIPLASH.active()) {
        doWhiplash(src)
    }

    if (dmg > 0) {
        forEachBuff<OnDamageTakenBuff> {
            it.onDamageTaken(dmg, src)
        }
    }
}

fun Char.doWhiplash(src: Any?) {
    val fov = this.updateFov(Dungeon.level)

    val charPositions = mutableListOf<Int>()
    for (char in Actor.chars()) {
        if (fov[char.pos] && char != this && Dungeon.level.distance(pos, char.pos) > 1) {
            charPositions.add(char.pos)
        }
    }

    val targetCell: Int? =
        if (charPositions.isNotEmpty()) {
            Random.element(charPositions)
        } else {
            val solid = Dungeon.level.solid
            val validCells = mutableListOf<Int>()
            for (i in 0 until Dungeon.level.length()) {
                if (fov[i] && solid[i] && Dungeon.level.distance(pos, i) > 1) {
                    validCells.add(i)
                }
            }
            Random.element(validCells)
        }

    if (targetCell != null) {
        val trajectory =
            Ballistica(
                pos,
                targetCell,
                Ballistica.MAGIC_BOLT,
            )

        WandOfBlastWave.throwChar(this, trajectory, 999, true, false, src)
    }
}

private var drRollChecksRunning = false

fun Char.drRollBonus(): Int {
    if (drRollChecksRunning) return 0

    var bonus = 0
    var mult = 1f
    forEachBuff<DrRollBuff> {
        bonus += it.drRollBonus()
        mult *= it.verySketchyDrMultBonus()
    }

    var baseRoll = 0

    if (mult != 1f) {
        drRollChecksRunning = true
        baseRoll = drRoll()
        drRollChecksRunning = false
    }

    return bonus + (baseRoll * (mult - 1)).roundToInt()
}

/**
 * Hook which is called when char attacks, to calculate flat damage bonus before multipliers
 */
fun Char.attackFlatDamageBonusHook(enemy: Char): Float {
    var bonus = 0f
    forEachBuff<AttackAmplificationBuff> {
        bonus += it.flatAttackBonus()
    }
    return bonus
}

/**
 * Hook which is called when char attacks, to calculate attack damage multiplier
 */
fun Char.attackDamageMultiplierHook(enemy: Char): Float {
    var mult = 1f
    forEachBuff<AttackAmplificationBuff> {
        mult *= it.attackMultiplier()
    }
    return mult
}

/**
 * Hook which is called when char attacks, but before the the damage is passed to enemy's defense proc
 */
fun Char.attackDamageBeforeApplyHook(
    enemy: Char,
    damage: Float,
): Float {
    var bonus = 0f
    forEachBuff<AttackAmplificationBuff> {
        bonus += it.flatAttackBonusPostMult()
    }
    return damage + bonus
}

fun Char.attackProcHook(
    enemy: Char,
    damage: Int,
) {
    forEachBuff<AttackProcBuff> {
        it.attackProc(enemy, damage)
    }
}

fun Char.defenseProcHook(
    enemy: Char,
    damage: Int,
) {
    forEachBuff<DefenseProcBuff> {
        it.defenseProc(enemy, damage)
    }
}

/**
 * Hook which is called when char dies.
 */
fun Char.deathHook(src: Any?) {
    forEachBuff<OnDeathEffectBuff> {
        it.onDeathProc()
    }
    if (this is Mob) {
        if (Modifier.BOMBERMOB.active()) {
            if ((Modifier.CONSTELLATION.active() || Random.Float() < 0.01f) && !isLevelBossOrSpecial()) {
                val nova =
                    Buff.append(
                        Dungeon.hero,
                        SuperNovaTracker::class.java,
                    )
                nova.pos = pos
                nova.harmsAllies = true
                GLog.w(Messages.get(CursedWand::class.java, "supernova"))
            } else {
                Bomb.igniteAt(bombermobBomb(), pos)
            }
        }
        if (Modifier.BODY_TRAPS.active()) {
            val l = Dungeon.level
            val t = l.randomTrap()
            Dungeon.level.placeRevealedTrapAndChangeTerrain(pos, Reflection.newInstance(t))
        }
        if (alignment != Char.Alignment.ALLY) {
            if (Modifier.ARROWHEAD.active()) {
                Buff.affect(Dungeon.hero, Arrowhead::class.java).addStack()
            }
            if (Modifier.PATRON_SAINTS.active()) {
                GameScene.add(Blob.seed(pos, PATRON_SEED_SOUL, PatronSaintsBlob::class.java))
                if (Modifier.PERSISTENT_SAINTS.active()) {
                    GameScene.add(
                        Blob.seed(
                            Dungeon.hero.pos,
                            PATRON_SEED_SOUL,
                            PatronSaintsBlob::class.java,
                        ),
                    )
                }
            }
        }
    }
}

@Suppress("NAME_SHADOWING")
fun Char.speedHook(speed: Float): Float {
    var speed = speed
    for (b in buffs()) {
        if (b is TimescaleBuff) {
            speed *= b.speedFactor()
        }
    }
    return speed
}

@Suppress("NAME_SHADOWING")
fun Mob.attackDelayHook(delay: Float): Float {
    var delay = delay
    for (b in buffs()) {
        if (b is AttackSpeedBuff) {
            delay /= b.attackSpeedFactor()
        }
    }
    return delay
}

fun Char.isInvulnerableHook(effect: Class<*>): Boolean {
    forEachBuff<InvulnerabilityBuff> {
        if (it.isInvulnerable(effect)) return true
    }
    return false
}

fun charHitAcuStatHook(
    attacker: Char,
    defender: Char,
    acuStat: Float,
): Float {
    var stat = acuStat
    for (buff in attacker.buffs()) {
        if (buff is AtkSkillChangeBuff) {
            stat = buff.modifyAtkSkill(stat, defender)
        }
    }
    return stat
}

fun charHitDefStatHook(
    attacker: Char,
    defender: Char,
    defStat: Float,
): Float {
    var stat = defStat
    for (buff in defender.buffs()) {
        if (buff is DefSkillChangeBuff) {
            stat = buff.modifyDefSkill(stat, attacker)
        }
    }
    return stat
}

fun charHitAcuRollHook(
    attacker: Char,
    defender: Char,
    acuRoll: Float,
): Float {
    var roll = acuRoll
    for (buff in attacker.buffs()) {
        if (buff is AtkSkillChangeBuff) {
            roll *= buff.atkRollMultiplier(attacker)
        }
    }
    return roll
}

fun charHitDefRollHook(
    attacker: Char,
    defender: Char,
    defRoll: Float,
): Float {
    var roll = defRoll
    for (buff in defender.buffs()) {
        if (buff is DefSkillChangeBuff) {
            roll *= buff.defRollMultiplier(attacker)
        }
    }
    return roll
}

fun Char.moveHook(
    step: Int,
    travelling: Boolean,
) {
    if (this is Hero && Modifier.BARRIER_BREAKER.active()) {
        val terrain = Dungeon.level.map[step]
        if (terrain == Terrain.OPEN_DOOR || terrain == Terrain.DOOR) {
            Dungeon.level.strongDestroy(step)
            GameScene.updateMap(step)

            Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.25f, 1.5f)
            Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.25f, 1.5f)
            CellEmitter.center(pos).burst(SmokeParticle.FACTORY, 5)

            spendConstant(1f)
        }
    } else if (this is Mob && Modifier.MOLES.active()) {
        var destruction = false
        if (Char.hasProp(this, Char.Property.LARGE) && !Dungeon.level.openSpace[step]) {
            var visibleDestruction = false
            for (o in PathFinder.NEIGHBOURS8) {
                val n = step + o
                if (Dungeon.level.solid[n]) {
                    Dungeon.level.destroyWall(n)
                    destruction = true
                    if (Dungeon.level.heroFOV[step]) {
                        CellEmitter.center(pos).burst(SmokeParticle.FACTORY, 5)
                        visibleDestruction = true
                    }
                }
            }
            if (visibleDestruction) Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.25f, 1.5f)
        }
        val terrain = Dungeon.level.map[step]
        if (Dungeon.level.solid[step] && terrain != Terrain.DOOR && terrain != Terrain.OPEN_DOOR) {
            destruction = true
            Dungeon.level.destroyWall(step)
            if (Dungeon.level.heroFOV[step]) {
                Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.25f, 1.5f)
                CellEmitter.center(pos).burst(SmokeParticle.FACTORY, 5)
            }
        }

        if (destruction) {
            FullSceneUpdater.requestFull()
        }
    }
}

/**
 * Called for when mob takes damage, before all other damage calculations
 */
fun Mob.mobIncomingDamageHook(
    dmg: Int,
    src: Any?,
): Int {
    if (Modifier.CARDINAL_DISABILITY.active()) {
        if (alignment == Char.Alignment.ENEMY) {
            val selfPos = Dungeon.level.cellToPoint(pos)
            val heroPos = Dungeon.level.cellToPoint(Dungeon.hero.pos)
            if (selfPos.x != heroPos.x && selfPos.y != heroPos.y) {
                return -1
            }
        }
    }

    return dmg
}

fun Mob.mobFirstAddedHook() {
    if (this is NPC) {
        return
    }
    applyModifiers()
}

fun Mob.applyModifiers() {
    if (buff(ModifiersAppliedTracker::class.java) != null) return
    Buff.affect(this, ModifiersAppliedTracker::class.java)
    // buff was rejected, can't apply modifiers, wait until the better time
    if (buff(ModifiersAppliedTracker::class.java) == null) return

    if (Modifier.ARROWHEAD.active()) {
        Buff.affect(this, MobArrowhead::class.java)
    }
    if (Modifier.CRYSTAL_SHELTER.active()) {
        Buff.affect(this, CrystalShield::class.java)
        Buff.append(this, CrystalShield.Layer::class.java)
    }
    if (Modifier.PREPARED_ENEMIES.active() && this is Snake) {
        HP *= 4
        HT *= 4
        defenseSkill = 0
    }
    if (Modifier.LOFT.active() && Modifier.MOLES.active()) {
        Buff.affect(this, Levitation::class.java, 2e9f)
    }
    if (Modifier.INSOMNIA.active()) {
        Buff.affect(this, InsomniaSpeed::class.java)
    }
    if (Modifier.DOMAIN_OF_HELL.active()) {
        Buff.affect(this, TargetedResistance::class.java).set(Burning::class.java to 0.9f)
    }
    if (Modifier.JACK_IN_A_BOX.active() && this is Mimic) {
        Buff.affect(this, DelayedBeckon::class.java)
    }
    if (Modifier.CROWD_DIVERSITY.active()) {
        if (buff(Resizing::class.java) == null) {
            Buff.affect(this, Resizing::class.java).multiplyRandom()
        }
    }
    if (Modifier.HYDRA.active()) {
        Buff.affect(this, HydraBuff.EnemyTracker::class.java)
    }
    if (Modifier.FORCED_PACIFISM.active()) {
        Buff.affect(this, ForcedPacifismBuff::class.java)
    }
}

fun Mob.mobBeforeActHook(
    enemyInFOV: Boolean,
    justAlerted: Boolean,
) {
    applyModifiers()
    if (enemyInFOV && Modifier.INSOMNIA.active()) {
        Buff.prolong(this, InsomniaSlowdown::class.java, InsomniaSlowdown.DURATION)
    }
}
