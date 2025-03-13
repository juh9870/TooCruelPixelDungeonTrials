package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks

import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.context.MemoryFrameListener
import kotlin.reflect.KProperty

class HookState : MemoryFrameListener {
    val values: MutableList<RawHookRef> = mutableListOf()
    var index: Int = 0

    inline fun <reified T : Any> get(tracker: Any): MutableHookRef<T> {
        if (index > values.size) {
            throw IllegalStateException("Hook index out of bounds")
        } else if (index == values.size) {
            values.add(RawHookRef(this, index, null, tracker))
        }

        val hook = values[index]
        if (hook.value != null && hook.value !is T) {
            throw IllegalStateException("Hook type mismatch for index $index: expected ${T::class}, got ${hook::class}")
        }

        if (hook.tracker != tracker) {
            hook.value = null
            hook.tracker = tracker
        }

        return MutableHookRef<T>(hook).also {
            index++
        }
    }

    override fun newFrame() {
        index = 0
    }
}

class RawHookRef(
    val state: HookState,
    val index: Int,
    var value: Any?,
    var tracker: Any
)

@JvmInline
value class HookRef<T : Any>(private val hook: MutableHookRef<T>) {
    /**
     * Get the current value of the hook, or initialize it if it has not been initialized.
     */
    fun getOrInit(value: T): T {
        return hook.getOrInit(value)
    }

    /**
     * Get the current value of the hook, or initialize it if it has not been initialized.
     */
    inline fun getOrInitWith(value: () -> T): T {
        tryGet().let {
            if (it != null) {
                return it
            }
        }
        return getOrInit(value())
    }

    /**
     * Get the current value of the hook.
     *
     * @throws IllegalStateException if the hook has not been initialized.
     */
    fun get(): T {
        return hook.get()
    }

    /**
     * Get the current value of the hook, or return null if it has not been initialized.
     */
    fun tryGet(): T? {
        return hook.tryGet()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return get()
    }
}

@JvmInline
value class MutableHookRef<T : Any>(private val hook: RawHookRef) {
    /**
     * Get the current value of the hook, or initialize it if it has not been initialized.
     */
    fun getOrInit(value: T): T {
        if (hook.value == null) {
            hook.value = value
        }

        @Suppress("UNCHECKED_CAST")
        return hook.value as T
    }

    /**
     * Get the current value of the hook, or initialize it if it has not been initialized.
     */
    inline fun getOrInitWith(crossinline value: () -> T): T {
        tryGet().let {
            if (it != null) {
                return it
            }
        }
        return getOrInit(value())
    }

    /**
     * Get the current value of the hook.
     *
     * @throws IllegalStateException if the hook has not been initialized.
     */
    fun get(): T {
        val curr = hook.value ?: throw IllegalStateException("Hook has not been initialized")

        @Suppress("UNCHECKED_CAST")
        return curr as T
    }

    /**
     * Get the current value of the hook, or return null if it has not been initialized.
     */
    fun tryGet(): T? {
        @Suppress("UNCHECKED_CAST")
        return hook.value as T?
    }

    /**
     * Set the next value of the hook.
     */
    fun set(value: T) {
        hook.value = value
    }

    fun immutable(): HookRef<T> {
        return HookRef(this)
    }

    private fun mismatchErr(): Nothing {
        throw IllegalStateException("Hook type mismatch for index ${hook.index}: unexpected ${hook.value!!::class}")
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        set(value)
    }
}