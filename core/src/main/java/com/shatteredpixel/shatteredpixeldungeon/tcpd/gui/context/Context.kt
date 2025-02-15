package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.context

import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Rect
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.InnerResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiId
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.PaintCache
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.Painter
import com.watabou.noosa.Group

class Context(val rootGroup: Group = Group()) {
    val paintCache: PaintCache = PaintCache()
    var mode: ContextMode = ContextMode.DYNAMIC

    @PublishedApi
    internal val memory: TwoFrameMap<UiId, Any> = TwoFrameMap()

    /**
     * Call this function once to create static UI.
     */
    inline fun <T> once(maxSize: Rect, block: Ui.() -> T): InnerResponse<T> {
        mode = ContextMode.STATIC
        return drawUi(maxSize, block)
    }

    /**
     * Call this function every frame to update the UI.
     */
    inline fun <T> update(maxSize: Rect, block: Ui.() -> T): InnerResponse<T> {
        rootGroup.clear()
        mode = ContextMode.DYNAMIC
        memory.swap()
        return drawUi(maxSize, block)
    }

    inline fun <reified T : Any> getMemory(id: UiId): T? {
        val value = memory.get(id);
        if (value != null && value !is T) {
            throw IllegalStateException("Memory value type mismatch: expected ${T::class}, got ${value::class}. Id: $id")
        }

        return value as T?
    }

    @PublishedApi
    internal inline fun <T> drawUi(maxSize: Rect, block: Ui.() -> T): InnerResponse<T> {
        val ui = Ui(this, maxSize, Painter.create(rootGroup, paintCache))
        val inner = ui.run(block)

        if (ui.stackSize() > 1) {
            throw IllegalStateException("Unbalanced pushLayout/popLayout calls")
        }

        val allocated = ui.top().allocatedSpace

        return InnerResponse(
            inner,
            UiResponse(allocated, ui.top().id())
        )
    }
}

enum class ContextMode {
    STATIC,
    DYNAMIC
}

class TwoFrameMap<K, V> {
    private var curr: MutableMap<K, V> = mutableMapOf()
    private var prev: MutableMap<K, V> = mutableMapOf()

    /**
     * Advances the map to the next frame, discarding values not accessed in the previous frame.
     */
    fun swap() {
        val temp = prev
        prev = curr
        curr = temp
        curr.clear()
    }

    fun get(id: K): V? {
        var value = curr[id]

        if (value == null) {
            value = prev[id]
            if (value != null) {
                curr[id] = value
            }
        }

        return value
    }

    inline fun getOrPut(id: K, defaultValue: () -> V): V {
        val value = get(id)
        if (value != null) {
            return value
        }
        val new = defaultValue()
        set(id, new)
        return new
    }

    fun set(id: K, value: V) {
        curr[id] = value
    }
}