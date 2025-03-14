package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks

import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.watabou.noosa.Game

inline fun <R> Ui.use(crossinline cb: HookState.() -> R): R {
    val id = this.top().id().with("hooks")

    val value = this.ctx().getOrPutMemory(id) {
        HookState()
    }

    return cb(value)
}

inline fun <reified T : Any> Ui.useState(
    tracker: Any,
    crossinline init: () -> T
): MutableHookRef<T> {
    return use {
        get<T>(tracker).apply { getOrInitWith(init) }
    }
}

inline fun <reified T : Any> Ui.useMemo(tracker: Any, crossinline init: () -> T): HookRef<T> {
    return use {
        get<T>(tracker).apply { getOrInitWith(init) }.immutable()
    }
}

class StaggeredHookData<T> {
    private var value: T? = null
    private var persistDuration = 0f
    private var nextValue: T? = null
    private var nextDuration = 0f

    fun compareSwap(newValue: T, stabilityTime: Float, persistTime: Float, elapsed: Float): T {
        if (value == null || value == newValue) {
            value = newValue
            nextDuration = 0f
            persistDuration += elapsed
            this.nextValue = null
            return newValue
        }

        if (nextValue != newValue) {
            nextValue = newValue
            nextDuration = elapsed
        } else {
            nextDuration += elapsed
        }
        if (nextDuration >= stabilityTime && persistDuration >= persistTime) {
            value = newValue
            nextValue = null
            nextDuration = 0f
            persistDuration = 0f
        }
        persistDuration += elapsed
        return value!!
    }
}

/**
 * A hook that allows for staggered updates to a value.
 *
 * Value will only get updated if it has been stable for `stabilityTime` and
 * the previous value persisted for at least `persistTime`.
 */
inline fun <reified T : Any> Ui.useStaggered(
    tracker: Any,
    stabilityTime: Float,
    persistTime: Float,
    crossinline value: () -> T
): T {
    return use {
        val hook = get<StaggeredHookData<T>>(tracker)

        val data = hook.getOrInitWith { StaggeredHookData() }

        data.compareSwap(value(), stabilityTime, persistTime, Game.elapsed)
    }
}