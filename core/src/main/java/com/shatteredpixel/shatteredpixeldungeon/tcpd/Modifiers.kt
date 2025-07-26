package com.shatteredpixel.shatteredpixeldungeon.tcpd

import com.shatteredpixel.shatteredpixeldungeon.Challenges
import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.RatSkull
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SaltCube
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.TextureDescriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.descriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.UnorderedPair
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.asBits
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.asBytes
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.assertEq
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.decodeBase58
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.encodeToBase58String
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.trimEnd
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons
import com.watabou.utils.BArray
import com.watabou.utils.Bundlable
import com.watabou.utils.Bundle
import com.watabou.utils.DeviceCompat

enum class Modifier(
    val id: Int,
    locString: String? = null,
    val dependencies: Array<Int> = emptyArray(),
    tags: Array<Tag>,
) {
    // Vanilla challenges
    CHAMPION_ENEMIES(
        7,
        locString = "champion_enemies",
        tags = arrayOf(Tag.ENEMY, Tag.COMBAT, Tag.NEW_STUFF),
    ),
    COLOSSEUM(
        32,
        dependencies = arrayOf(CHAMPION_ENEMIES.id),
        tags = arrayOf(Tag.EXTREME, Tag.ENEMY),
    ),
    STRONGER_BOSSES(8, locString = "stronger_bosses", tags = arrayOf(Tag.COMBAT, Tag.BOSS)),
    ON_DIET(0, locString = "no_food", tags = arrayOf(Tag.HERO, Tag.ITEM)),
    FAITH_ARMOR(1, locString = "no_armor", tags = arrayOf(Tag.HERO, Tag.ITEM)),
    PHARMACOPHOBIA(2, locString = "no_healing", tags = arrayOf(Tag.HERO, Tag.ITEM)),
    BARREN_LAND(3, locString = "no_herbalism", tags = arrayOf(Tag.ITEM, Tag.LEVEL)) {
        override fun _isItemBlocked(item: Item): Boolean = item is Dewdrop
    },
    DROUGHT(71, tags = arrayOf(Tag.LEVEL)) {
        override fun _nGrassMult(): Float = 0.5f

        override fun _nWaterMult(): Float = 0.5f
    },
    SWARM_INTELLIGENCE(4, locString = "swarm_intelligence", tags = arrayOf(Tag.ENEMY)),
    DARKNESS(5, locString = "darkness", tags = arrayOf(Tag.LEVEL, Tag.HERO)),
    FORBIDDEN_RUNES(6, locString = "no_scrolls", tags = arrayOf(Tag.ITEM)),

    // Custom content!
    CARDINAL_DISABILITY(9, tags = arrayOf(Tag.COMBAT, Tag.NEW_STUFF)),
    RACING_THE_DEATH(10, tags = arrayOf(Tag.EXTREME, Tag.HERO, Tag.NEW_STUFF)),
    HORDE(11, tags = arrayOf(Tag.ENEMY, Tag.LEVEL)) {
        override fun _nMobsMult(): Float = 2f
    },
    INVASION(12, tags = arrayOf(Tag.HARD, Tag.ENEMY, Tag.LEVEL)),
    GREAT_MIGRATION(
        13,
        dependencies = arrayOf(INVASION.id),
        tags = arrayOf(Tag.EXTREME, Tag.ENEMY, Tag.LEVEL),
    ),
    MUTAGEN(14, tags = arrayOf(Tag.ENEMY, Tag.RNG)),
    EVOLUTION(
        15,
        dependencies = arrayOf(MUTAGEN.id),
        tags = arrayOf(Tag.HARD, Tag.ENEMY, Tag.RNG),
    ) {
        override fun _isItemBlocked(item: Item): Boolean = item is RatSkull
    },
    ROTTEN_LUCK(16, tags = arrayOf(Tag.HERO, Tag.RNG, Tag.COMBAT)),
    ARROWHEAD(17, tags = arrayOf(Tag.HERO, Tag.COMBAT, Tag.NEW_STUFF)),
    THUNDERSTRUCK(
        18,
        dependencies = arrayOf(ARROWHEAD.id),
        tags = arrayOf(Tag.SILLY, Tag.HERO, Tag.COMBAT),
    ),
    SECOND_TRY(19, tags = arrayOf(Tag.LEVEL, Tag.ITEM, Tag.RNG)),
    CRYSTAL_SHELTER(20, tags = arrayOf(Tag.COMBAT, Tag.NEW_STUFF)),
    CRYSTAL_BLOOD(21, tags = arrayOf(Tag.COMBAT, Tag.NEW_STUFF)),
    DEEPER_DANGER(22, tags = arrayOf(Tag.COMBAT, Tag.TRAPS)),
    HEAD_START(23, tags = arrayOf(Tag.POSITIVE, Tag.DUNGEON)),
    PRISON_EXPRESS(66, dependencies = arrayOf(HEAD_START.id), tags = arrayOf(Tag.DUNGEON)),
    BLINDNESS(24, tags = arrayOf(Tag.HARD, Tag.HERO)),
    BLOODBAG(25, tags = arrayOf(Tag.HERO, Tag.COMBAT, Tag.NEW_STUFF)),
    REVENGE(26, tags = arrayOf(Tag.COMBAT, Tag.NEW_STUFF)),
    REVENGE_FURY(27, tags = arrayOf(Tag.COMBAT, Tag.NEW_STUFF)),
    PREPARED_ENEMIES(28, tags = arrayOf(Tag.ENEMY, Tag.COMBAT)),
    REPEATER(29, tags = arrayOf(Tag.TRAPS)),
    DUPLICATOR(30, tags = arrayOf(Tag.ENEMY, Tag.TRAPS)),
    BODY_TRAPS(83, tags = arrayOf(Tag.TRAPS)),
    EXTREME_CAUTION(31, tags = arrayOf(Tag.LEVEL, Tag.TRAPS)) {
        override fun _nTrapsMult(): Float = 4f
    },
    PATRON_SAINTS(33, tags = arrayOf(Tag.COMBAT, Tag.NEW_STUFF)),
    PERSISTENT_SAINTS(
        34,
        dependencies = arrayOf(PATRON_SAINTS.id),
        tags = arrayOf(Tag.HARD, Tag.COMBAT),
    ),
    HOLY_WATER(35, tags = arrayOf(Tag.COMBAT, Tag.LEVEL, Tag.NEW_STUFF)),
    INTOXICATION(36, tags = arrayOf(Tag.HERO, Tag.ITEM, Tag.NEW_STUFF)),
    PLAGUE(37, tags = arrayOf(Tag.HERO, Tag.ITEM, Tag.NEW_STUFF)),
    TOXIC_WATER(38, tags = arrayOf(Tag.HERO, Tag.LEVEL, Tag.NEW_STUFF)),
    CERTAINTY_OF_STEEL(39, tags = arrayOf(Tag.HERO, Tag.NEW_STUFF)) {
        override fun _isItemBlocked(item: Item): Boolean = item is SaltCube
    },
    GOLDEN_COLOSSUS(
        52,
        dependencies = arrayOf(CERTAINTY_OF_STEEL.id),
        tags = arrayOf(Tag.POSITIVE, Tag.HERO, Tag.NEW_STUFF),
    ) {
        override fun _isItemBlocked(item: Item): Boolean = item is MasterThievesArmband
    },
    PARADOX_LEVELGEN(40, tags = arrayOf(Tag.LEVEL, Tag.SILLY)),
    RETIERED(41, tags = arrayOf(Tag.ITEM)),
    UNTIERED(42, dependencies = arrayOf(RETIERED.id), tags = arrayOf(Tag.HARD, Tag.ITEM, Tag.RNG)),
    TIER_UP(88, dependencies = arrayOf(UNTIERED.id), tags = arrayOf(Tag.ITEM, Tag.BOSS)),
    UNSTABLE_ACCESSORIES(43, tags = arrayOf(Tag.SILLY, Tag.ITEM, Tag.RNG, Tag.HERO)),
    PANDEMONIUM(44, tags = arrayOf(Tag.SILLY, Tag.ITEM, Tag.RNG, Tag.HERO)),
    BARRIER_BREAKER(45, tags = arrayOf(Tag.HERO)),
    MOLES(46, tags = arrayOf(Tag.HARD, Tag.ENEMY, Tag.LEVEL, Tag.NEW_STUFF)),
    LOFT(47, tags = arrayOf(Tag.SILLY, Tag.LEVEL)),
    BULKY_FRAME(48, tags = arrayOf(Tag.ENEMY, Tag.HERO)),
    SHROUDING_PRESENCE(
        56,
        dependencies = arrayOf(BULKY_FRAME.id),
        tags = arrayOf(Tag.HARD, Tag.ENEMY, Tag.HERO),
    ),
    SLIDING(49, tags = arrayOf(Tag.SILLY, Tag.LEVEL, Tag.HERO, Tag.NEW_STUFF)) {
        override fun _nWaterMult(): Float = 1.5f
    },
    INSOMNIA(50, tags = arrayOf(Tag.HARD, Tag.HERO, Tag.ENEMY)),
    LOOT_PARADISE(51, tags = arrayOf(Tag.POSITIVE, Tag.ITEM, Tag.LEVEL)),
    BOMBERMOB(53, tags = arrayOf(Tag.SILLY, Tag.ENEMY, Tag.NEW_STUFF)),
    CONSTELLATION(
        62,
        dependencies = arrayOf(BOMBERMOB.id),
        tags = arrayOf(Tag.SILLY, Tag.ENEMY, Tag.NEW_STUFF),
    ),
    CURSED(54, tags = arrayOf(Tag.ITEM)),
    CURSE_MAGNET(
        55,
        dependencies = arrayOf(CURSED.id),
        tags = arrayOf(Tag.HARD, Tag.HERO, Tag.ITEM),
    ),
    EXTERMINATION(57, tags = arrayOf(Tag.DUNGEON, Tag.ENEMY, Tag.NEW_STUFF)),
    POSTPAID_LOOT(
        58,
        dependencies = arrayOf(EXTERMINATION.id),
        tags = arrayOf(Tag.HARD, Tag.ITEM, Tag.ITEM, Tag.DUNGEON, Tag.NEW_STUFF),
    ),
    MIMICS(59, tags = arrayOf(Tag.ITEM, Tag.MIMICS, Tag.LEVEL)),
    MIMICS_ALL(
        60,
        dependencies = arrayOf(MIMICS.id),
        tags = arrayOf(Tag.HARD, Tag.ITEM, Tag.MIMICS, Tag.LEVEL),
    ),
    MIMICS_GRIND(
        61,
        dependencies = arrayOf(MIMICS.id),
        tags = arrayOf(Tag.POSITIVE, Tag.MIMICS, Tag.LEVEL),
    ),
    JACK_IN_A_BOX(78, tags = arrayOf(Tag.MIMICS, Tag.ENEMY, Tag.LEVEL)),
    RECURSIVE_HIERARCHY(
        80,
        dependencies = arrayOf(JACK_IN_A_BOX.id),
        tags = arrayOf(Tag.MIMICS, Tag.ENEMY),
    ),
    BOXED(79, tags = arrayOf(Tag.MIMICS, Tag.ENEMY, Tag.LEVEL)),
    REPOPULATION(63, tags = arrayOf(Tag.ENEMY)),
    RESURRECTION(64, dependencies = arrayOf(REPOPULATION.id), tags = arrayOf(Tag.HARD, Tag.ENEMY)),
    FRACTAL_HIVE(
        65,
        dependencies = arrayOf(REPOPULATION.id),
        tags = arrayOf(Tag.EXTREME, Tag.ENEMY),
    ),
    INFERTILITY(90, tags = arrayOf(Tag.ENEMY, Tag.POSITIVE)),
    HYDRA(91, dependencies = arrayOf(INFERTILITY.id), tags = arrayOf(Tag.ENEMY)),
    CROOKED_DIE(67, tags = arrayOf(Tag.SILLY, Tag.RNG)),
    CRUMBLED_STAIRS(68, tags = arrayOf(Tag.DUNGEON)),
    MULTICLASSING(69, tags = arrayOf(Tag.SILLY, Tag.HERO)),
    EXOTIC_GOODS(70, tags = arrayOf(Tag.POSITIVE, Tag.ITEM)) {
        override fun _isItemBlocked(item: Item): Boolean = item is ExoticCrystals
    },
    OVER_THE_EDGE(72, tags = arrayOf(Tag.LEVEL, Tag.SILLY)),
    WHIPLASH(73, tags = arrayOf(Tag.SILLY, Tag.COMBAT)),
    CASUAL_APPROACH(74, tags = arrayOf(Tag.HERO, Tag.COMBAT)),
    LET_THEM_REST(75, tags = arrayOf(Tag.POSITIVE, Tag.HERO)),
    ETERNAL_FLAMES(76, tags = arrayOf(Tag.ENEMY, Tag.ENVIRONMENT)),
    DOMAIN_OF_HELL(77, tags = arrayOf(Tag.HARD, Tag.LEVEL, Tag.ENVIRONMENT)),
    CROWD_DIVERSITY(81, tags = arrayOf(Tag.ENEMY, Tag.COMBAT)),
    IN_YOUR_FACE(82, tags = arrayOf(Tag.ENEMY, Tag.LEVEL)),
    PERFECT_INFORMATION(84, tags = arrayOf(Tag.POSITIVE, Tag.HERO)),
    COMPLETE_KNOWLEDGE(94, tags = arrayOf(Tag.POSITIVE, Tag.ITEM)) {
        override fun _isItemBlocked(item: Item): Boolean = item is ShardOfOblivion
    },
    SAFETY_BUFFER(85, tags = arrayOf(Tag.POSITIVE)),
    SKELETON_CREW(86, tags = arrayOf(Tag.ENEMY, Tag.LEVEL, Tag.HERO)) {
        override fun _nMobsMult(): Float = 0.5f
    },
    ABANDONED_SHIP(
        87,
        tags = arrayOf(Tag.ENEMY, Tag.LEVEL),
        dependencies = arrayOf(SKELETON_CREW.id),
    ) {
        override fun _nMobsMult(): Float = 0f
    },
    UNINSPIRED_TO_LEARN(89, tags = arrayOf(Tag.HERO, Tag.BOSS)),
    PROTECTED_GOODS(92, tags = arrayOf(Tag.ITEM, Tag.BOSS, Tag.DUNGEON)),
    BOSS_RUSH(
        93,
        dependencies = arrayOf(HEAD_START.id, PROTECTED_GOODS.id),
        tags = arrayOf(Tag.BOSS, Tag.DUNGEON),
    ),
    // Next ID: 95
    ;

    val tags = Tag.process(dependencies.isNotEmpty(), tags)
    private val localizationKey = locString ?: name.lowercase()
    private val localizationClass =
        if (locString == null) Modifier::class.java else Challenges::class.java

    fun localizedName(): String = Messages.get(localizationClass, localizationKey)

    fun localizedDesc(): String = Messages.get(localizationClass, localizationKey + "_desc")

    @Suppress("FunctionName")
    open fun _isItemBlocked(item: Item): Boolean = false

    @Suppress("FunctionName")
    open fun _nMobsMult(): Float = 1f

    @Suppress("FunctionName")
    open fun _nWaterMult(): Float = 1f

    @Suppress("FunctionName")
    open fun _nGrassMult(): Float = 1f

    @Suppress("FunctionName")
    open fun _nTrapsMult(): Float = 1f

    fun active() = Dungeon.tcpdData?.modifiers?.isEnabled(this) ?: false

    fun conflicts(): List<Modifier> =
        conflicts.getOrPut(this) {
            mutableListOf()
        }

    companion object {
        val ALL: Array<Modifier> = Modifier.entries.sortedBy { it.id }.toTypedArray()
        private val conflicts = mutableMapOf<Modifier, MutableList<Modifier>>()

        init {
            if (ALL.last().id != ALL.size - 1) {
                val seen = mutableMapOf<Int, Modifier>()
                for (mod in ALL) {
                    val existing = seen[mod.id]
                    if (existing != null) {
                        throw IllegalStateException("Modifier id ${mod.id} is in use by both ${mod.name} and ${existing.name}")
                    }
                    seen[mod.id] = mod
                }
                throw IllegalStateException("Modifier IDs contain gaps!")
            }

            val conflictsSet = mutableSetOf<UnorderedPair<Modifier>>()

            fun addConflict(
                a: Modifier,
                b: Modifier,
            ) {
                if (conflictsSet.add(UnorderedPair(a, b))) {
                    conflicts.getOrPut(a) { mutableListOf() }.add(b)
                    conflicts.getOrPut(b) { mutableListOf() }.add(a)
                } else {
                    throw IllegalStateException(
                        "Conflict between ${a.name} and ${b.name} already exists!",
                    )
                }
            }

            addConflict(ABANDONED_SHIP, HORDE)
            addConflict(INFERTILITY, REPOPULATION)
        }

        fun fromVanilla(challengeId: Int): Modifier =
            when (challengeId) {
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

enum class Tag(
    val hidden: Boolean = false,
) {
    NEW_STUFF,

    EASY,

    // not assigned directly, but assumed if no other difficulty tag is present
    NORMAL,
    HARD,
    EXTREME,

    SILLY,
    POSITIVE,

    ENEMY,
    HERO,
    COMBAT,
    LEVEL,
    ENVIRONMENT,
    ITEM,
    RNG,
    DUNGEON,
    TRAPS,
    MIMICS,
    BOSS,

    ADDON(hidden = true),
    ;

    fun localizedName(): String = Messages.get(ModifierTag::class.java, name.lowercase())

    fun localizedDesc(): String = Messages.get(ModifierTag::class.java, name.lowercase() + "_desc")

    fun isDifficulty() = this == SILLY || this == POSITIVE || this == NORMAL || this == HARD || this == EXTREME

    fun icon(): TextureDescriptor =
        when (this) {
            ENEMY -> TCPDIcons.RAT.descriptor()
            HERO -> TextureDescriptor.HeroClass(HeroClass.WARRIOR, 6)
            COMBAT -> TextureDescriptor.ItemSprite(ItemSpriteSheet.SWORD)
            LEVEL -> TCPDIcons.MAP.descriptor()
            ENVIRONMENT -> Icons.GRASS.descriptor()
            ITEM -> TextureDescriptor.ItemSprite(ItemSpriteSheet.CHEST)
            NEW_STUFF -> TCPDIcons.STAR.descriptor()
            RNG -> TCPDIcons.TOKENS.descriptor()
            DUNGEON -> Icons.ENTER.descriptor()
            TRAPS -> TextureDescriptor.ItemSprite(ItemSpriteSheet.TRAP_MECHANISM)
            MIMICS -> TCPDIcons.MIMIC.descriptor()
            BOSS -> TCPDIcons.DWARF_KING.descriptor()
            SILLY -> TCPDIcons.CAKE.descriptor()
            ADDON -> TCPDIcons.UP_DOWN_ARROWS.descriptor()
            POSITIVE -> TextureDescriptor.ItemSprite(ItemSpriteSheet.ANKH)
            EASY -> TCPDIcons.CHALLENGE_DULL.descriptor()
            NORMAL -> TCPDIcons.CHALLENGE_GOLD.descriptor()
            HARD -> TCPDIcons.CHALLENGE_GOLD_BLOOD.descriptor()
            EXTREME -> TCPDIcons.CHALLENGE_GOLD_OOZE.descriptor()
        }

    companion object {
        internal fun process(
            hasDependencies: Boolean,
            tags: Array<Tag>,
        ): List<Tag> {
            tags.sortBy { it.ordinal }
            val isNormal = tags.none { it.isDifficulty() }

            if (!isNormal && !hasDependencies) {
                return tags.toList()
            }

            return tags
                .toMutableList()
                .apply {
                    if (isNormal) add(NORMAL)
                    if (hasDependencies) add(ADDON)
                }
        }
    }
}

private class ModifierTag

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
            val bits = decodeBits(encoded)
            return Modifiers(bits.copyOf(Modifier.entries.size))
        }

        fun encodeBits(bits: BooleanArray): String {
            val encoded = bits.asBytes(false).trimEnd().encodeToBase58String()
            if (encoded.all { it == '1' }) return ""
            return encoded
        }

        fun decodeBits(encoded: String): BooleanArray = encoded.decodeBase58().asBits().trimEnd(false)

        fun debugModeActive(): Boolean = DeviceCompat.isDebug()

        const val MODIFIERS = "modifiers"

        init {
            doTests()
        }
    }

    fun asRaw(): BooleanArray = modifiers.copyOf()

    fun isChallenged(): Boolean = modifiers.any { it }

    fun activeChallengesCount(): Int = modifiers.count { it }

    fun isEnabled(modifier: Modifier): Boolean = modifiers[modifier.id]

    fun isVanillaEnabled(challengeId: Int): Boolean = isEnabled(Modifier.fromVanilla(challengeId))

    fun enable(modifier: Modifier) {
        modifiers[modifier.id] = true

        modifier.dependencies.forEach {
            enable(Modifier.ALL[it])
        }
        for (conflict in modifier.conflicts()) {
            disable(conflict)
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

    fun isItemBlocked(item: Item): Boolean = Modifier.entries.any { modifiers[it.id] && it._isItemBlocked(item) }

    fun isActionBanned(
        item: Item,
        action: String,
    ): Boolean {
        if (item.cursed && isEnabled(Modifier.CURSE_MAGNET)) {
            return action == Item.AC_DROP || action == Item.AC_THROW
        }
        return false
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

    fun nWaterMult(): Float {
        var mult = 1f
        for (modifier in Modifier.entries) {
            if (modifiers[modifier.id]) {
                mult *= modifier._nWaterMult()
            }
        }
        return mult
    }

    fun nGrassMult(): Float {
        var mult = 1f
        for (modifier in Modifier.entries) {
            if (modifiers[modifier.id]) {
                mult *= modifier._nGrassMult()
            }
        }
        return mult
    }

    fun nTrapsMult(): Float {
        var mult = 1f
        for (modifier in Modifier.entries) {
            if (modifiers[modifier.id]) {
                mult *= modifier._nTrapsMult()
            }
        }
        return mult
    }

    fun scalingDepthBonus(): Int = if (isEnabled(Modifier.DEEPER_DANGER)) 10 else 0

    override fun restoreFromBundle(bundle: Bundle) {
        bundle.getBooleanArray(MODIFIERS).copyInto(modifiers)
    }

    override fun storeInBundle(bundle: Bundle) {
        bundle.put(MODIFIERS, modifiers)
    }

    fun serializeToString(): String {
        val str = encodeBits(modifiers)
        return str
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Modifiers

        return modifiers.contentEquals(other.modifiers)
    }

    override fun hashCode(): Int = modifiers.contentHashCode()
}

private fun doTests() {
    val mods =
        booleanArrayOf(
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
            false,
        )
    val newMods = booleanArrayOf(true, false, true)

    val ogBytes = mods.asBytes(false).trimEnd()
    val newBytes = newMods.asBytes(false).trimEnd()

    val ogBase58 = ogBytes.encodeToBase58String()
    val newBase58 = newBytes.encodeToBase58String()

    assertEq(ogBase58, newBase58)
}
