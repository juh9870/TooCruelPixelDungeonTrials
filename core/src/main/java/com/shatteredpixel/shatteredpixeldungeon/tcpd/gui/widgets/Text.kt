package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.AnimationState
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.useMemo
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.WidgetResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.LRUCache
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock
import com.watabou.noosa.RenderedText
import com.watabou.noosa.Visual
import kotlin.math.ceil
import kotlin.math.roundToInt

class UiText(val text: String, val size: Int, val multiline: Boolean) {
    fun show(ui: Ui): WidgetResponse<RenderedTextBlock> {
        val top = ui.top()
        val space = top.layout.nextAvailableSpace(ui.top().style())
        val id = top.nextAutoId()
        val text = top.painter().drawText(id, space, text, size, multiline)

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
    dimInactiveText(res)
    return res
}

fun Ui.dimInactiveText(res: WidgetResponse<RenderedTextBlock>, active: Boolean? = null) {
    val top = top()
    val id = res.response.id.with("labelDimmer")
    val enabled = active ?: top.isEnabled()
    val anim = ctx().getOrPutMemory(id) { AnimationState(enabled) }
    res.widget.alpha(
        anim.animate(
            enabled,
            top.style().interactionAnimationDuration
        ) { 0.3f + 0.7f * it })
}

fun <T : Visual> Ui.dimInactiveVisual(res: WidgetResponse<T>, active: Boolean? = null) {
    val top = top()
    val id = res.response.id.with("labelDimmer")
    val enabled = active ?: top.isEnabled()
    val anim = ctx().getOrPutMemory(id) { AnimationState(enabled) }
    res.widget.alpha(
        anim.animate(
            enabled,
            top.style().interactionAnimationDuration
        ) { 0.3f + 0.7f * it })
}

@Suppress("NAME_SHADOWING")
fun Ui.shrinkToFitLabel(
    text: String,
    defaultSize: Int,
    height: Int? = null
): WidgetResponse<RenderedTextBlock> {
    val availableWidth = top().nextAvailableSpace().width()
    val size by useMemo(Pair(text, availableWidth)) {
        var size = defaultSize
        do {
            val width = measureTextWidth(text, size)
            if (width <= availableWidth) break
            size--
        } while (size > 3)
        return@useMemo size
    }
    val height = height ?: textHeight(defaultSize)

    return horizontal {
        val spacer = spacer(Vec2(0, height))

        val allocated =
            top().allocateSize(Vec2(availableWidth, spacer.rect.height()))
        val id = top().nextAutoId();
        val text = top().painter()
            .drawText(id, allocated, text, size, false)
        text.setPos(
            allocated.left().toFloat(),
            allocated.top().toFloat() + (allocated.height()
                .toFloat() - text.height()) / 2
        )
        WidgetResponse(text, UiResponse(allocated, id))
    }.inner
}

private val MEASURED_TEXT: MutableMap<Pair<String, Int>, Float> = LRUCache(256)

fun measureTextWidth(text: String, size: Int): Float {
    val key = Pair(text, size)
    return MEASURED_TEXT.getOrPut(key) {
        RenderedText(text, size).width
    }
}

fun textHeight(size: Int): Int {
    return (size * 0.75f).roundToInt()
}