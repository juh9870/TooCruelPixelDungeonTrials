package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui

import com.shatteredpixel.shatteredpixeldungeon.Chrome
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.context.Context
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.watabou.noosa.NinePatch
import com.watabou.noosa.ui.Component

abstract class TcpdComponent(chrome: NinePatch = Chrome.get(Chrome.Type.WINDOW)) : Component() {
    private val ctx = Context()
    private var inUpdate = false
    private var firstUpdate = true

    init {
        this.addToFront(ctx.rootGroup)
    }

    override fun layout() {
        super.layout()
        if (!inUpdate) update()
    }

    override fun destroy() {
        super.destroy()
        ctx.destroy()
    }

    override fun update() {
        if (!isUpdating() && !firstUpdate) return
        firstUpdate = false
        inUpdate = true
        val res = ctx.update(
            Rect.fromSize(
                Pos2(
                    this.x.toInt(),
                    this.y.toInt(),
                ),
                Vec2(
                    this.width.toInt(), this.height.toInt()
                )

            )
        ) {
            drawUi()
        }

        val size = res.response.rect.size()
        setSize(size.x.toFloat(), size.y.toFloat())
        super.update()
        inUpdate = false
    }

    abstract fun Ui.drawUi()

    protected open fun isUpdating(): Boolean {
        return true
    }
}