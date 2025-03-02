package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui

import com.shatteredpixel.shatteredpixeldungeon.Chrome
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.context.Context
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.ui.Window

abstract class TcpdWindow : Window(0, 0, Chrome.get(Chrome.Type.WINDOW)) {
    private val ctx = Context()
    protected var maxSize: Vec2 = Vec2(120, Int.MAX_VALUE)

    init {
        this.addToFront(ctx.rootGroup)
    }

    override fun destroy() {
        super.destroy()
        ctx.destroy()
    }

    override fun update() {
        val res = ctx.update(
            Rect.fromSize(
                Pos2(0, 0),
                this.maxSize
            )
        ) {
            drawUi()
        }

        val size = res.response.rect.size()
        resize(size.x, size.y)
        super.update()
    }

    abstract fun Ui.drawUi()
}