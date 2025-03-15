package com.shatteredpixel.shatteredpixeldungeon.tcpd

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.watabou.utils.Bundlable
import com.watabou.utils.Bundle

class TCPDData(trial: Trial) : Bundlable {
    var modifiers: Modifiers
    var trial: Trial? = null

    constructor() : this(Trial.CUSTOM)

    init {
        this.modifiers = trial.getModifiers() ?: throw IllegalArgumentException("Invalid trial")
        if (trial != Trial.CUSTOM) this.trial = trial
    }

    fun asInfoData(): TCPDGameInfoData {
        return TCPDGameInfoData().also {
            it.modifiers = modifiers
            it.trials = trial
        }
    }

    fun restoreFromInfoData(data: TCPDGameInfoData) {
        modifiers = data.modifiers
        trial = data.trials
    }

    fun isChallenged(): Boolean {
        return modifiers.isChallenged()
    }

    override fun restoreFromBundle(bundle: Bundle) {
        modifiers = bundle.get(MODIFIERS) as Modifiers
        if (bundle.contains(TRIAL)) {
            trial = bundle.get(TRIAL) as Trial
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        bundle.put(MODIFIERS, modifiers)
        trial?.let { bundle.put(TRIAL, it) }
    }

    companion object {
        const val MODIFIERS = "modifiers"
        const val TRIAL = "trial"
    }
}

class TCPDGameInfoData : Bundlable {
    lateinit var modifiers: Modifiers
    var trials: Trial? = null

    fun modifiersBtnString(): String {
        return trials?.name ?: Messages.get(Trials::class.java, "custom")
    }

    fun isChallenged(): Boolean {
        return modifiers.isChallenged()
    }

    override fun restoreFromBundle(bundle: Bundle) {
        modifiers = bundle.get(MODIFIERS) as Modifiers
        if (bundle.contains(TRIAL)) {
            trials = bundle.get(TRIAL) as Trial
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        bundle.put(MODIFIERS, modifiers)
        trials?.let { bundle.put(TRIAL, it) }
    }

    companion object {
        const val MODIFIERS = "modifiers"
        const val TRIAL = "trial"
    }
}