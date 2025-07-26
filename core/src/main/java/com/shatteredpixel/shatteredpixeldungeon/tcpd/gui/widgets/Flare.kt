package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets

import com.shatteredpixel.shatteredpixeldungeon.effects.Flare
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.WidgetResponse

class UiFlare {
    fun show(
        ui: Ui,
        rays: Int,
        radius: Float,
    ): WidgetResponse<Flare> {
        val top = ui.top()
        val id = top.nextAutoId()
        val allocated = top.allocateSize(Vec2(0, 0))
        val flare = top.painter().drawFlare(id, allocated.center(), rays, radius)
        return WidgetResponse(flare, UiResponse(allocated, id))
    }
}

fun Ui.flare(
    rays: Int = 8,
    radius: Float = 16f,
): WidgetResponse<Flare> = UiFlare().show(this, rays, radius)
