package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.Statistics
import com.shatteredpixel.shatteredpixeldungeon.actors.Char
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade
import com.shatteredpixel.shatteredpixeldungeon.levels.Level
import com.shatteredpixel.shatteredpixeldungeon.levels.Level.TIME_TO_RESPAWN
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.Exterminating
import com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs.MindVisionExtBuff
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.furrowCell
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyBoxed
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyCursed
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyDomainOfHell
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyDrought
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyExtermination
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyHolyWater
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyInYourFace
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyJackInTheBox
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyLoft
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyLootParadise
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyMimics
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyOverTheEdge
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyProtectedGoods
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyRecursiveHierarchy
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applySecondTry
import com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks.level.applyThunderstruck
import com.shatteredpixel.shatteredpixeldungeon.tcpd.modifier.Thoughtless
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage
import com.watabou.noosa.Game
import com.watabou.utils.BArray
import com.watabou.utils.GameMath
import com.watabou.utils.PathFinder
import kotlin.math.max
import kotlin.math.min

fun RegularLevel.createItemsHook() {
    if (Modifier.HEAD_START.active() && Dungeon.depth == 1) {
        val nUpgrades = headStartRequiredSoU() - Dungeon.LimitedDrops.UPGRADE_SCROLLS.count
        val nStrength = headStartRequiredPoS() - Dungeon.LimitedDrops.STRENGTH_POTIONS.count
        repeat(nUpgrades) {
            drop(ScrollOfUpgrade(), placeItemPos())
            Dungeon.LimitedDrops.UPGRADE_SCROLLS.count++
        }
        repeat(nStrength) {
            drop(PotionOfStrength(), placeItemPos())
            Dungeon.LimitedDrops.STRENGTH_POTIONS.count++
        }
    }
}

fun headStartRequiredPoS(): Int = if (Modifier.PRISON_EXPRESS.active()) 2 else 1

fun headStartRequiredSoU(): Int = if (Modifier.PRISON_EXPRESS.active()) 3 else 2

@RequiresOptIn(
    message = "Level creation hooks should only be called from the level post create hook",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class LevelCreationHooks

@OptIn(LevelCreationHooks::class)
fun Level.postCreateHook() {
    if (Modifier.DROUGHT.active()) {
        applyDrought()
    }
    if (Modifier.SECOND_TRY.active()) {
        applySecondTry()
    }
    if (Modifier.HOLY_WATER.active()) {
        applyHolyWater()
    }
    if (Modifier.LOFT.active()) {
        applyLoft()
    }
    if (Modifier.LOOT_PARADISE.active()) {
        applyLootParadise()
    }
    if (Modifier.CURSED.active()) {
        applyCursed()
    }
    if (Modifier.MIMICS.active()) {
        applyMimics()
    }
    if (Modifier.IN_YOUR_FACE.active()) {
        applyInYourFace()
    }
    if (Modifier.EXTERMINATION.active()) {
        applyExtermination()
    }
    if (Modifier.JACK_IN_A_BOX.active()) {
        applyJackInTheBox()
    }
    if (Modifier.BOXED.active()) {
        applyBoxed()
    }
    if (Modifier.RECURSIVE_HIERARCHY.active()) {
        applyRecursiveHierarchy()
    }
    if (Modifier.THUNDERSTRUCK.active()) {
        applyThunderstruck()
    }
    if (Modifier.OVER_THE_EDGE.active()) {
        applyOverTheEdge()
    }
    if (Modifier.DOMAIN_OF_HELL.active()) {
        applyDomainOfHell()
    }
    if (Modifier.PROTECTED_GOODS.active()) {
        applyProtectedGoods()
    }
}

@Suppress("NAME_SHADOWING")
fun Level.updateBlockingFovHook(
    c: Char,
    modifiableBlocking: BooleanArray,
    blocking: BooleanArray?,
): BooleanArray? {
    var blocking = blocking
    if (c is Hero || c.alignment == Char.Alignment.ALLY) {
        if (Modifier.LOFT.active()) {
            blocking = blocking ?: initBlocking(modifiableBlocking)
            for (i in blocking.indices) {
                if (!blocking[i] && pit[i]) {
                    blocking[i] = true
                }
            }
        }
        if (Modifier.BULKY_FRAME.active()) {
            val shrouding = Modifier.SHROUDING_PRESENCE.active()
            var pos: Int
            blocking = blocking ?: initBlocking(modifiableBlocking)
            for (mob in mobs) {
                if (mob.alignment == Char.Alignment.ALLY) continue
                pos = mob.pos
                blocking[pos] = true
                if (shrouding) {
                    for (c in PathFinder.NEIGHBOURS8) {
                        blocking[pos + c] = true
                    }
                }
            }
        }
    }
    return blocking
}

fun Level.updateHeroMindFovHook(
    h: Hero,
    heroMindFov: BooleanArray,
) {
    val shadowFov = BooleanArray(length())
    for (mob in mobs.toTypedArray()) {
        var heroDistance = -1
        val pos = mob.pos
        for (buff in mob.buffs().toTypedArray()) {
            if (buff is MindVisionExtBuff) {
                var radius = buff.revealRadius()
                if (radius <= 0) {
                    continue
                }
                if (radius > ShadowCaster.MAX_DISTANCE) {
                    radius = ShadowCaster.MAX_DISTANCE
                }
                val maxHeroDistance = buff.maxHeroDistance()
                if (maxHeroDistance >= 0 && heroDistance < 0) {
                    heroDistance = distance(pos, h.pos)
                }
                if (maxHeroDistance < 0 || maxHeroDistance < heroDistance) {
                    if (radius == 1) {
                        for (i in PathFinder.NEIGHBOURS9) {
                            heroMindFov[pos + i] = true
                        }
                    } else {
                        val w = width()
                        val x = pos % w
                        val y = pos / w
                        ShadowCaster.castShadow(x, y, w, shadowFov, losBlocking, radius)
                        BArray.or(heroMindFov, shadowFov, heroMindFov)
                    }
                }
            }
        }
    }
}

fun dungeonObserveHook(dist: Int) {
    val level = Dungeon.level
    for (mob in level.mobs.toTypedArray()) {
        var heroDistance = -1
        val pos = mob.pos
        for (buff in mob.buffs().toTypedArray()) {
            if (buff is MindVisionExtBuff) {
                var radius = buff.revealRadius()
                if (radius <= 0) {
                    continue
                }
                if (radius > ShadowCaster.MAX_DISTANCE) {
                    radius = ShadowCaster.MAX_DISTANCE
                }
                val maxHeroDistance = buff.maxHeroDistance()
                if (maxHeroDistance >= 0 && heroDistance < 0) {
                    heroDistance = level.distance(pos, Dungeon.hero.pos)
                }

                if (maxHeroDistance < 0 || maxHeroDistance < heroDistance) {
                    if (radius == 1) {
                        BArray.or(
                            level.visited,
                            level.heroFOV,
                            pos - 1 - level.width(),
                            3,
                            level.visited,
                        )
                        BArray.or(level.visited, level.heroFOV, pos - 1, 3, level.visited)
                        BArray.or(
                            level.visited,
                            level.heroFOV,
                            pos - 1 + level.width(),
                            3,
                            level.visited,
                        )
                        GameScene.updateFog(pos, 2)
                        continue
                    }
                    val w = level.width()
                    val x = pos % w
                    val y = pos / w
                    val l = max(0, (x - dist))
                    val r = min((x + dist), (level.width() - 1))
                    val t = max(0, (y - dist))
                    val b = min((y + dist), (level.height() - 1))
                    val width = r - l + 1

                    var pos1 = l + t * Dungeon.level.width()
                    for (i in t..b) {
                        BArray.or(
                            level.visited,
                            level.heroFOV,
                            pos,
                            width,
                            Dungeon.level.visited,
                        )
                        pos1 += Dungeon.level.width()
                    }
                    GameScene.updateFog(pos, dist)
                }
            }
        }
    }
}

private var bypassTransitionLimitations = false

fun Level.activateTransitionHook(
    hero: Hero,
    transition: LevelTransition,
): Boolean {
    if (bypassTransitionLimitations) return true
    if (Modifier.EXTERMINATION.active()) {
        if (!Exterminating.exterminationDone(this)) return false
    }
    val crumbling = Modifier.CRUMBLED_STAIRS.active()
    val prison = Modifier.PRISON_EXPRESS.active()
    val bossRush = Modifier.BOSS_RUSH.active()
    if (Statistics.amuletObtained) {
        if (crumbling && transition.destDepth >= Dungeon.depth) {
            Game.runOnRenderThread {
                GameScene.show(
                    WndMessage(
                        Messages.get(
                            Modifier::class.java,
                            "crumbled_stairs_no_distractions",
                        ),
                    ),
                )
            }
            return false
        }
    } else {
        if (
            (
                crumbling ||
                    (prison && Dungeon.depth <= 6) ||
                    (bossRush && Dungeon.depth % 5 == 0)
            ) &&
            transition.type == LevelTransition.Type.REGULAR_ENTRANCE
        ) {
            Game.runOnRenderThread {
                GameScene.show(
                    WndMessage(
                        Messages.get(
                            Hero::class.java,
                            "leave",
                        ),
                    ),
                )
            }
            return false
        }
    }

    if (Modifier.THOUGHTLESS.active()) {
        return Thoughtless.confirmAffectedItems(hero, transition)
    }
    return true
}

fun beforeTransitionHook() {
    if (Modifier.THOUGHTLESS.active()) {
        Thoughtless.clearAffectedItems()
    }
}

@Suppress("NAME_SHADOWING")
fun Level.respawnCooldownHook(cooldown: Float): Float {
    var cooldown = cooldown
    if (Modifier.RESURRECTION.active()) {
        return 1f
    } else if (Modifier.REPOPULATION.active()) {
        cooldown *= GameMath.gate(0f, 1.1f * mobCount().toFloat() / mobLimit() - 0.1f, 1f)
        if (Modifier.FRACTAL_HIVE.active()) {
            cooldown = min(cooldown, TIME_TO_RESPAWN / 2f)
        }
    }
    return cooldown
}

fun Level.transitionNow(
    type: LevelTransition.Type,
    force: Boolean,
) {
    val tr = getTransition(type)
    if (tr != null) {
        if (force) bypassTransitionLimitations = true
        activateTransition(Dungeon.hero, tr)
        bypassTransitionLimitations = false
    }
}

private fun Level.initBlocking(modifiableBlocking: BooleanArray): BooleanArray {
    System.arraycopy(
        losBlocking,
        0,
        modifiableBlocking,
        0,
        modifiableBlocking.size,
    )

    return modifiableBlocking
}

private fun RegularLevel.placeItemPos(roomType: Class<out Room?>? = null): Int {
    val cell: Int =
        if (roomType != null) randomDropCellExposedHook(roomType) else randomDropCellExposedHook()

    furrowCell(cell)
    return cell
}
