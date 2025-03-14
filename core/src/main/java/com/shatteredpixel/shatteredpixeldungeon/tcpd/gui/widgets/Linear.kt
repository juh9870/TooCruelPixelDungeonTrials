package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets

import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.InnerResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Layout
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.LayoutConstructor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiId
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.ComponentConstructor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.NinePatchDescriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.Painter
import com.watabou.noosa.NinePatch
import com.watabou.noosa.ui.Component

data class WithLayout(
    val layout: LayoutConstructor?,
    val background: NinePatchDescriptor? = null,
) {
    fun createPainter(ui: Ui, id: UiId): Pair<Painter, NinePatchComponent?> {
        val painter = ui.top().painter().withComponent(id, NinePatchComponent.Companion)
        val group = painter.getGroup();
        if (group != null) {
            (group as NinePatchComponent).setNinePatch(background!!)
            return Pair(painter, group)
        }
        return Pair(painter, null)
    }

    inline fun <T> show(ui: Ui, crossinline block: () -> T): InnerResponse<T> {
        val id = ui.top().nextAutoId().with("linear")
        val (painter, ninePatch) = if (background != null) {
            createPainter(ui, id)
        } else {
            Pair(ui.top().painter(), null)
        }

        val margins = ninePatch?.ninePatch?.margins() ?: Margins.ZERO

        val res = ui.withLayout(
            margins = margins, layout = layout, block = block, painter = painter, id = id,
        )

        return res
    }
}

inline fun <T> Ui.vertical(
    background: NinePatchDescriptor? = null, crossinline block: () -> T
): InnerResponse<T> {
    return WithLayout(
        layout = Layout.Vertical,
        background = background,
    ).show(
        this, block
    )
}

inline fun <T> Ui.verticalJustified(
    background: NinePatchDescriptor? = null, crossinline block: () -> T
): InnerResponse<T> {
    return WithLayout(
        layout = Layout.VerticalJustified,
        background = background,
    ).show(
        this, block
    )
}

inline fun <T> Ui.horizontal(
    background: NinePatchDescriptor? = null, crossinline block: () -> T
): InnerResponse<T> {
    return WithLayout(
        layout = Layout.Horizontal,
        background = background,
    ).show(this, block)
}

inline fun <T> Ui.stack(
    background: NinePatchDescriptor? = null, crossinline block: () -> T
): InnerResponse<T> {
    return WithLayout(
        layout = Layout.Stack,
        background = background,
    ).show(this, block)
}

inline fun <T> Ui.stackJustified(
    background: NinePatchDescriptor? = null, crossinline block: () -> T
): InnerResponse<T> {
    return WithLayout(
        layout = Layout.StackJustified,
        background = background,
    ).show(this, block)
}

inline fun <T> Ui.stackFill(
    background: NinePatchDescriptor? = null, crossinline block: () -> T
): InnerResponse<T> {
    return WithLayout(
        layout = Layout.StackFill,
        background = background,
    ).show(this, block)
}

inline fun <T> Ui.rightToLeft(
    background: NinePatchDescriptor? = null, crossinline block: () -> T
): InnerResponse<T> {
    return WithLayout(
        layout = Layout.RightToLeft,
        background = background,
    ).show(this, block)
}

inline fun <T> Ui.columns(
    sizes: FloatArray,
    spacing: Int? = null,
    background: NinePatchDescriptor? = null, crossinline block: () -> T
): InnerResponse<T> {
    return WithLayout(
        layout = Layout.ColumnsLayout.constructor(sizes, spacing ?: top().style().itemSpacing),
        background = background,
    ).show(this, block)
}

class NinePatchComponent : Component() {
    companion object : ComponentConstructor {
        override fun construct(): Component {
            return NinePatchComponent()
        }

        override fun componentClass(): Class<out Component> {
            return NinePatchComponent::class.java
        }
    }

    var descriptor: NinePatchDescriptor? = null
        private set
    var ninePatch: NinePatch? = null
        private set

    fun setNinePatch(descriptor: NinePatchDescriptor): NinePatch {
        if (this.descriptor == descriptor) {
            return this.ninePatch!!
        }

        this.descriptor = descriptor
        val np = descriptor.get()
        this.ninePatch?.destroy()
        this.ninePatch = np
        add(np)
        layout()
        return np
    }

    override fun layout() {
        super.layout()
        ninePatch?.let {
            it.x = x
            it.y = y
            it.size(width, height)
        }
    }

    override fun clear() {
        super.clear()
        if (ninePatch != null) {
            add(ninePatch)
        }
    }
}