package com.shatteredpixel.shatteredpixeldungeon.tcpd.actors.buffs

import com.shatteredpixel.shatteredpixeldungeon.actors.Char

interface TimescaleBuff {
    fun speedFactor(): Float
}

interface DamageAmplificationBuff {
    /**
     * Multiplier for the received damage
     */
    fun damageMultiplier(source: Any?): Float
}

interface DefSkillChangeBuff {
    /**
     * Modifier for the defense skill, before the roll is made
     *
     * Mainly used for returning [Char.INFINITE_EVASION][Char.INFINITE_EVASION]
     */
    fun modifyDefSkill(
        defSkill: Float,
        attacker: Char,
    ): Float = defSkill

    /**
     * Multiplier for the defense roll
     */
    fun defRollMultiplier(attacker: Char): Float
}

interface AtkSkillChangeBuff {
    /**
     * Modifier for the attack skill, before the roll is made
     *
     * Mainly used for returning [Char.INFINITE_ACCURACY][Char.INFINITE_ACCURACY]
     */
    fun modifyAtkSkill(
        atkSkill: Float,
        defender: Char,
    ): Float = atkSkill

    /**
     * Multiplier for the attack roll
     */
    fun atkRollMultiplier(defender: Char): Float
}

interface AttackAmplificationBuff {
    fun flatAttackBonus(): Float = 0f

    fun attackMultiplier(): Float = 1f

    fun flatAttackBonusPostMult(): Float = 0f
}

interface DefenseProcBuff {
    fun defenseProc(
        enemy: Char,
        damage: Int,
    )
}

interface AttackProcBuff {
    fun attackProc(
        enemy: Char,
        damage: Int,
    )
}

interface InvulnerabilityBuff {
    fun isInvulnerable(effect: Class<out Any>): Boolean
}

interface OnDeathEffectBuff {
    fun onDeathProc()
}

interface OnDamageTakenBuff {
    fun onDamageTaken(
        damage: Int,
        src: Any?,
    )
}

interface MindVisionExtBuff {
    fun revealRadius(): Int

    fun maxHeroDistance(): Int = -1
}

interface ResistanceBuff {
    fun resist(effect: Class<*>): Float
}
