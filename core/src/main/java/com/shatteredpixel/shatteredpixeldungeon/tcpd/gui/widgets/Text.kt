package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.WidgetResponse
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock
import kotlin.math.ceil

class UiText(val text: String, val size: Int, val multiline: Boolean) {
    fun show(ui: Ui): WidgetResponse<RenderedTextBlock> {
        val top = ui.top()
        val space = top.layout.nextAvailableSpace(ui.top().style())
        val id = top.nextAutoId()
        val text = top.painter().drawText(id, space, text, size, multiline) as RenderedTextBlock

        val textSize = Vec2(ceil(text.width()).toInt(), ceil(text.height()).toInt());
        val rect = top.allocateSize(textSize)

        if (rect.width() > textSize.x || rect.height() > textSize.y) {
            val newRect = rect.centerInside(textSize)
            text.setPos(newRect.min.x.toFloat(), newRect.min.y.toFloat())
            PixelScene.align(text)
        }
        return WidgetResponse(text, UiResponse(rect, id))
    }
}

fun Ui.label(
    text: String,
    size: Int,
    multiline: Boolean = false
): WidgetResponse<RenderedTextBlock> {
    return UiText(text, size, multiline).show(this)
}

fun Ui.activeLabel(
    text: String,
    size: Int,
    multiline: Boolean = false
): WidgetResponse<RenderedTextBlock> {
    val res = UiText(text, size, multiline).show(this)
    if(!top().isEnabled()) {
        res.widget.alpha(0.3f)
    }
    return res
}