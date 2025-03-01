package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout

import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Rect
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Style
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.context.Context
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.Painter
import com.watabou.noosa.ui.Component

class Ui(private val ctx: Context, availableSpace: Rect, painter: Painter) {
    private val stack: MutableList<UiStackItem>

    init {
        val rootUi = UiStackItem(
            layout = Layout.Stack(availableSpace),
            painter = painter,
            id = UiId(0),
            margins = Margins.ZERO,
        )

        stack = mutableListOf(rootUi)
    }

    fun top(): UiStackItem {
        return stack.last()
    }

    fun ctx(): Context {
        return ctx
    }

    fun stackSize(): Int {
        return stack.size
    }

    fun pushLayout(
        availableSpace: Rect? = null,
        layout: LayoutConstructor? = null,
        margins: Margins? = null,
        id: UiId? = null,
        painter: Painter? = null,
        style: Style? = null,
    ): UiId {
        val parent = stack.last()
        val m = margins ?: Margins.ZERO
        val item = UiStackItem(
            layout = (layout ?: parent.layout.childContinued()).construct(
                availableSpace ?: parent.layout.nextAvailableSpace(style ?: parent.style()).shrink(m)
            ),
            id = id ?: parent.nextAutoId(),
            painter = painter,
            style = style,
            margins = m,
            parent = parent,
        )
        stack.add(item)
        return item.id
    }

    inline fun <T> withLayout(
        availableSpace: Rect? = null,
        layout: LayoutConstructor? = null,
        margins: Margins? = null,
        id: UiId? = null,
        painter: Painter? = null,
        style: Style? = null,
        block: () -> T
    ): InnerResponse<T> {
        val layoutId = pushLayout(availableSpace, layout, margins, id, painter, style)
        val inner = block()
        val response = popLayout(layoutId)
        return InnerResponse(inner, response)
    }

    fun popLayout(id: UiId): UiResponse {
        val removed = stack.removeLast()
        if (removed.id != id) {
            throw IllegalStateException("Popped layout with wrong ID: ${removed.id} != $id")
        }
        val allocatedSpace = removed.allocatedSpace.expand(removed.margins)
        val parent = stack.last()
        val response = parent.allocateSize(allocatedSpace.size())

        // if the removed layout had a different painter group, we need to update the rect accordingly
        val curPainterGroup = removed.painter().getGroup()
        if (curPainterGroup != null && curPainterGroup != parent.painter().getGroup()) {
            if (curPainterGroup is Component) {
                curPainterGroup.setRect(
                    response.rect.left().toFloat(),
                    response.rect.top().toFloat(),
                    response.rect.width().toFloat(),
                    response.rect.height().toFloat()
                )
            }
        }

        return response
    }
}

class UiStackItem(
    val margins: Margins,
    val layout: Layout,
    val id: UiId,
    private var painter: Painter? = null,
    private var parent: UiStackItem? = null,
    private var style: Style? = null,
) {
    private var nextId: UiId = id.next()
    var allocatedSpace: Rect =
        Rect.fromMinMax(layout.getFullAvailableSpace().min, layout.getFullAvailableSpace().min)
        private set

    fun style(): Style {
        var style = this.style ?: parent?.style()
        if (style != null) {
            return style
        }
        // No parent style found, use our own
        style = Style()
        this.style = style
        return style
    }

    fun setStyle(style: Style) {
        this.style = style
    }

    fun painter(): Painter {
        val painter = this.painter ?: parent?.painter()
        if (painter == null) {
            throw IllegalStateException("UI tree doesn't have any painter")
        }
        return painter
    }

    /**
     * Returns the id of this UI
     */
    fun id(): UiId {
        return id
    }

    /**
     * Returns the automatic ID for the next placed element.
     */
    fun nextAutoId(): UiId {
        val id = this.nextId
        this.nextId = id.next()
        return id
    }

    /**
     * Adds the given amount of space to the cursor.
     * @param amount The amount of space to add.
     */
    fun addSpace(amount: Int) {
        layout.addSpace(amount)
    }

    /**
     * Allocates a rectangle of the given desired size and advances the cursor.
     *
     * The actual size of the allocated rectangle may differ from the desired size.
     *
     * @param desiredSize The desired size of the rectangle to allocate.
     */
    fun allocateSize(desiredSize: Vec2): UiResponse {
        val rect = layout.allocate(desiredSize, style())
        allocatedSpace = allocatedSpace.union(rect)
        return UiResponse(rect, nextAutoId())
    }
}

@JvmInline
value class UiId(internal val id: Int) {
    fun next(): UiId {
        return with(1)
    }

    fun with(id: Any): UiId {
        return UiId(Pair(this.id, id).hashCode())
    }
}

interface Widget {
    fun ui(ui: Ui): UiResponse
}

data class UiResponse(val rect: Rect, val id: UiId)
data class InnerResponse<T>(val inner: T, val response: UiResponse)