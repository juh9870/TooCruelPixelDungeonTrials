package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets

import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.WidgetResponse
import com.watabou.noosa.ColorBlock

fun Ui.spacer(size: Vec2): UiResponse {
    val rect = top().allocateSize(size)
    return UiResponse(rect, top().nextAutoId())
}

fun Ui.colorBlock(size: Vec2, color: UInt): WidgetResponse<ColorBlock> {
    val rect = top().allocateSize(size)
    val id = top().nextAutoId()
    val block = top().painter().drawRect(id, rect, color.toInt())
    return WidgetResponse(block, UiResponse(rect, id))
}

fun Ui.hSeparator(height: Int, color: UInt): WidgetResponse<ColorBlock> {
    return colorBlock(Vec2(top().nextAvailableSpace().width(), height), color)
}

fun Ui.vSeparator(width: Int, color: UInt): WidgetResponse<ColorBlock> {
    return colorBlock(Vec2(width, top().nextAvailableSpace().height()), color)
}