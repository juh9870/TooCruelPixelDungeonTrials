package com.shatteredpixel.shatteredpixeldungeon.tcpd

import com.shatteredpixel.shatteredpixeldungeon.Challenges
import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.RatSkull
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.asBits
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.asBytes
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.assertEq
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.decodeBase58
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.encodeToBase58String
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.trimEnd
import com.watabou.utils.BArray
import com.watabou.utils.Bundlable
import com.watabou.utils.Bundle
import com.watabou.utils.DeviceCompat

enum class Modifier(
    val id: Int,
    locString: String? = null,
    val dependencies: Array<Int> = emptyArray()
) {
    // Vanilla challenges
    CHAMPION_ENEMIES(7, locString = "champion_enemies"),
    COLOSSEUM(32, dependencies = arrayOf(CHAMPION_ENEMIES.id)),
    STRONGER_BOSSES(8, locString = "stronger_bosses"),
    ON_DIET(0, locString = "no_food"),
    FAITH_ARMOR(1, locString = "no_armor"),
    PHARMACOPHOBIA(2, locString = "no_healing"),
    BARREN_LAND(3, locString = "no_herbalism") {
        override fun _isItemBlocked(item: Item): Boolean {
            return item is Dewdrop
        }
    },
    SWARM_INTELLIGENCE(4, locString = "swarm_intelligence"),
    DARKNESS(5, locString = "darkness"),
    FORBIDDEN_RUNES(6, locString = "no_scrolls"),

    // Custom content!
    CARDINAL_DISABILITY(9),
    RACING_THE_DEATH(10),
    HORDE(11) {
        override fun _nMobsMult(): Float {
            return 2f
        }
    },
    INVASION(12),
    GREAT_MIGRATION(13, dependencies = arrayOf(INVASION.id)),
    MUTAGEN(14),
    EVOLUTION(15, dependencies = arrayOf(MUTAGEN.id)) {
        override fun _isItemBlocked(item: Item): Boolean {
            return item is RatSkull
        }
    },
    ROTTEN_LUCK(16),
    ARROWHEAD(17),
    THUNDERSTRUCK(18, dependencies = arrayOf(ARROWHEAD.id)),
    SECOND_TRY(19),
    CRYSTAL_SHELTER(20),
    CRYSTAL_BLOOD(21),
    DEEPER_DANGER(22),
    HEAD_START(23),
    BLINDNESS(24),
    BLOODBAG(25),
    REVENGE(26),
    REVENGE_FURY(27),
    PREPARED_ENEMIES(28),
    REPEATER(29),
    DUPLICATOR(30),
    EXTREME_CAUTION(31),
    PATRON_SAINTS(33),
    PERSISTENT_SAINTS(34, dependencies = arrayOf(PATRON_SAINTS.id)),
    HOLY_WATER(35),
    INTOXICATION(36),
    PLAGUE(37),
    TOXIC_WATER(38),
    CERTAINTY_OF_STEEL(39) {
        override fun _isItemBlocked(item: Item): Boolean {
            return item is ChaliceOfBlood
        }
    },
    PARADOX_LEVELGEN(40),
    RETIERED(41),
    UNTIERED(42, dependencies = arrayOf(RETIERED.id)),
    UNSTABLE_ACCESSORIES(43),
    PANDEMONIUM(44),
    BARRIER_BREAKER(45),
    MOLES(46),
    LOFT(47),
    BULKY_FRAME(48),
    SLIDING(49),
    INSOMNIA(50),
    LOOT_PARADISE(51),
    ;

    companion object {
        val ALL: Array<Modifier> = Modifier.entries.sortedBy { it.id }.toTypedArray()

        init {
            if (ALL.last().id != ALL.size - 1) {
                throw IllegalStateException("Modifier IDs contain gaps!")
            }
        }

        fun fromVanilla(challengeId: Int): Modifier {
            return when (challengeId) {
                Challenges.NO_FOOD -> ON_DIET
                Challenges.NO_ARMOR -> FAITH_ARMOR
                Challenges.NO_HEALING -> PHARMACOPHOBIA
                Challenges.NO_HERBALISM -> BARREN_LAND
                Challenges.SWARM_INTELLIGENCE -> SWARM_INTELLIGENCE
                Challenges.DARKNESS -> DARKNESS
                Challenges.NO_SCROLLS -> FORBIDDEN_RUNES
                Challenges.CHAMPION_ENEMIES -> CHAMPION_ENEMIES
                Challenges.STRONGER_BOSSES -> STRONGER_BOSSES
                else -> throw IllegalArgumentException("Unknown vanilla challenge id: $challengeId")
            }
        }
    }

    private val localizationKey = locString ?: name.lowercase()
    private val localizationClass =
        if (locString == null) Modifier::class.java else Challenges::class.java

    fun localizedName(): String {
        return Messages.get(localizationClass, localizationKey)
    }

    fun localizedDesc(): String {
        return Messages.get(localizationClass, localizationKey + "_desc")
    }

    open fun _isItemBlocked(item: Item): Boolean {
        return false
    }

    open fun _nMobsMult(): Float {
        return 1f
    }

    fun active() = Dungeon.tcpdData.modifiers.isEnabled(this)
}

class Modifiers() : Bundlable {
    private val modifiers: BooleanArray = BooleanArray(Modifier.entries.size)

    constructor(modifiers: BooleanArray) : this() {
        modifiers.copyInto(this.modifiers)
    }

    constructor(vararg modifiers: Modifier) : this() {
        for (modifier in modifiers) {
            enable(modifier)
        }
    }

    companion object {
        fun deserializeFromString(encoded: String): Modifiers {
            val bits = encoded.decodeBase58().asBits()
            return Modifiers(bits.copyOf(Modifier.entries.size))
        }

        fun debugModeActive(): Boolean {
            return DeviceCompat.isDebug()
        }

        const val MODIFIERS = "modifiers"

        init {
            doTests()
        }
    }

    fun asRaw(): BooleanArray {
        return modifiers.copyOf()
    }

    fun isChallenged(): Boolean {
        return modifiers.any { it }
    }

    fun activeChallengesCount(): Int {
        return modifiers.count { it }
    }

    fun isEnabled(modifier: Modifier): Boolean {
        return modifiers[modifier.id]
    }

    fun isVanillaEnabled(challengeId: Int): Boolean {
        return isEnabled(Modifier.fromVanilla(challengeId))
    }

    fun enable(modifier: Modifier) {
        modifiers[modifier.id] = true

        modifier.dependencies.forEach {
            enable(Modifier.ALL[it])
        }
    }

    fun disable(modifier: Modifier) {
        modifiers[modifier.id] = false

        for (mod in Modifier.entries) {
            if (mod.dependencies.contains(modifier.id)) {
                disable(mod)
            }
        }
    }

    fun toggle(modifier: Modifier) {
        if (isEnabled(modifier)) {
            disable(modifier)
        } else {
            enable(modifier)
        }
    }

    fun enableFrom(other: Modifiers) {
        BArray.setFalse(modifiers)
        for (entry in Modifier.entries) {
            if (other.isEnabled(entry)) {
                enable(entry)
            }
        }
    }

    fun disableAll() {
        BArray.setFalse(modifiers)
    }

    fun isItemBlocked(item: Item): Boolean {
        return Modifier.entries.any { modifiers[it.id] && it._isItemBlocked(item) }
    }

    fun nMobsMult(): Float {
        var mult = 1f
        for (modifier in Modifier.entries) {
            if (modifiers[modifier.id]) {
                mult *= modifier._nMobsMult()
            }
        }
        return mult
    }

    fun scalingDepthBonus(): Int {
        return if (isEnabled(Modifier.DEEPER_DANGER)) 10 else 0
    }

    override fun restoreFromBundle(bundle: Bundle) {
        bundle.getBooleanArray(MODIFIERS).copyInto(modifiers)
    }

    override fun storeInBundle(bundle: Bundle) {
        bundle.put(MODIFIERS, modifiers)
    }

    fun serializeToString(): String {
        val str = modifiers.asBytes(false).trimEnd().encodeToBase58String()
        if (str.all { it == '1' }) return ""
        return str
    }
}

private fun doTests() {
    val mods = booleanArrayOf(
        true,
        false,
        true,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false
    )
    val newMods = booleanArrayOf(true, false, true)

    val ogBytes = mods.asBytes(false).trimEnd()
    val newBytes = newMods.asBytes(false).trimEnd()

    val ogBase58 = ogBytes.encodeToBase58String()
    val newBase58 = newBytes.encodeToBase58String()

    assertEq(ogBase58, newBase58)
}