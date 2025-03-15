package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui

import com.shatteredpixel.shatteredpixeldungeon.Chrome
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.context.Context
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.ui.Window
import com.watabou.noosa.NinePatch

abstract class TcpdWindow(chrome: NinePatch = Chrome.get(Chrome.Type.WINDOW)) :
    Window(0, 0, chrome) {
    private val ctx = Context()
    protected var maxSize: Vec2 = Vec2(120, Int.MAX_VALUE)
    private var firstUpdate = true

    init {
        this.addToFront(ctx.rootGroup)
    }

    override fun destroy() {
        super.destroy()
        ctx.destroy()
    }

    override fun update() {
        if (!isUpdating() && !firstUpdate) return
        firstUpdate = false
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

    protected open fun isUpdating(): Boolean {
        return true
    }
}