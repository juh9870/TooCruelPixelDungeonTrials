package com.shatteredpixel.shatteredpixeldungeon.tcpd

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.shatteredpixel.shatteredpixeldungeon.Assets
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene
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

    fun copy(): Trial {
        return Trial(name, modifiers.copyOf(), lockedClass)
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
//        val NONE = Trial("!!!NULL TRIAL!!!")

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

    constructor(
        name: String, version: Int, trials: List<Trial> = listOf()
    ) : this() {
        this.name = name
        this.version = version
        this.trials = trials
    }

    fun compareUpdate(other: TrialGroup): Boolean {
        if (other.version <= version) return false
        trials = other.trials
        wantNotify = true
        if (name.isBlank()) {
            name = other.name
        }
        markUpdated()
        return true
    }

    fun copyData(): TrialGroup {
        val g = TrialGroup()
        g.name = name
        g.url = url
        g.trials = trials
        g.version = version
        g.internalId = internalId
        return g
    }

    fun markUpdated() {
        wantNotify = true
    }

    fun notificationShown() {
        if (wantNotify) {
            wantNotify = false
            Trials.save()
        }
    }

    fun nameOrTrimmedUrl(): String {
        return name.ifBlank {
            var url = url
            if (url.startsWith("https://")) {
                url = url.substring(8)
            } else if (url.startsWith("http://")) {
                url = url.substring(7)
            }
            if (url.length > 16) {
                "${url.substring(0, 8)}[...]${url.substring(url.length - 8)}"
            } else {
                url
            }
        }
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
        if (bundle.contains(INTERNAL_ID)) {
            internalId = bundle.getInt(INTERNAL_ID)
        }
        if (bundle.contains(WANT_NOTIFY)) {
            wantNotify = bundle.getBoolean(WANT_NOTIFY)
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
        if (internalId != null) bundle.put(INTERNAL_ID, internalId!!)
        if (wantNotify) bundle.put(WANT_NOTIFY, true)
    }

    companion object {
        private const val TRIALS = "trials"
        private const val NAME = "name"
        private const val URL = "url"
        private const val INTERNAL_ID = "internal_id"
        private const val VERSION = "version"
        private const val WANT_NOTIFY = "want_notify"

        val DEFAULT_GROUPS: List<TrialGroup> = run {
            val groups = mutableListOf<TrialGroup>()
            val names = mapOf(1 to "normal.json", 2 to "hard.json", 3 to "extreme.json")
            val url =
                "https://raw.githubusercontent.com/juh9870/TooCruelPixelDungeonTrials/refs/heads/trials/core/src/main/assets/trials/"
            for ((id, name) in names) {
                val file = Gdx.files.internal(Assets.TCPD.Trials.BASEPATH + name)
                val bundle = Bundle.read(file.read())
                val group = fromNetworkBundle(bundle)
                group.url = url + name
                group.internalId = id
                groups.add(group)
            }
            groups
        }

        fun fromNetworkBundle(bundle: Bundle): TrialGroup {
            val name = bundle.getString(NAME)
            var version = bundle.getInt(VERSION)
            if (version < 0) version = 0
            val trials = bundle.getBundleArray(TRIALS).map { trial ->
                Trial.fromNetworkBundle(trial)
            }

            return TrialGroup(
                name = name, version = version, trials = trials
            )
        }
    }
}

class Trials : Bundlable {
    private val groups = mutableListOf<TrialGroup>()

    fun getGroups(): List<TrialGroup> {
        return groups
    }

    fun removeGroup(group: TrialGroup) {
        groups.remove(group)
        save()
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
            return Trials().also {
                it.groups.addAll(TrialGroup.DEFAULT_GROUPS)
            }
        }

        var curTrial: Trial? = null
            set(value) {
                field = value
                if (value?.lockedClass != null && value.lockedClass != GamesInProgress.selectedClass) {
                    val scene = ShatteredPixelDungeon.scene()
                    if (scene is HeroSelectScene) {
                        scene.setSelectedHero(value.lockedClass!!)
                    } else {
                        GamesInProgress.selectedClass = value.lockedClass!!
                    }
                }
            }

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
            val trials = empty()

            try {
                val bundle = FileUtils.bundleFromFile(TRIALS_FILE)
                trials.restoreFromBundle(bundle)
            } catch (_: IOException) {
            }

            val internals: MutableMap<Int, TrialGroup> = mutableMapOf()
            TrialGroup.DEFAULT_GROUPS.associateByTo(internals) { it.internalId!! }

            var anyUpdated = false
            for (group in trials.groups) {
                val id = group.internalId ?: continue

                val internal = internals.remove(id)

                if (internal != null) anyUpdated = group.compareUpdate(internal) || anyUpdated
            }

            for (group in internals.values) {
                trials.groups.add(group.copyData().also { g ->
                    g.markUpdated()
                })
                anyUpdated = true
            }

            this.trials = trials

            if (anyUpdated) {
                trials.groups.sortBy { it.internalId }
                save()
            }

            return trials
        }

        fun addGroup(url: String): Boolean {
            val trials = load()
            synchronized(trials) {
                for (g in trials.groups) {
                    if (g.url == url) {
                        return false
                    }
                }
                val g = TrialGroup("", -1)
                g.url = url
                trials.groups.add(g)
                save()
            }
            return true
        }

        fun heroClassAvailable(cl: HeroClass): Boolean {
            val locked = curTrial?.lockedClass
            return locked == null || locked == cl
        }

        fun heroClassLockedMsg(wantClass: HeroClass): String {
            return Messages.get(
                Trials::class.java,
                "hero_class_locked",
                curTrial!!.name,
                curTrial!!.lockedClass!!.title(),
                wantClass.title()
            )
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

                            if (group.compareUpdate(newGroup)) save()
                        } catch (e: Exception) {
                            group.updateError = "Bad group structure:\n${e.message}"
                            Game.reportException(e)
                            return
                        }
                    }

                    override fun failed(t: Throwable?) {
                        group.isUpdating = false
                        if (t is SSLProtocolException) {
                            group.updateError =
                                "Update failed due to SSL error\nYour device may not support the required encryption"
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