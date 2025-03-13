package com.shatteredpixel.shatteredpixeldungeon.tcpd

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.asBits
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.decodeBase58
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.trimEnd
import com.watabou.noosa.Game
import com.watabou.utils.Bundlable
import com.watabou.utils.Bundle
import com.watabou.utils.FileUtils
import java.io.IOException
import javax.net.ssl.SSLProtocolException

class Trial() : Bundlable {
    var name: String = ""
    var modifiers: BooleanArray = BooleanArray(0)
    var lockedClass: HeroClass? = null

    private var valid: Boolean? = null
    private var errorCause: String? = null

    constructor(name: String, modifiers: BooleanArray, lockedClass: HeroClass? = null) : this() {
        this.name = name
        this.modifiers = modifiers
        this.lockedClass = lockedClass
    }

    constructor(name: String, lockedClass: HeroClass? = null, vararg modifiers: Modifier) : this() {
        this.name = name
        this.modifiers = Modifiers(*modifiers).asRaw()
        this.lockedClass = lockedClass
    }

    override fun restoreFromBundle(bundle: Bundle) {
        name = bundle.getString(NAME)
        modifiers = bundle.getBooleanArray(MODIFIERS)
        if (bundle.contains(LOCKED_CLASS)) {
            lockedClass = bundle.getEnum(LOCKED_CLASS, HeroClass::class.java)
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        bundle.put(NAME, name)
        bundle.put(MODIFIERS, modifiers)
        lockedClass?.let { bundle.put(LOCKED_CLASS, it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Trial) return false

        if (name != other.name) return false
        if (!modifiers.contentEquals(other.modifiers)) return false
        if (lockedClass != other.lockedClass) return false

        return true
    }

    fun isValid(): Boolean {
        valid?.let { return it }

        if (modifiers.isEmpty()) {
            errorCause = "empty"
            return false
        } else if (modifiers.size > Modifier.entries.size) {
            errorCause = "future_modifiers"
            return false
        }

        valid = true
        return true
    }

    fun localizedErrorMessage(): String? {
        if (isValid()) {
            return null
        }

        return Messages.get(Trial::class.java, "error_${errorCause}")
    }

    fun getModifiers(): Modifiers? {
        if (!isValid()) {
            return null
        }

        return Modifiers(modifiers)
    }

    fun setModifiers(modifiers: Modifiers) {
        if (!modifiers.isChallenged()) return
        this.modifiers = modifiers.asRaw()
    }

    override fun hashCode(): Int {
        return arrayOf(name, modifiers, lockedClass).contentDeepHashCode()
    }

    companion object {
        private const val NAME = "name"
        private const val MODIFIERS = "modifiers"
        private const val LOCKED_CLASS = "locked_class"

        val CUSTOM = Trial("Custom")

        fun fromNetworkBundle(bundle: Bundle): Trial {
            val name = bundle.getString(NAME)
            val modifiersCode = bundle.getString(MODIFIERS)
            val decoded = modifiersCode.decodeBase58().asBits().trimEnd()

            val lockedClass = if (bundle.contains(LOCKED_CLASS)) {
                bundle.getEnum(LOCKED_CLASS, HeroClass::class.java)
            } else {
                null
            }

            return Trial(
                name, decoded, lockedClass
            )
        }
    }
}

class TrialGroup() : Bundlable {
    var name: String = ""
    var url: String = ""
    var trials = listOf<Trial>()
    var version: Int = 0
    var internalId: Int? = null

    var wantNotify = false
    var isUpdating = false
    var updateError: String? = null
    var updatedAt: Long = 0

    constructor(
        name: String, updatedInVersion: Int, internalId: Int?, vararg trials: Trial
    ) : this(
        name, updatedInVersion, internalId, trials.toList()
    )

    constructor(
        name: String, version: Int, internalId: Int?, trials: List<Trial>
    ) : this() {
        this.name = name
        this.version = version
        this.trials = trials
        this.internalId = internalId
    }

    override fun restoreFromBundle(bundle: Bundle) {
        name = bundle.getString(NAME)
        url = bundle.getString(URL)

        val trials = mutableListOf<Trial>()
        // Use manual restoration to avoid possible arbitrary class instantiation
        for (b in bundle.getBundleArray(TRIALS)) {
            val t = Trial()
            t.restoreFromBundle(b)
            trials.add(t)
        }
        this.trials = trials
        this.version = bundle.getInt(VERSION)
        if (this.version == 0) {
            this.version = Game.versionCode
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        bundle.put(NAME, name)
        bundle.put(URL, url)
        val trialsBundles = Array(trials.size) { i ->
            val b = Bundle()
            trials[i].storeInBundle(b)
            b
        }
        bundle.put(TRIALS, trialsBundles)
        bundle.put(VERSION, version)
    }

    companion object {
        private const val TRIALS = "trials"
        private const val NAME = "name"
        private const val URL = "url"
        private const val VERSION = "version"

        fun fromNetworkBundle(bundle: Bundle): TrialGroup {
            val name = bundle.getString(NAME)
            var version = bundle.getInt(VERSION)
            if(version < 0) version = 0
            val trials = bundle.getBundleArray(TRIALS).map { trial ->
                Trial.fromNetworkBundle(trial)
            }

            return TrialGroup(
                name = name,
                version = version,
                internalId = null,
                trials = trials
            )
        }

        val BUILT_IN = arrayOf(
            TrialGroup(
                "Normal",
                843,
                1,
                Trial("Tier List", null, Modifier.RETIERED, Modifier.STRONGER_BOSSES),
                Trial("Bloodbath", HeroClass.WARRIOR, Modifier.BLOODBAG, Modifier.ARROWHEAD),
                Trial(
                    "Great Faith", HeroClass.CLERIC, Modifier.FAITH_ARMOR, Modifier.PATRON_SAINTS
                ),
                Trial(
                    "Withered Garden",
                    HeroClass.HUNTRESS,
                    Modifier.BARREN_LAND,
                    Modifier.CARDINAL_DISABILITY
                ),
                Trial("Champion's Path", null, Modifier.CHAMPION_ENEMIES, Modifier.STRONGER_BOSSES),
                Trial(
                    "Downgrade",
                    null,
                    Modifier.RETIERED,
                    Modifier.UNTIERED,
                    Modifier.FORBIDDEN_RUNES
                ),
                Trial("Amethyst", null, Modifier.CRYSTAL_BLOOD, Modifier.CRYSTAL_SHELTER),
                Trial(
                    "Clumsiness Unforgiven",
                    null,
                    Modifier.ROTTEN_LUCK,
                    Modifier.PREPARED_ENEMIES,
                    Modifier.BARRIER_BREAKER,
                    Modifier.SLIDING
                ),
                Trial(
                    "Minefield",
                    null,
                    Modifier.EXTREME_CAUTION,
                    Modifier.REPEATER,
                ),
                Trial(
                    "Toxic Spillage",
                    null,
                    Modifier.TOXIC_WATER,
                    Modifier.INTOXICATION,
                ),
                Trial(
                    "Unarmored Statue",
                    null,
                    Modifier.SECOND_TRY,
                    Modifier.FAITH_ARMOR,
                    Modifier.ON_DIET,
                    Modifier.CERTAINTY_OF_STEEL,
                ),
            ), TrialGroup(
                "Hard",
                843,
                2,
                Trial(
                    "Apocalypse",
                    null,
                    Modifier.BARREN_LAND,
                    Modifier.MUTAGEN,
                    Modifier.SWARM_INTELLIGENCE,
                    Modifier.INSOMNIA,
                ),
                Trial("Wall Breakers", null, Modifier.MOLES, Modifier.SWARM_INTELLIGENCE),
                Trial(
                    "Operation Bloodmoon",
                    null,
                    Modifier.SWARM_INTELLIGENCE,
                    Modifier.HORDE,
                    Modifier.MUTAGEN,
                    Modifier.EVOLUTION,
                    Modifier.REVENGE,
                    Modifier.REVENGE_FURY
                ),
                Trial(
                    "The Dark Ages",
                    null,
                    Modifier.DARKNESS,
                    Modifier.BLINDNESS,
                    Modifier.PLAGUE,
                    Modifier.TOXIC_WATER
                ),
                Trial("Roarborers", null, Modifier.INSOMNIA, Modifier.MOLES, Modifier.REVENGE),
                Trial(
                    "The Unblessed Machine",
                    HeroClass.DUELIST,
                    Modifier.FAITH_ARMOR,
                    Modifier.PHARMACOPHOBIA,
                    Modifier.PREPARED_ENEMIES,
                    Modifier.CERTAINTY_OF_STEEL,
                    Modifier.BARRIER_BREAKER,
                    Modifier.HORDE,
                    Modifier.HEAD_START,
                ),
                Trial(
                    "Bastard of Shadows",
                    HeroClass.ROGUE,
                    Modifier.RACING_THE_DEATH,
                    Modifier.BLINDNESS,
                    Modifier.DARKNESS,
                    Modifier.ARROWHEAD,
                    Modifier.LOFT,
                    Modifier.UNTIERED,
                ),
                Trial(
                    "Academy Dropout",
                    HeroClass.MAGE,
                    Modifier.PANDEMONIUM,
                    Modifier.UNSTABLE_ACCESSORIES,
                    Modifier.UNTIERED,
                    Modifier.PARADOX_LEVELGEN,
                    Modifier.FORBIDDEN_RUNES,
                ),
            ), TrialGroup(
                "Extreme", 843, 3, Trial(
                    "Crystal Crusher",
                    null,
                    Modifier.ARROWHEAD,
                    Modifier.THUNDERSTRUCK,
                    Modifier.CRYSTAL_SHELTER,
                    Modifier.CRYSTAL_BLOOD,
                    Modifier.PREPARED_ENEMIES,
                    Modifier.SWARM_INTELLIGENCE
                ), Trial(
                    "The Challenger",
                    null,
                    Modifier.COLOSSEUM,
                    Modifier.STRONGER_BOSSES,
                    Modifier.HEAD_START,
                    Modifier.RETIERED,
                ), Trial(
                    "Extinction Event",
                    null,
                    Modifier.GREAT_MIGRATION,
                    Modifier.DEEPER_DANGER,
                    Modifier.EXTREME_CAUTION,
                    Modifier.REPEATER,
                    Modifier.DUPLICATOR,
                    Modifier.PARADOX_LEVELGEN,
                    Modifier.HEAD_START,
                ), Trial(
                    "The Holy City",
                    null,
                    Modifier.PATRON_SAINTS,
                    Modifier.PERSISTENT_SAINTS,
                    Modifier.HOLY_WATER,
                    Modifier.CHAMPION_ENEMIES,
                    Modifier.FAITH_ARMOR,
                ), Trial(
                    "Teh Chariot",
                    null,
                    Modifier.MOLES,
                    Modifier.HORDE,
                    Modifier.SWARM_INTELLIGENCE,
                    Modifier.HEAD_START,
                    Modifier.BULKY_FRAME,
                    Modifier.SLIDING,
                )
            )
        )
    }
}

class Trials : Bundlable {
    private val groups = mutableListOf<TrialGroup>()

    fun getGroups(): List<TrialGroup> {
        return groups.toList()
    }

    override fun restoreFromBundle(bundle: Bundle) {
        groups.clear()
        for (g in bundle.getBundleArray(GROUPS)) {
            val group = TrialGroup()
            group.restoreFromBundle(g)
            groups.add(group)
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        val groupsBundles = mutableListOf<Bundle>()
        for (group in groups) {
            if (group.url.isBlank()) {
                continue
            }

            val b = Bundle()
            group.storeInBundle(b)
            groupsBundles.add(b)
        }

        bundle.put(GROUPS, groupsBundles.toTypedArray())
    }

    companion object {
        private const val GROUPS = "groups"
        private var trials: Trials? = null

        private const val TRIALS_FILE: String = "trials.dat"

        private fun empty(): Trials {
            return Trials().also { it.groups.addAll(TrialGroup.BUILT_IN) }
        }

        var curTrial: Trial? = null

        fun save() {
            val trials = trials ?: return
            synchronized(trials) {
                val bundle = Bundle()
                trials.storeInBundle(bundle)

                try {
                    FileUtils.bundleToFile(TRIALS_FILE, bundle)
                } catch (e: IOException) {
                    ShatteredPixelDungeon.reportException(e)
                }
            }
        }

        fun load(): Trials {
            if (trials != null) {
                return trials!!
            }
            this.trials = empty()

            try {
                val bundle = FileUtils.bundleFromFile(TRIALS_FILE)
                this.trials!!.restoreFromBundle(bundle)
            } catch (_: IOException) {
            }

            return trials!!
        }

        fun addGroup(url: String): Boolean {
            val trials = load()
            synchronized(trials) {
                for (g in trials.groups) {
                    if (g.url == url) {
                        return false
                    }
                }
                val g = TrialGroup("", Game.versionCode, null)
                g.version = -1
                g.url = url
                trials.groups.add(g)
                save()
            }
            return true
        }

        fun checkForUpdates() {
            for (group in load().groups) {
                if (group.url.isBlank() || group.isUpdating) {
                    continue
                }
                group.isUpdating = true
                val httpGet = Net.HttpRequest(Net.HttpMethods.GET)
                httpGet.url = group.url
                httpGet.setHeader("Accept", "application/json")

                Gdx.net.sendHttpRequest(httpGet, object : Net.HttpResponseListener {
                    override fun handleHttpResponse(httpResponse: Net.HttpResponse?) {
                        group.isUpdating = false
                        if (httpResponse == null) {
                            group.updateError = "Missing response"
                            return
                        }
                        val responseString = httpResponse.resultAsString
                        val bundle = try {
                            Bundle.read(responseString.byteInputStream())
                        } catch (e: Exception) {
                            if (responseString.contains("<html>", ignoreCase = true)) {
                                group.updateError = "Got HTML response, not JSON"
                            } else {
                                group.updateError = "Bad response body:\n${e.message}"
                            }
                            Game.reportException(e)
                            return
                        }

                        try {
                            val newGroup = TrialGroup.fromNetworkBundle(bundle)
                            group.updateError = null
                            if (newGroup.version > group.version) {
                                group.trials = newGroup.trials
                                group.wantNotify = true
                                group.updatedAt = Game.realTime
                                group.version = newGroup.version
                                if(group.name.isBlank()) {
                                    group.name = newGroup.name
                                }
                                save()
                            }
                        } catch (e: Exception) {
                            group.updateError = "Bad group structure:\n${e.message}"
                            Game.reportException(e)
                            return
                        }
                    }

                    override fun failed(t: Throwable?) {
                        group.isUpdating = false
                        if (t is SSLProtocolException) {
                            group.updateError = "Update failed due to SSL error\nYour device may not support the required encryption"
                        } else {
                            group.updateError = "Update failed:\n${t?.message}"
                        }
                        Game.reportException(t)
                    }

                    override fun cancelled() {
                        group.isUpdating = false
                    }
                })
            }
        }
    }
}