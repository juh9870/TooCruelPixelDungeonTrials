package com.shatteredpixel.shatteredpixeldungeon.tcpd

import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.Rankings
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.getMap
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.putMap
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.tcpdDataReadOnly
import com.watabou.utils.Bundlable
import com.watabou.utils.Bundle
import com.watabou.utils.FileUtils
import java.io.IOException
import kotlin.math.sign

@RequiresOptIn(message = "Internal score access is discouraged", level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class InternalScore

class ModifierScore : Bundlable {
    var wins: Int = 0
        private set
    var losses: Int = 0
        private set

    @InternalScore
    fun setWins(wins: Int) {
        this.wins = wins
    }

    @InternalScore
    fun setLosses(losses: Int) {
        this.losses = losses
    }

    override fun restoreFromBundle(bundle: Bundle) {
        wins = bundle.getInt(WINS)
        losses = bundle.getInt(LOSSES)
    }

    override fun storeInBundle(bundle: Bundle) {
        bundle.put(WINS, wins)
        bundle.put(LOSSES, losses)
    }

    companion object {
        private const val WINS: String = "wins"
        private const val LOSSES: String = "losses"
    }
}

class TrialScore : Bundlable {
    var wins: Int = 0
        private set
    var seededWins: Int = 0
        private set
    var losses: Int = 0
        private set

    private var deletedWins: Int = 0
    private var deletedSeededWins: Int = 0
    private var deletedLosses: Int = 0

    private var modifiers: String = ""
    private var records: MutableList<Rankings.Record>? = null
    private var addRecords = mutableListOf<Rankings.Record>()

    @InternalScore
    internal fun setModifiers(modifiers: String) {
        this.modifiers = modifiers
        recalculate()
    }

    fun statsEmpty(): Boolean = wins == 0 && seededWins == 0 && losses == 0

    fun rankingsEmpty(): Boolean = load().isEmpty()

    fun statsNotEmpty(): Boolean = !statsEmpty()

    fun save() {
        val records = records ?: return
        synchronized(records) {
            val bundle = Bundle()
            bundle.put(RECORDS, records)
            try {
                FileUtils.bundleToFile(recordsFile(), bundle)
            } catch (e: IOException) {
                ShatteredPixelDungeon.reportException(e)
            }
        }
    }

    fun load(): List<Rankings.Record> {
        if (records != null) return records!!
        val records = mutableListOf<Rankings.Record>()

        try {
            val bundle = FileUtils.bundleFromFile(recordsFile())
            val items = bundle.getCollection(RECORDS)
            for (item in items) {
                records.add(item as Rankings.Record)
            }
        } catch (_: IOException) {
        }

        this.records = records
        recalculate()
        return records
    }

    fun record(record: Rankings.Record) {
        this.addRecords.add(record)
        recalculate()
    }

    private fun recalculate() {
        val records = records
        if (records == null) {
            load()
            return // load will call recalculate again
        }

        var wins = 0
        var seededWins = 0
        var losses = 0

        var deletedWins = this.deletedWins
        var deletedSeededWins = this.deletedSeededWins
        var deletedLosses = this.deletedLosses

        var recordsChanged = false
        synchronized(records) {
            val existingRecords = records.map { it.gameID }.toMutableSet()

            // check rankings for records matching this modifiers
            Rankings.INSTANCE.load()
            addRecords.addAll(Rankings.INSTANCE.records)
            for (record in addRecords) {
                val data = record.tcpdDataReadOnly()
                if (data == null || data.modifiers.serializeToString() != modifiers) {
                    continue
                }

                if (existingRecords.contains(record.gameID)) {
                    continue
                }

                records.add(record)
                existingRecords.add(record.gameID)
                recordsChanged = true
            }
            addRecords.clear()

            records.sortWith(SCORE_COMPARATOR)
            while (records.size > MAX_RECORDS) {
                val deleted = records.removeAt(records.size - 1)
                if (deleted.win) {
                    if (deleted.customSeed != null && deleted.customSeed.isNotEmpty()) {
                        deletedSeededWins++
                    } else {
                        deletedWins++
                    }
                } else {
                    deletedLosses++
                }
                recordsChanged = true
            }

            for (record in records) {
                if (record.win) {
                    if (record.customSeed != null && record.customSeed.isNotEmpty()) {
                        seededWins++
                    } else {
                        wins++
                    }
                } else {
                    losses++
                }
            }
        }

        if (recordsChanged) {
            save()
        }

        this.wins += deletedWins
        this.seededWins += deletedSeededWins
        this.losses += deletedLosses

        if (
            this.wins != wins ||
            this.seededWins != seededWins ||
            this.losses != losses ||
            this.deletedWins != deletedWins ||
            this.deletedSeededWins != deletedSeededWins ||
            this.deletedLosses != deletedLosses
        ) {
            this.wins = wins
            this.seededWins = seededWins
            this.losses = losses
            this.deletedWins = deletedWins
            this.deletedSeededWins = deletedSeededWins
            this.deletedLosses = deletedLosses
            TCPDScores.save()
        }
    }

    private fun recordsFile(): String = "$RANKINGS_FOLDER/$modifiers.dat"

    override fun restoreFromBundle(bundle: Bundle) {
        wins = bundle.getInt(WINS)
        losses = bundle.getInt(LOSSES)
        seededWins = bundle.getInt(SEEDED_WINS)

        deletedWins = bundle.getInt(DELETED_WINS)
        deletedSeededWins = bundle.getInt(DELETED_SEEDED_WINS)
        deletedLosses = bundle.getInt(DELETED_LOSSES)
    }

    override fun storeInBundle(bundle: Bundle) {
        bundle.put(WINS, wins)
        bundle.put(SEEDED_WINS, seededWins)
        bundle.put(LOSSES, losses)

        bundle.put(DELETED_WINS, deletedWins)
        bundle.put(DELETED_SEEDED_WINS, deletedSeededWins)
        bundle.put(DELETED_LOSSES, deletedLosses)
    }

    companion object {
        private const val WINS: String = "wins"
        private const val SEEDED_WINS: String = "seeded_wins"
        private const val LOSSES: String = "losses"
        private const val DELETED_WINS: String = "deleted_wins"
        private const val DELETED_SEEDED_WINS: String = "deleted_seeded_wins"
        private const val DELETED_LOSSES: String = "deleted_losses"
        private const val RECORDS: String = "records"
        private const val RANKINGS_FOLDER: String = "trials_rankings"

        private const val MAX_RECORDS: Int = 10

        private val SCORE_COMPARATOR =
            java.util.Comparator<Rankings.Record> { lhs, rhs ->
                // this covers custom seeded runs and dailies
                if (rhs.customSeed.isEmpty() && lhs.customSeed.isNotEmpty()) {
                    return@Comparator +1
                } else if (lhs.customSeed.isEmpty() && rhs.customSeed.isNotEmpty()) {
                    return@Comparator -1
                }

                val result = sign((rhs.score - lhs.score).toDouble()).toInt()
                if (result == 0) {
                    sign((rhs.gameID.hashCode() - lhs.gameID.hashCode()).toDouble()).toInt()
                } else {
                    result
                }
            }
    }
}

class TCPDScores : Bundlable {
    private var modifiers: MutableMap<Int, ModifierScore> = mutableMapOf()
    private var trials: MutableMap<String, TrialScore> = mutableMapOf()

    fun modifierScore(modifier: Modifier): ModifierScore = modifiers.getOrPut(modifier.id) { ModifierScore() }

    @OptIn(InternalScore::class)
    fun trialScore(trial: Trial): TrialScore? {
        if (trial.isCustom()) {
            return null // Custom trials are not tracked separately
        }
        val modifiers = trial.getModifiers() ?: return null
        val modifiersString = modifiers.serializeToString()
        return trials.getOrPut(modifiersString) {
            TrialScore().also {
                it.setModifiers(
                    modifiersString,
                )
            }
        }
    }

    @OptIn(InternalScore::class)
    override fun restoreFromBundle(bundle: Bundle) {
        modifiers =
            bundle.getMap(
                MODIFIERS,
                { k -> getIntArray(k).toTypedArray() },
                { k -> getCollection(k).map { it as ModifierScore }.toTypedArray() },
            )
        if (bundle.contains(TRIALS + "_keys")) {
            trials =
                bundle.getMap(
                    TRIALS,
                    { k -> getStringArray(k) },
                    { k -> getCollection(k).map { it as TrialScore }.toTypedArray() },
                )
            for (kv in trials) {
                kv.value.setModifiers(kv.key)
            }
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        // remove empty modifiers
        for ((key, stats) in modifiers.toList()) {
            if (stats.wins == 0 && stats.losses == 0) {
                modifiers.remove(key)
            }
        }
        // remove empty trials
        for ((key, stats) in trials.toList()) {
            if (stats.wins == 0 && stats.seededWins == 0 && stats.losses == 0) {
                trials.remove(key)
            }
        }
        bundle.putMap(
            MODIFIERS,
            modifiers,
            { k, v -> put(k, v.toIntArray()) },
            { k, v -> put(k, v.toList()) },
        )

        bundle.putMap(
            TRIALS,
            trials,
            { k, v -> put(k, v) },
            { k, v -> put(k, v.toList()) },
        )
    }

    companion object {
        private var scores: TCPDScores? = null

        private const val MODIFIERS: String = "modifiers"
        private const val TRIALS: String = "trials"

        private const val SCORES_FILE: String = "tcpd_scores.dat"

        @OptIn(InternalScore::class)
        fun submit(
            win: Boolean,
            record: Rankings.Record,
        ) {
            if (Dungeon.hero == null) {
                return
            }

            val scores = load()

            val trial = Dungeon.tcpdData.trial
            if (trial != null && !trial.isCustom()) {
                scores.trialScore(trial)?.record(record)
            }

            if (win && (record.customSeed != null && record.customSeed.isNotEmpty())) {
                // Seeded wins are not counted for individual modifiers
                return
            }

            for (modifier in Modifier.ALL) {
                if (!modifier.active()) {
                    continue
                }
                val score = scores.modifierScore(modifier)
                if (win) {
                    score.setWins(score.wins + 1)
                } else {
                    score.setLosses(score.losses + 1)
                }
            }

            save()
        }

        fun save() {
            val scores = scores ?: return
            synchronized(scores) {
                val bundle = Bundle()
                scores.storeInBundle(bundle)

                try {
                    FileUtils.bundleToFile(SCORES_FILE, bundle)
                } catch (e: IOException) {
                    ShatteredPixelDungeon.reportException(e)
                }
            }
        }

        fun load(): TCPDScores {
            if (scores != null) return scores!!
            val scores = TCPDScores()

            try {
                val bundle = FileUtils.bundleFromFile(SCORES_FILE)
                scores.restoreFromBundle(bundle)
            } catch (_: IOException) {
            }

            this.scores = scores
            updateRankings()
            return scores
        }

        // Updates all trials available in recent rankings
        fun updateRankings() {
            val scores = load()
            Rankings.INSTANCE.load()
            for (record in Rankings.INSTANCE.records) {
                val data =
                    record.tcpdDataReadOnly()?.trials?.let {
                        scores.trialScore(it)?.load()
                    }
            }
        }
    }
}
